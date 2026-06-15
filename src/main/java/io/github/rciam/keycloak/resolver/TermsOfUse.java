package io.github.rciam.keycloak.resolver;

import org.jboss.logging.Logger;
import java.io.ByteArrayOutputStream;
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

public class TermsOfUse {

    private static final Logger logger = Logger.getLogger(TermsOfUse.class);

    private static boolean FOLDER_INITIALIZED = false;
    private static WatchService watchService;
    private static WatchKey watchKey;
    private static String defaultTermsOfUseHtml;
    private static Map<String, String> realmsTermsOfUseHtml = new HashMap<>(); //todo: maybe make this a synchronized map


    public TermsOfUse(){
        initializeStatics();
    }

    public String getTermsOfUse(String realmName){
        return realmsTermsOfUseHtml.get(realmName);
    }

    /**
     * Does not broadcast, because it's not safe to send any configuration or code through unsecure
     * plaintext (serialized) event messages and materialize on the other nodes
    */
    public void setTermsOfUse(String realmName, String termsOfUseHtml) {
        termsOfUseHtml = Commons.removeScriptTags(termsOfUseHtml);
        localSynchronizeRealmTermsOfUse(realmName, termsOfUseHtml);
    }

    private void initializeStatics(){

        if(!FOLDER_INITIALIZED) {
            try {
                Files.createDirectories(Paths.get(getTermsOfUseFolder()));
                FOLDER_INITIALIZED = true;
            } catch (IOException e) {
                logger.error(String.format("Could not create theme's terms of use base folder: %s  That's serious, please fix it.", getTermsOfUseFolder()));
            }
        }

        if(defaultTermsOfUseHtml == null)
            loadDefaultTermsOfUse();

        if(watchKey == null) {
            Runnable runnable = () -> {
                Path path = Paths.get(getTermsOfUseFolder());
                try {
                    watchService = FileSystems.getDefault().newWatchService();
                    watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

                    while ((watchKey = watchService.take()) != null) {
                        for (WatchEvent<?> event : watchKey.pollEvents()) {
                            if(event.context().toString().endsWith(".html")){
                                if(event.kind() == ENTRY_CREATE) {
                                    String realmName = event.context().toString().replace(".html","");
                                    String fileContents = Commons.readFile(getTermsOfUseFile(realmName));
                                    realmsTermsOfUseHtml.put(realmName, fileContents);
                                }
                                if(event.kind() == ENTRY_MODIFY) {
                                    String realmName = event.context().toString().replace(".html","");
                                    String fileContents = Commons.readFile(getTermsOfUseFile(realmName));
                                    realmsTermsOfUseHtml.replace(realmName, fileContents);
                                }
                                if(event.kind() == ENTRY_DELETE) {
                                    String realmName = event.context().toString().replace(".html","");
                                    realmsTermsOfUseHtml.remove(realmName);
                                }
                                logger.info("Noticed a change on a theme's terms-of-use file. " + "Event kind:" + event.kind() + " File : " + event.context() + " - Updating terms-of-use accordingly!");
                            }
                        }
                        watchKey.reset();
                    }
                }
                catch(IOException | InterruptedException ex){
                    logger.error(String.format("Theme's terms of use files - Could not monitor folder %s for file changes. Expect serious problem with the terms of use in the UI", path));
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }

    }

    private String getTermsOfUseFolder(){
        return String.format("%s/%s/%s", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER, Commons.TERMS_OF_USE_FOLDER);
    }

    public String getTermsOfUseFile(String realmName){
        return String.format("%s/%s/%s/%s.html", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER, Commons.TERMS_OF_USE_FOLDER, realmName);
    }

    public void localSynchronizeRealmTermsOfUse(String realmName){
        localSynchronizeRealmTermsOfUse(realmName, null);
    }

    private void localSynchronizeRealmTermsOfUse(String realmName, String termsOfUseHtml) {
        String filePath = getTermsOfUseFile(realmName);
        File htmlFile = new File(filePath);
        String fileContent;
        if(!htmlFile.exists()) {
            fileContent = termsOfUseHtml != null ? termsOfUseHtml : defaultTermsOfUseHtml;
            try {
                Commons.writeFile(filePath, fileContent);
            }
            catch(IOException ex){
                logger.error("Theme - Could not write to theme's 'terms of use' file: " + filePath);
            }
        }
        else {
            try {
                fileContent = termsOfUseHtml != null ? termsOfUseHtml : Commons.readFile(filePath);
                if(termsOfUseHtml != null)
                    Commons.writeFile(filePath, fileContent);
            }
            catch (IOException ex){
                logger.error("Theme - Could not read theme's 'terms of use' file: " + filePath);
                fileContent = defaultTermsOfUseHtml;
            }
        }
        if(realmsTermsOfUseHtml.keySet().contains(realmName))
            realmsTermsOfUseHtml.replace(realmName, fileContent);
        else
            realmsTermsOfUseHtml.put(realmName, fileContent);
    }

    private void loadDefaultTermsOfUse(){
        if(defaultTermsOfUseHtml != null)
            return;
        InputStream is = TermsOfUse.class.getClassLoader().getResourceAsStream("termsofuse.html");
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = is.read(buffer)) != -1; )
                result.write(buffer, 0, length);
            defaultTermsOfUseHtml = result.toString("UTF-8");
        }
        catch(IOException ex) {
            logger.error("Could not initialize the 'terms of use' html snippet of this theme. Expect some problems if this theme is used", ex);
        }
    }



    /**
     * This is intended to be run ONLY upon the shutdown. DO NOT call it in any other case!
     */
    public static void shutdownAllWatchersAndListeners() {
        logger.info("Shutting down the watch service of the theme's terms of use folder");
        watchKey.cancel();
        try {
            watchService.close();
        }
        catch(IOException ex){
            logger.info("Could not shutdown the watch service of the theme's terms of use folder");
        }
    }

}
