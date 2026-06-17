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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
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
            session.realms().getRealmsStream().forEach(realmModel -> {
                termsOfUse.localSynchronizeRealmTermsOfUse(realmModel.getName());
                themeConfig.localSynchronizeConfig(realmModel.getName());
            });
        });



        // Listen to Keycloak create and delete realm event
        factory.register(event -> {
            if (event instanceof RealmModel.RealmPostCreateEvent) {
                RealmModel.RealmPostCreateEvent createEvent =
                        (RealmModel.RealmPostCreateEvent) event;
                String realmName = createEvent.getCreatedRealm().getName();
                logger.infof("Theme configuration for creating realm with name %s",realmName);
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
            } else if (event instanceof RealmModel.RealmRemovedEvent) {
                RealmModel.RealmRemovedEvent removeEvent =
                        (RealmModel.RealmRemovedEvent) event;

                String realmName = removeEvent.getRealm().getName();
                logger.infof("Theme configuration for deleting realm with name %s",realmName);
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
        });
    }
    @Override
    public void close() {
        TermsOfUse.shutdownAllWatchersAndListeners();
        ThemeConfig.shutdownAllWatchersAndListeners();
    }

}
