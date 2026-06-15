package io.github.rciam.keycloak.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.rciam.keycloak.resolver.stubs.Configuration;
import org.jboss.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * This class loads additional configuration for different projects (i.e. different icons, different header and footer captions, etc.)
 */
public class ThemeConfig {

    private static final Logger logger = Logger.getLogger(ThemeConfig.class);

    private static WatchService watchService;
    private static WatchKey watchKey;
    private static boolean FOLDER_INITIALIZED = false;
    private static Configuration defaultConfig;
    private static Map<String, Configuration> realmsConfigs = new HashMap<>();


    public ThemeConfig() {
        initializeStatics();
    }


    public Configuration getConfig(String realmName) {
        return realmsConfigs.get(realmName);
    }

    /**
     * Does not broadcast, because it's not safe to send any configuration or code through unsecure
     * plaintext (serialized) event messages and materialize on the other nodes.
     * Also, config should update partially, meaning that if contains a subset of the fields (i.e. a single field),
     * should update only those, leaving the others intact.
     */
    public void setConfig(String realmName, Configuration config) {
        Configuration previous = realmsConfigs.get(realmName);
        Configuration merged = new Configuration();
        merged.putAll(previous);
        config.entrySet().forEach(entry -> {
            merged.put(entry.getKey(), entry.getValue());
        });
        realmsConfigs.put(realmName, merged);
        localSynchronizeConfig(realmName, merged);
    }


    private void initializeStatics() {

        if(!FOLDER_INITIALIZED) {
            try {
                Files.createDirectories(Paths.get(getThemeConfigFolder()));
                FOLDER_INITIALIZED = true;
            } catch (IOException e) {
                logger.error(String.format("Could not create theme's terms of use base folder: %s  That's serious, please fix it.", getThemeConfigFolder()));
            }
        }

        if(defaultConfig == null)
            loadDefaultConfig();

        if(watchKey == null) {
            Runnable runnable = () -> {
                Path path = Paths.get(getThemeConfigFolder());
                try {
                    watchService = FileSystems.getDefault().newWatchService();
                    watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

                    while ((watchKey = watchService.take()) != null) {
                        for (WatchEvent<?> event : watchKey.pollEvents()) {
                            if(event.context().toString().endsWith(".json")){
                                if(event.kind() == ENTRY_CREATE) {
                                    String realmName = event.context().toString().replace(".json","");
                                    String fileContents = Commons.readFile(getThemeConfigFilePath(realmName));
                                    try {
                                        Configuration config = new ObjectMapper().readValue(fileContents, Configuration.class);
                                        realmsConfigs.put(realmName, config);
                                    }
                                    catch(IOException ex){
                                        logger.error("Could not update the keycloak-theme-vanilla specific config: " + fileContents , ex);
                                    }
                                }
                                if(event.kind() == ENTRY_MODIFY) {
                                    String realmName = event.context().toString().replace(".json","");
                                    String fileContents = Commons.readFile(getThemeConfigFilePath(realmName));
                                    try {
                                        Configuration config = new ObjectMapper().readValue(fileContents, Configuration.class);
                                        realmsConfigs.replace(realmName, config);
                                    }
                                    catch(IOException ex){
                                        logger.error("Could not update the keycloak-theme-vanilla specific config: " + fileContents , ex);
                                    }
                                }
                                if(event.kind() == ENTRY_DELETE) {
                                    String realmName = event.context().toString().replace(".json","");
                                    realmsConfigs.remove(realmName);
                                }
                                logger.info("Noticed a change on a theme's configuration file. " + "Event kind:" + event.kind() + " File : " + event.context() + " - Updating configuration accordingly!");
                            }
                        }
                        watchKey.reset();
                    }
                }
                catch(IOException | InterruptedException ex){
                    logger.error(String.format("Theme's configuration file - Could not monitor folder %s for file changes. Expect serious problem with the theme configuration in the UI", path));
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }

    }


    public void localSynchronizeConfig(String realmName) {
        localSynchronizeConfig(realmName, null);
    }

    private void localSynchronizeConfig(String realmName, Configuration config) {
        ObjectMapper om = new ObjectMapper();
        String filePath = getThemeConfigFilePath(realmName);
        File configFile = new File(filePath);
        Configuration configuration;
        if(!configFile.exists()) {
            configuration = config != null ? config : defaultConfig;
            try {
                Commons.writeFile(filePath, om.writeValueAsString(configuration));
            }
            catch(IOException ex){
                logger.error("Theme - Could not write to theme's config file: " + filePath);
            }
        }
        else {
            try {
                configuration = config != null ? config : om.readValue(Commons.readFile(filePath), Configuration.class);
                if(configuration != null)
                    Commons.writeFile(filePath, om.writeValueAsString(configuration));
            }
            catch (IOException ex){
                logger.error("Theme - Could not read theme's config file: " + filePath);
                configuration = defaultConfig;
            }
        }
        if(realmsConfigs.keySet().contains(realmName))
            realmsConfigs.replace(realmName, configuration);
        else
            realmsConfigs.put(realmName, configuration);
    }


    private void loadDefaultConfig(){
        InputStream is = getClass().getClassLoader().getResourceAsStream("configuration.json");
        try {
            defaultConfig = new ObjectMapper().readValue(is, Configuration.class);
        }
        catch(IOException ex){
            defaultConfig = new Configuration();
            logger.error("Could not read the default keycloak-theme-vanilla specific config.", ex);
        }
    }


    private String getThemeConfigFolder(){
        return String.format("%s/%s/%s", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER, Commons.CONFIGURATIONS_FOLDER);
    }

    public String getThemeConfigFilePath(String realmName){
        return String.format("%s/%s/%s/%s.json", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER, Commons.CONFIGURATIONS_FOLDER, realmName);
    }

    /**
     * This is intended to be run ONLY upon the shutdown. DO NOT call it in any other case!
     */
    public static void shutdownAllWatchersAndListeners() {
        logger.info("Shutting down the watch service of the theme's config folder");
        watchKey.cancel();
        try {
            watchService.close();
        }
        catch(IOException ex){
            logger.info("Could not shutdown the watch service of the theme's config folder");
        }
    }

}

