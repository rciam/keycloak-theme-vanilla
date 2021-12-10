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

import io.github.rciam.keycloak.resolver.TermsOfUse;
import io.github.rciam.keycloak.resolver.ThemeConfig;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThemeResourceProvider implements RealmResourceProvider {

    private KeycloakSession session;
    private static ThemeConfig themeConfig;
    private static TermsOfUse termsOfUse;

    public ThemeResourceProvider(KeycloakSession session) {
        this.session = session;
        if(themeConfig == null)
            themeConfig = new ThemeConfig(session);
        if(termsOfUse == null)
            termsOfUse = new TermsOfUse(session);
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }


    @GET
    @Path("/theme-config")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,List<String>> getThemeConfig() {
        return themeConfig.getConfig(session.getContext().getRealm().getName());
    }


    @GET
    @Path("/terms-of-use")
    @Produces(MediaType.TEXT_HTML)
    public String getTermsOfUse(
            @QueryParam("complete") @DefaultValue("true") Optional<Boolean> completeOp
    ) {
        boolean complete = completeOp.isPresent() ? completeOp.get() : true;
        return complete ? "<!DOCTYPE html><html><body>" + termsOfUse.getTermsOfUse(session.getContext().getRealm().getName()) + "</body></html>" : termsOfUse.getTermsOfUse(session.getContext().getRealm().getName());
    }



    /**
     * This should be used from login pages to show all available identity providers of the realm for logging in.
     * It has to be a public endpoint.
     */
    @GET
    @Path("/identity-providers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IdentityProviderBean.IdentityProvider> getIdentityProviders(
            @QueryParam("keyword") @DefaultValue("") String keyword,
            @QueryParam("first") @DefaultValue("0") Integer firstResult,
            @QueryParam("max") @DefaultValue("2147483647") Integer maxResults,
            @QueryParam("client_id") @DefaultValue("") String clientId,
            @QueryParam("tab_id") @DefaultValue("") String tabId,
            @QueryParam("session_code") @DefaultValue("") String sessionCode
    ) {
        if(firstResult < 0 || maxResults < 0)
            throw new BadRequestException("Should specify params firstResult and maxResults to be >= 0");
        RealmModel realm = session.getContext().getRealm();
        final String lowercaseKeyword = keyword.toLowerCase();
        List<IdentityProviderModel> identityProviders = realm.getIdentityProvidersStream()
                .filter(idp -> {
                    String name = idp.getDisplayName() == null ? "" : idp.getDisplayName();
                    return name.toLowerCase().contains(lowercaseKeyword) || idp.getAlias().toLowerCase().contains(lowercaseKeyword);
                })
                .skip(firstResult)
                .limit(maxResults)
                .collect(Collectors.toList());

        //this translates to http 204 code (instead of an empty list's 200). Is used to specify that its a end-of-stream.
        if(identityProviders.isEmpty())
            return null;

        AuthenticationSessionManager authSessionManager = new AuthenticationSessionManager(session);
        AuthenticationSessionModel authSessionModel = authSessionManager.getCurrentAuthenticationSession(realm, realm.getClientByClientId(clientId), tabId);
        identityProviders = filterIdentityProviders(identityProviders.stream(), session, authSessionModel);

        //Expose through the Bean, because it makes some extra processing. URI is re-composed back in the UI, so we can ignore here
        //returns empty list if all idps are filtered out, and not null. This is important for the UI
        IdentityProviderBean idpBean = new IdentityProviderBean(realm, session, identityProviders, URI.create(""));
        return idpBean.getProviders()!=null ? idpBean.getProviders() : new ArrayList<>();
    }


    /**
     * This should be used from login pages to show any promoted identity providers of the realm for logging in with.
     * It has to be a public endpoint.
     */
    @GET
    @Path("/identity-providers-promoted")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IdentityProviderBean.IdentityProvider> getPromotedIdentityProviders() {
        RealmModel realm = session.getContext().getRealm();
        List<IdentityProviderModel> promotedProviders = new ArrayList<>();
        realm.getIdentityProvidersStream().forEach(idp -> {
            if(idp.getConfig()!=null && "true".equals(idp.getConfig().get("promotedLoginbutton")))
                promotedProviders.add(idp);
        });

        //Expose through the Bean, because it makes some extra processing. URI is re-composed back in the UI, so we can ignore here
        IdentityProviderBean idpBean = new IdentityProviderBean(realm, session, promotedProviders, URI.create(""));
        return idpBean.getProviders()!=null ? idpBean.getProviders() : new ArrayList<>();

    }


    /**
     * <b> This is actually the function LoginFormsUtil.filterIdentityProviders() </b>
     *
     * @param providers
     * @param session
     * @param authSession
     * @return
     */
    public static List<IdentityProviderModel> filterIdentityProviders(Stream<IdentityProviderModel> providers, KeycloakSession session, AuthenticationSessionModel authSession) {
        if (authSession != null) {
            SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, "BROKERED_CONTEXT");
            if (serializedCtx != null) {
                IdentityProviderModel idp = serializedCtx.deserialize(session, authSession).getIdpConfig();
                return (List)providers.filter((p) -> {
                    return !Objects.equals(p.getAlias(), idp.getAlias());
                }).collect(Collectors.toList());
            }
        }

        return (List)providers.collect(Collectors.toList());
    }



}
