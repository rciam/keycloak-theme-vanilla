package io.github.rciam.keycloak.resolver;

import io.github.rciam.keycloak.exception.InaccessibleFileException;
import io.github.rciam.keycloak.exception.InvalidPathException;
import io.github.rciam.keycloak.resolver.stubs.cache.CacheKey;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class Resources {

    private static final Logger logger = Logger.getLogger(Resources.class);

    private static long MAX_RESOURCES_IN_CACHE = 50;
    private static Long MAX_CACHE_FILE_SIZE_BYTES = 1048576L; //1MB
    private static boolean FOLDER_INITIALIZED = false;

    private static WatchService watchService;
    private static Map<WatchKey, Map.Entry<Path, String>> watchKeys;

    private static Map<CacheKey, byte[]> realmsResources;

    public Resources(){
        initializeStatics();
    }

    public byte[] getResource(CacheKey cacheKey){
        byte[] payload = realmsResources.get(cacheKey);
        if(payload == null){ //search filesystem
            String filePath = getResourceFilePath(cacheKey.getRealmName(), cacheKey.getResourceName());
            try {
                payload = Commons.readRawFile(filePath);
                if(payload != null && payload.length < MAX_CACHE_FILE_SIZE_BYTES)
                    realmsResources.put(cacheKey, payload);
            } catch(IOException ex){
                logger.error(String.format("Theme: could not load resource file %s of realm %s at %s", cacheKey.getResourceName(), cacheKey.getRealmName(), filePath));
            }
        }
        return payload;
    }

    private void initializeStatics() {

        logger.info("Initializing Resources of theme");
        if(!FOLDER_INITIALIZED) {
            try {
                Files.createDirectories(Paths.get(getBaseResourcesFolderPath()));
                FOLDER_INITIALIZED = true;
            } catch (IOException e) {
                logger.error(String.format("Could not create theme's resources base folder: %s  That's serious, please fix it.", getBaseResourcesFolderPath()));
            }
        }

        if(realmsResources == null){
            // Create a thread-safe LRU Map natively in Java
            int initialCapacity = (int) MAX_RESOURCES_IN_CACHE + 1;
            realmsResources = Collections.synchronizedMap(
                    new LinkedHashMap<CacheKey, byte[]>(initialCapacity, 0.75F, true) {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<CacheKey, byte[]> eldest) {
                            return size() > MAX_RESOURCES_IN_CACHE;
                        }
                    }
            );
        }

        if(watchService == null) {

            Runnable runnable = () -> {

                try {
                    watchService = FileSystems.getDefault().newWatchService();
                    watchKeys = new HashMap<WatchKey, Map.Entry<Path, String>>();
                    registerWatch(Paths.get(getBaseResourcesFolderPath()), null);

                    for (;;) {
                        WatchKey key;
                        try {
                            key = watchService.take();
                        } catch (InterruptedException x) {
                            return;
                        }
                        Path dir = watchKeys.get(key).getKey();
                        for (WatchEvent<?> event: key.pollEvents()) {

                            // Context for directory entry event is the file name of entry
                            WatchEvent<Path> ev = cast(event);
                            Path name = ev.context();
                            Path child = dir.resolve(name);

                            if (Files.isDirectory(child, NOFOLLOW_LINKS)) //if it is a directory, do not process the event
                                continue;

                            //not a directory -> it's a file, so process the event
                            if (event.kind() == ENTRY_CREATE || event.kind() == ENTRY_MODIFY) {
                                //read the resource and add it in the cache
                                try {
                                    String[] realmAndResourceName = getRealmAndResourceName(child);
                                    byte[] data = readFilesystemResource(child.toString());
                                    if(data != null && data.length < MAX_CACHE_FILE_SIZE_BYTES)
                                        realmsResources.put(new CacheKey(realmAndResourceName[0], realmAndResourceName[1]), data);
                                } catch (InaccessibleFileException | InvalidPathException e) {
                                    logger.warn(e.getMessage());
                                }
                            }

                            if (event.kind() == ENTRY_DELETE) {
                                //remove the resource from the cache
                                try {
                                    String[] realmAndResourceName = getRealmAndResourceName(child);
                                    // Changed from .evict() to standard Map .remove()
                                    realmsResources.remove(new CacheKey(realmAndResourceName[0], realmAndResourceName[1]));
                                } catch (InvalidPathException e) {
                                    logger.warn(e.getMessage());
                                }
                            }

                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            watchKeys.remove(key);
                        }
                    }

                }
                catch(IOException ex){
                    logger.error(String.format("Theme's terms of use files - Could not monitor folder %s for file changes. Expect serious problem with the terms of use in the UI", getBaseResourcesFolderPath()));
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }

    }

    private String getBaseResourcesFolderPath(){
        return String.format("%s/%s/%s", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER, Commons.RESOURCES_FOLDER);
    }

    public String getResourcesFolderPathOfRealm(String realmName){
        return String.format("%s/%s/%s/%s", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER, Commons.RESOURCES_FOLDER, realmName);
    }

    private String getResourceFilePath(String realmName, String resourceName){
        return String.format("%s/%s/%s/%s/%s", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER, Commons.RESOURCES_FOLDER, realmName, resourceName);
    }

    public void createRealmResourcesFolder(String realmName){
        new File(getResourcesFolderPathOfRealm(realmName)).mkdirs();
    }

    public void saveFilesystemResource(String realmName, String resourceName, byte[] data) {
        createRealmResourcesFolder(realmName);
        String path = getResourceFilePath(realmName, resourceName);
        try {
            Commons.writeFile(path, data);
        }
        catch(IOException ex){
            logger.error(String.format("Theme - Could not write the file %s of the realm %s at the filepath %s. Please check the file permissions", resourceName, realmName, path));
        }
    }

    private byte[] readFilesystemResource(String path) throws InaccessibleFileException {
        try {
            return Commons.readRawFile(path);
        }
        catch(IOException ex) {
            throw new InaccessibleFileException(String.format("Theme - Could not read the file from the filepath %s. Please check the file existence or permissions", path));
        }
    }

    private String[] getRealmAndResourceName(Path path) throws InvalidPathException {
        String subpath = path.toString().replace(getBaseResourcesFolderPath(),"");
        subpath = subpath.startsWith(File.separator) ? subpath.substring(1) : subpath;
        subpath = subpath.endsWith(File.separator) ? subpath.substring(0, subpath.length()-1) : subpath;
        String [] splits = subpath.split("/");
        if (splits.length != 2) // should never happen, means that the folder has depth >1 from basePath
            throw new InvalidPathException(String.format("Should have a path of %s/<REALM_NAME>/fileXYZ , got: %s", getBaseResourcesFolderPath(), path.toString()));
        return splits;
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    public void registerWatch(Path dir, String realmName) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        watchKeys.put(key, new AbstractMap.SimpleEntry<Path, String>(dir, realmName));
    }

    public void deregisterWatch(String realmName) {
        if(realmName == null)
            return;
        List<WatchKey> watchkeysToRemove = watchKeys.entrySet().stream()
                .filter(realmAndPathEntry -> realmName.equals(realmAndPathEntry.getValue().getValue()))
                .map(realmAndPathEntry -> realmAndPathEntry.getKey())
                .collect(Collectors.toList());
        for(WatchKey watchKey : watchkeysToRemove){
            watchKey.cancel();
            watchKeys.remove(watchKey);
        }
    }

    public static Map<CacheKey, byte[]> getRealmsResources() {
        return realmsResources;
    }

    public void evictRealmResources(String realmName) {
        if (realmName == null || realmsResources == null) return;
        // Safely remove all keys matching the realm name
        synchronized (realmsResources) {
            realmsResources.keySet().removeIf(key -> realmName.equals(key.getRealmName()));
        }
    }
}