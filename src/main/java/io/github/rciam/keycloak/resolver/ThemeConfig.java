package io.github.rciam.keycloak.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;

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

    private static WatchService watchService;
    private static WatchKey watchKey;
    private static boolean FOLDER_INITIALIZED = false;
    private static Map<String, List<String>> config;


    public ThemeConfig() {
        initializeStatics();
    }

    public List<String> get(String configParam){
        return config.get(configParam);
    }

    public Map<String, List<String>> getConfig() {
        return config;
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

        if(config == null)
            synchronizeConfig();

        if(watchKey == null) {
            Runnable runnable = () -> {
                Path path = Paths.get(getThemeConfigFolder());
                try {
                    watchService = FileSystems.getDefault().newWatchService();
                    watchKey = path.register(watchService, ENTRY_MODIFY);

                    while ((watchKey = watchService.take()) != null) {
                        for (WatchEvent<?> event : watchKey.pollEvents()) {
                            if(event.context().toString().endsWith("configuration.json")){
                                if(event.kind() == ENTRY_MODIFY) {
                                    String fileContents = Commons.readFile(getThemeConfigFilePath());
                                    try {
                                        config = new ObjectMapper().readValue(fileContents, new TypeReference<HashMap<String, List<String>>>() {});
                                    }
                                    catch(IOException ex){
                                        config = new HashMap<>();
                                        logger.error("Could not update the keycloak-theme-vanilla specific config: " + fileContents , ex);
                                    }
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


    private void synchronizeConfig() {
        ObjectMapper om = new ObjectMapper();
        String filePath = getThemeConfigFilePath();
        File configFile = new File(filePath);
        String defaultConfig = getDefaultConfigString();
        String fileContent;
        if(!configFile.exists()) {
            try {
                Commons.writeFile(filePath, defaultConfig);
            }
            catch(IOException ex){
                logger.error("Theme - Could not write to theme's config file: " + filePath);
            }
            fileContent = defaultConfig;
        }
        else {
            try {
                fileContent = Commons.readFile(filePath);
            }
            catch (IOException ex){
                logger.error("Theme - Could not read theme's config file: " + filePath);
                fileContent = "";
            }
        }

        try {
            config = om.readValue(fileContent, new TypeReference<HashMap<String, List<String>>>() {});
        }
        catch(IOException ex){
            config = new HashMap<>();
            logger.error("Could not parse the keycloak-theme-vanilla specific config: " + fileContent , ex);
        }

    }


    private String getDefaultConfigString(){
        InputStream is = getClass().getClassLoader().getResourceAsStream("configuration.json");
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));
    }


    private Map<String,List<String>> getDefaultConfig(){
        Map<String,List<String>> defaultConfig;
        InputStream is = getClass().getClassLoader().getResourceAsStream("configuration.json");
        try {
            defaultConfig = new ObjectMapper().readValue(is, new TypeReference<HashMap<String, List<String>>>() {});
        }
        catch(IOException ex){
            defaultConfig = new HashMap<>();
            logger.error("Could not read the default keycloak-theme-vanilla specific config.", ex);
        }
        return defaultConfig;
    }


    private String getThemeConfigFolder(){
        return String.format("%s/%s", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER);
    }

    private String getThemeConfigFilePath(){
        return String.format("%s/%s/configuration.json", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER);
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

