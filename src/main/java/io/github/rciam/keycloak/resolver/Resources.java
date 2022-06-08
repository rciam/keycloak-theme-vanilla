package io.github.rciam.keycloak.resolver;

import io.github.rciam.keycloak.exception.InaccessibleFileException;
import io.github.rciam.keycloak.exception.InvalidPathException;
import io.github.rciam.keycloak.resolver.stubs.cache.CacheKey;
import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.events.RealmRemovedEvent;
import org.keycloak.models.cache.infinispan.events.RealmUpdatedEvent;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class Resources {

    private static final Logger logger = Logger.getLogger(Resources.class);

    private static String CACHE_NAME = "realm-resources";
    private static long MAX_RESOURCES_IN_CACHE = 50;
    private static Long MAX_CACHE_FILE_SIZE_BYTES = 1048576L; //1MB

    private static boolean REALMS_LISTENER_ADDED = false;
    private static final String CREATE_RESOURCE = "CREATE_RESOURCE";
    private static final String DELETE_RESOURCE = "DELETE_RESOURCE";
    private static ClusterProvider cluster;
    private static boolean FOLDER_INITIALIZED = false;

    private static WatchService watchService;
    private static Map<WatchKey, Map.Entry<Path, String>> watchKeys;

    private static Cache<CacheKey, byte[]> realmsResources;

    public Resources(KeycloakSession session){
        initializeStatics(session);
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

    /**
     * Does not broadcast, because it's not safe to send any configuration or code through unsecure
     * plaintext (serialized) event messages and materialize on the other nodes
    */
    public void setResource(CacheKey cacheKey, byte[] resource) {
        saveFilesystemResource(cacheKey.getRealmName(), cacheKey.getResourceName(), resource);
        if(resource != null && resource.length < MAX_CACHE_FILE_SIZE_BYTES)
            realmsResources.put(cacheKey, resource);
    }

    private void initializeStatics(KeycloakSession session) {

        if(!FOLDER_INITIALIZED) {
            try {
                Files.createDirectories(Paths.get(getBaseResourcesFolderPath()));
                FOLDER_INITIALIZED = true;
            } catch (IOException e) {
                logger.error(String.format("Could not create theme's resources base folder: %s  That's serious, please fix it.", getBaseResourcesFolderPath()));
            }
        }

        if(realmsResources == null){
            GlobalConfigurationBuilder globalConfigBuilder = new GlobalConfigurationBuilder();
            DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfigBuilder.nonClusteredDefault().build());
            ConfigurationBuilder cacheConfigBuilder = new ConfigurationBuilder();
            Configuration cacheConfiguration = cacheConfigBuilder
                    .simpleCache(true)
                    .memory()
                    .whenFull(EvictionStrategy.REMOVE)
                    .maxCount(MAX_RESOURCES_IN_CACHE)
                    .build();
            realmsResources = cacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).getOrCreateCache(CACHE_NAME, cacheConfiguration);
        }

        if(cluster == null)
            cluster = session.getProvider(ClusterProvider.class);

        if(!REALMS_LISTENER_ADDED){
            //register local listener
            session.getKeycloakSessionFactory().register(event -> {
                if(event instanceof RealmModel.RealmCreationEvent) {
                    String realmId = ((RealmModel.RealmCreationEvent)event).getCreatedRealm().getId();
                    String realmName = ((RealmModel.RealmCreationEvent)event).getCreatedRealm().getName();
                    //create realm folder (if not exists)
                    createRealmResourcesFolder(realmName);
                    //add file listener to the folder (if listener does not exist)
                    try {
                        registerWatch(Paths.get(getResourcesFolderPathOfRealm(realmName)), realmName);
                    }
                    catch(IOException ex){
                        logger.error(String.format("Theme: Could not monitor folder %s for file changes. Expect serious problem with the terms of use in the UI", getResourcesFolderPathOfRealm(realmName)));
                    }

                    cluster.notify(CREATE_RESOURCE, RealmUpdatedEvent.create(realmId, realmName), true, ClusterProvider.DCNotify.ALL_BUT_LOCAL_DC); //broadcast creation event to all other cluster nodes
                }
                else if(event instanceof RealmModel.RealmRemovedEvent) {
                    String realmId = ((RealmModel.RealmRemovedEvent)event).getRealm().getId();
                    String realmName = ((RealmModel.RealmRemovedEvent)event).getRealm().getName();
                    //remove the file listener
                    deregisterWatch(realmName);
                    //clear the cache of this realm
                    realmsResources.keySet().stream().filter(cacheKey -> realmName.equals(cacheKey.getRealmName())).collect(Collectors.toList())
                            .stream().forEach(cacheKey -> realmsResources.evict(cacheKey));
                    //remove the whole dir contents
                    Commons.deleteFolderAndContents(new File(getResourcesFolderPathOfRealm(realmName)));

                    cluster.notify(DELETE_RESOURCE, RealmRemovedEvent.create(realmId, realmName), true, ClusterProvider.DCNotify.ALL_BUT_LOCAL_DC); //broadcast deletion event to all other cluster nodes
                }
            });

            //register cluster listeners (these are events coming from the other nodes)
            cluster.registerListener(CREATE_RESOURCE, (ClusterEvent event) -> {
                RealmUpdatedEvent realmUpdatedEvent = (RealmUpdatedEvent) event;
                //create realm folder (if not exists)
                createRealmResourcesFolder(realmUpdatedEvent.getId());
                //add file listener to the folder (if listener does not exist)
                try {
                    registerWatch(Paths.get(getResourcesFolderPathOfRealm(realmUpdatedEvent.getId())), realmUpdatedEvent.getId());
                }
                catch(IOException ex){
                    logger.error(String.format("Theme: Could not monitor folder %s for file changes. Expect serious problem with the terms of use in the UI", getResourcesFolderPathOfRealm(realmUpdatedEvent.getId())));
                }
            });
            cluster.registerListener(DELETE_RESOURCE, (ClusterEvent event) -> {
                RealmRemovedEvent realmRemovedEvent = (RealmRemovedEvent) event;
                //remove the file listener
                deregisterWatch(realmRemovedEvent.getId());
                //clear the cache of this realm
                realmsResources.keySet().stream().filter(cacheKey -> realmRemovedEvent.getId().equals(cacheKey.getRealmName())).collect(Collectors.toList())
                        .stream().forEach(cacheKey -> realmsResources.evict(cacheKey));
                //remove the whole dir contents
                Commons.deleteFolderAndContents(new File(getResourcesFolderPathOfRealm(realmRemovedEvent.getId())));
            });

            REALMS_LISTENER_ADDED = true;
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

//                            System.out.format("%s: %s\n", event.kind().name(), child);

                            if (Files.isDirectory(child, NOFOLLOW_LINKS)) //if it is a directory, do not process the event
                                continue;

                            //not a directory -> it's a file, so process the event
                            if (event.kind() == ENTRY_CREATE) {
                                //read the resource and add it in the cache
                                try {
                                    String[] realmAndResourceName = getRealmAndResourceName(child);
                                    byte[] data = readFilesystemResource(child.toString());
                                    if(data != null && data.length < MAX_CACHE_FILE_SIZE_BYTES)
                                        realmsResources.put(new CacheKey(realmAndResourceName[0], realmAndResourceName[1]), data);
                                } catch (InaccessibleFileException | InvalidPathException e) {
                                    logger.error(e.getMessage());
                                }
                            }

                            if(event.kind() == ENTRY_MODIFY) {
                                //read the resource and add it in the cache
                                try {
                                    String[] realmAndResourceName = getRealmAndResourceName(child);
                                    byte[] data = readFilesystemResource(child.toString());
                                    if(data != null && data.length < MAX_CACHE_FILE_SIZE_BYTES)
                                        realmsResources.put(new CacheKey(realmAndResourceName[0], realmAndResourceName[1]), data);
                                } catch (InaccessibleFileException | InvalidPathException e) {
                                    logger.error(e.getMessage());
                                }
                            }

                            if (event.kind() == ENTRY_DELETE) {
                                //remove the resource from the cache
                                try {
                                    String[] realmAndResourceName = getRealmAndResourceName(child);
                                    realmsResources.evict(new CacheKey(realmAndResourceName[0], realmAndResourceName[1]));
                                } catch (InvalidPathException e) {
                                    logger.error(e.getMessage());
                                }
                            }

                        }

                        // reset key and remove from set if directory no longer accessible
                        boolean valid = key.reset();
                        if (!valid) {
                            watchKeys.remove(key);
//                            // all directories are inaccessible
//                            if (watchKeys.isEmpty()) {
//                                break;
//                            }
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

    private String getResourcesFolderPathOfRealm(String realmName){
        return String.format("%s/%s/%s/%s", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER, Commons.RESOURCES_FOLDER, realmName);
    }

    private String getResourceFilePath(String realmName, String resourceName){
        return String.format("%s/%s/%s/%s/%s", Commons.getBasePath(), Commons.THEME_WORKING_FOLDER, Commons.RESOURCES_FOLDER, realmName, resourceName);
    }


    private void createRealmResourcesFolder(String realmName){
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
        if(splits.length != 2) // should never happen, means that the folder has depth >1 from basePath
            throw new InvalidPathException(String.format("Should have a path of %s/<REALM_NAME>/fileXYZ , got: %s", getBaseResourcesFolderPath(), path.toString()));
        return splits;
    }


    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    private void registerWatch(Path dir, String realmName) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        watchKeys.put(key, new AbstractMap.SimpleEntry<Path, String>(dir, realmName));
    }

    private void deregisterWatch(String realmName) {
        if(realmName == null)
            return;
        int initialSize = watchKeys.size();
        List<WatchKey> watchkeysToRemove = watchKeys.entrySet().stream()
                .filter(realmAndPathEntry -> realmName.equals(realmAndPathEntry.getValue().getValue()))
                .map(realmAndPathEntry -> realmAndPathEntry.getKey())
                .collect(Collectors.toList());
        for(WatchKey watchKey : watchkeysToRemove){
            watchKey.cancel();
            watchKeys.remove(watchKey);
        }
        if(watchKeys.size() + watchkeysToRemove.size() == initialSize)
            logger.error(String.format("Should remove %d watch(es) but removed %d for realm %s", watchkeysToRemove.size(), initialSize - watchKeys.size(), realmName));
    }


}
