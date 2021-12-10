package io.github.rciam.keycloak.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.rciam.keycloak.resolver.stubs.Configuration;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * This class loads additional configuration for different projects (i.e. different icons, different header and footer captions, etc.)
 */
public class ThemeConfig {

    private static final Logger logger = Logger.getLogger(ThemeConfig.class);

    private static boolean REALMS_LISTENER_ADDED = false;
    private static WatchService watchService;
    private static WatchKey watchKey;
    private static boolean FOLDER_INITIALIZED = false;
    private static Configuration defaultConfig;
    private static Map<String, Configuration> realmsConfigs;


    public ThemeConfig(KeycloakSession session) {
        initializeStatics(session);
    }


    public Configuration getConfig(String realmName) {
        return realmsConfigs.get(realmName);
    }

    private void initializeStatics(KeycloakSession session) {

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

        if(realmsConfigs == null){
            realmsConfigs = new HashMap<>();
            session.realms().getRealmsStream().forEach(realmModel -> {
                synchronizeConfig(realmModel.getName());
            });
        }

        if(!REALMS_LISTENER_ADDED){
            session.getKeycloakSessionFactory().register(event -> {
                if(event instanceof RealmModel.RealmCreationEvent) {
                    String realmName = ((RealmModel.RealmCreationEvent)event).getCreatedRealm().getName();
                    synchronizeConfig(realmName);
                }
                if(event instanceof RealmModel.RealmRemovedEvent) {
                    String realmName = ((RealmModel.RealmRemovedEvent)event).getRealm().getName();
                    String filePath = getThemeConfigFilePath(realmName);
                    File configFile = new File(filePath);
                    if(configFile.exists())
                        configFile.delete();
                }
            });
            REALMS_LISTENER_ADDED = true;
        }


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


    private void synchronizeConfig(String realmName) {
        ObjectMapper om = new ObjectMapper();
        String filePath = getThemeConfigFilePath(realmName);
        File configFile = new File(filePath);
        Configuration configuration;
        if(!configFile.exists()) {
            try {
                Commons.writeFile(filePath, om.writeValueAsString(defaultConfig));
            }
            catch(IOException ex){
                logger.error("Theme - Could not write to theme's config file: " + filePath);
            }
            configuration = defaultConfig;
        }
        else {
            try {
                configuration = om.readValue(Commons.readFile(filePath), Configuration.class);
            }
            catch (IOException ex){
                logger.error("Theme - Could not read theme's config file: " + filePath);
                configuration = defaultConfig;
            }
        }
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

    private String getThemeConfigFilePath(String realmName){
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

