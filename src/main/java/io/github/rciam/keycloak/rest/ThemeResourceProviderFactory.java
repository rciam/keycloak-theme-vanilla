/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.rciam.keycloak.rest;

import io.github.rciam.keycloak.resolver.Commons;
import io.github.rciam.keycloak.resolver.Resources;
import io.github.rciam.keycloak.resolver.TermsOfUse;
import io.github.rciam.keycloak.resolver.ThemeConfig;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.events.RealmRemovedEvent;
import org.keycloak.models.cache.infinispan.events.RealmUpdatedEvent;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ThemeResourceProviderFactory implements RealmResourceProviderFactory {

    private static final Logger logger = Logger.getLogger(Resources.class);

    public static final String ID = "theme-info";
    private static ThemeConfig themeConfig;
    private static TermsOfUse termsOfUse;
    private static Resources resources;
    private static ClusterProvider cluster;
    private static final String THEME_REALM_CREATED = "THEME_REALM_CREATED";
    private static final String THEME_REALM_DELETED = "THEME_REALM_DELETED";


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new ThemeResourceProvider(session, themeConfig, termsOfUse, resources);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        themeConfig = new ThemeConfig();
        termsOfUse = new TermsOfUse();
        resources = new Resources();
        KeycloakModelUtils.runJobInTransaction(factory, (KeycloakSession session) -> {
            cluster = session.getProvider(ClusterProvider.class);
            session.realms().getRealmsStream().forEach(realmModel -> {
                termsOfUse.localSynchronizeRealmTermsOfUse(realmModel.getName());
                themeConfig.localSynchronizeConfig(realmModel.getName());
            });
        });

        cluster.registerListener(THEME_REALM_CREATED, (ClusterEvent clusterEvent) -> {
            if (clusterEvent instanceof RealmUpdatedEvent realmEvent) {
                logger.infof("Event for theme configuration due to create realm with name %s",realmEvent.getId());
                createRealmAction(realmEvent.getId());
            }
        });
        cluster.registerListener(THEME_REALM_DELETED, (ClusterEvent clusterEvent) -> {
            if (clusterEvent instanceof RealmRemovedEvent realmEvent) {
                logger.infof("Event for theme configuration due to delete realm with name %s",realmEvent.getId());
                deleteRealmAction(realmEvent.getId());
            }
        });

        // Listen to Keycloak create and delete realm event
        factory.register(event -> {
            if (event instanceof RealmModel.RealmPostCreateEvent createEvent) {
                String realmName = createEvent.getCreatedRealm().getName();
                logger.infof("Theme configuration for creating realm with name %s",realmName);
                createRealmAction(realmName);
                cluster.notify(THEME_REALM_CREATED,RealmUpdatedEvent.create(realmName,realmName), true);
            } else if (event instanceof RealmModel.RealmRemovedEvent removeEvent) {
                String realmName = removeEvent.getRealm().getName();
                logger.infof("Theme configuration for deleting realm with name %s",realmName);
                deleteRealmAction(realmName);
                cluster.notify(THEME_REALM_DELETED,RealmRemovedEvent.create(realmName,realmName), true);
            }
        });
    }

    private void createRealmAction(String realmName){
        termsOfUse.localSynchronizeRealmTermsOfUse(realmName);

        //create realm folder (if not exists)
        resources.createRealmResourcesFolder(realmName);
        //add file listener to the folder (if listener does not exist)
        try {
            resources.registerWatch(Paths.get(resources.getResourcesFolderPathOfRealm(realmName)), realmName);
        }
        catch(IOException ex){
            logger.error(String.format("Theme: Could not monitor folder %s for file changes. Expect serious problem with the terms of use in the UI", resources.getResourcesFolderPathOfRealm(realmName)));
        }

        //theme config
        themeConfig.localSynchronizeConfig(realmName);
    }

    private void deleteRealmAction(String realmName){
        String filePath = termsOfUse.getTermsOfUseFile(realmName);
        File htmlFile = new File(filePath);
        if(htmlFile.exists())
            htmlFile.delete();

        //remove the file listener
        resources.deregisterWatch(realmName);
        //clear the cache of this realm
        resources.getRealmsResources().keySet().stream().filter(cacheKey -> realmName.equals(cacheKey.getRealmName())).collect(Collectors.toList())
                .stream().forEach(cacheKey -> resources.getRealmsResources().evict(cacheKey));
        //remove the whole dir contents
        Commons.deleteFolderAndContents(new File(resources.getResourcesFolderPathOfRealm(realmName)));

        //theme config
        String filePathConfig = themeConfig.getThemeConfigFilePath(realmName);
        File configFile = new File(filePathConfig);
        if(configFile.exists())
            configFile.delete();
    }

    @Override
    public void close() {
        TermsOfUse.shutdownAllWatchersAndListeners();
        ThemeConfig.shutdownAllWatchersAndListeners();
    }

}
