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

import io.github.rciam.keycloak.cookie.RciamCookieProvider;
import io.github.rciam.keycloak.resolver.Resources;
import io.github.rciam.keycloak.resolver.TermsOfUse;
import io.github.rciam.keycloak.resolver.ThemeConfig;
import io.github.rciam.keycloak.resolver.stubs.Configuration;
import io.github.rciam.keycloak.resolver.stubs.cache.CacheKey;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.http.FormPartValue;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrderedModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.theme.Theme;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThemeResourceProvider implements RealmResourceProvider {

    private static final Logger logger = Logger.getLogger(ThemeResourceProvider.class);
    private static final String KEYCLOAK_REMEMBER_IDPS = "KEYCLOAK_REMEMBER_IDPS_";
    private static final String ICON_THEME_PREFIX = "kcLogoIdP-";
    private static final String IDP_THEME_CONFIG_PREFIX = "kcTheme-";
    private static final String ALIAS_IN = "aliasIn";

    protected ClientConnection clientConnection;

    private KeycloakSession session;
    private static ThemeConfig themeConfig;
    private static TermsOfUse termsOfUse;
    private static Resources resources;

    public ThemeResourceProvider(KeycloakSession session) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        if (themeConfig == null) {
            themeConfig = new ThemeConfig(session);
        }
        if (termsOfUse == null) {
            termsOfUse = new TermsOfUse(session);
        }
        if (resources == null) {
            resources = new Resources(session);
        }
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

    @POST
    @Path("/theme-config")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setThemeConfig(@Context final HttpHeaders headers, Configuration config) {
        if(!isRealmManager(headers))
            return Response.status(401).build();
        themeConfig.setConfig(session.getContext().getRealm().getName(), config);
        return Response.status(201).build();
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

    @POST
    @Path("/terms-of-use")
    @Consumes(MediaType.TEXT_HTML)
    public Response setTermsOfUse(@Context final HttpHeaders headers, String termsOfUseHtml) {
        if(!isRealmManager(headers))
            return Response.status(401).build();
        termsOfUse.setTermsOfUse(session.getContext().getRealm().getName(), termsOfUseHtml);
        return Response.status(201).build();
    }


    @GET
    @Path("/resource/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getResource(@PathParam("filename") String filename) {
        RealmModel realm = session.getContext().getRealm();
        byte[] data = resources.getResource(new CacheKey(realm.getName(), filename));

        if(data == null)
            return Response.status(404).entity("Could not find the resource "+filename).build();

        return Response.ok()
                .header("Content-Disposition","attachment; filename=\"" + filename + "\"")
                .entity(data)
                .build();
    }

    @POST
    @Path("/resource/{filename}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response setResource(@Context final HttpHeaders headers, @PathParam("filename") String filename) {
        if(!isRealmManager(headers))
            return Response.status(401).build();
        RealmModel realm = session.getContext().getRealm();
        MultivaluedMap<String, FormPartValue> formDataMap = session.getContext().getHttpRequest().getMultiPartFormParameters();
        if (!formDataMap.containsKey("file")) {
            return Response.status(400).entity("Should have one file uploaded. Please attach the file content in the multimap for the file "+ filename).build();
        }
        InputStream inputStream = formDataMap.getFirst("file").asInputStream();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int temp;
            byte[] buffer = new byte[1000];
            while ((temp = inputStream.read(buffer)) != -1)
                byteArrayOutputStream.write(buffer, 0, temp);
            resources.saveFilesystemResource(realm.getName(), filename, byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            logger.errorf(e, "Problem uploading file with name %s", filename);
            return Response.status(Response.Status.BAD_REQUEST).entity("Problem uploading file with name "+ filename).build();
        }

        return Response.status(201).build();
    }


    /**
     * This should be used from login pages to show all available identity providers of the realm for logging in.
     * It has to be a public endpoint.
     */
    @GET
    @Path("/identity-providers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RciamIdentityProvider> getIdentityProviders(
            @QueryParam("keyword") @DefaultValue("") String keyword,
            @QueryParam("first") @DefaultValue("0") Integer firstResult,
            @QueryParam("max") @DefaultValue("2147483647") Integer maxResults,
            @QueryParam("client_id") @DefaultValue("") String clientId,
            @QueryParam("tab_id") @DefaultValue("") String tabId,
            @QueryParam("session_code") @DefaultValue("") String sessionCode
    ) {
        if (firstResult < 0 || maxResults < 0)
            throw new BadRequestException("Should specify params firstResult and maxResults to be >= 0");
        RealmModel realm = session.getContext().getRealm();

        //login options together with searching alias or display name
        Map<String, String> searchOptions = IdentityProviderStorageProvider.LoginFilter.getLoginSearchOptions();
        searchOptions.put(IdentityProviderModel.SEARCH, "*" + keyword + "*");
        AuthenticationSessionManager authSessionManager = new AuthenticationSessionManager(session);
        searchOptions.put(IdentityProviderModel.ALIAS_NOT_IN, getExistingIDP(session, authSessionManager.getCurrentAuthenticationSession(realm, realm.getClientByClientId(clientId), tabId)));

        //Expose through the Bean, because it makes some extra processing. URI is re-composed back in the UI, so we can ignore
        URI uri = UriBuilder.fromUri(session.getContext().getUri().getBaseUri().getPath()).build();
        return session.identityProviders().getAllStream(searchOptions, firstResult, maxResults).map(idp -> createIdentityProvider(realm, uri, idp)).toList();
    }

    //similar to the method of IdentityProviderBean - no cjheck with user
    private String getExistingIDP(KeycloakSession session, AuthenticationSessionModel authSession) {

        String existingIDPAlias = null;
        if (authSession != null && Objects.equals(LoginActionsService.FIRST_BROKER_LOGIN_PATH, authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH))) {
            SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
            final IdentityProviderModel existingIdp = (serializedCtx == null) ? null : serializedCtx.deserialize(session, authSession).getIdpConfig();
            if (existingIdp != null) {
                existingIDPAlias = existingIdp.getAlias();
            }

        }
        return existingIDPAlias;
    }

    /**
     * This should be used from login pages to show any promoted identity providers of the realm for logging in with.
     * It has to be a public endpoint.
     */
    @GET
    @Path("/identity-providers-promoted")
    @Produces(MediaType.APPLICATION_JSON)
    public PromotedBean getPromotedIdentityProviders() {
        RealmModel realm = session.getContext().getRealm();
        URI uri = UriBuilder.fromUri(session.getContext().getUri().getBaseUri().getPath()).build();

        List<RciamIdentityProvider> lastLoginIdPs = new ArrayList<>();
        String idpsCookie = session.getProvider(RciamCookieProvider.class).get(KEYCLOAK_REMEMBER_IDPS + realm.getId());
        String lastLoginIdPAliasStr = null;
        if (StringUtil.isNotBlank(idpsCookie)) {
            try {
                List<String> lastLoginIdPAlias = JsonSerialization.readValue(URLDecoder.decode(idpsCookie, StandardCharsets.UTF_8), List.class);
                lastLoginIdPAliasStr = lastLoginIdPAlias.stream().collect(Collectors.joining(","));
                Map<String, String> searchOptions = IdentityProviderStorageProvider.LoginFilter.getLoginSearchOptions();
                searchOptions.put("promotedLoginbutton", "true");
                searchOptions.put("aliasIn", lastLoginIdPAliasStr);
                lastLoginIdPs = session.identityProviders().getAllStream(searchOptions, null, null).map(idp -> createIdentityProvider(realm,uri,idp)).collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<String, String> searchOptionsPromoted = IdentityProviderStorageProvider.LoginFilter.getLoginSearchOptions();
        searchOptionsPromoted.put("promotedLoginbutton", "true");
        if (lastLoginIdPAliasStr != null) {
            searchOptionsPromoted.put(IdentityProviderModel.ALIAS_NOT_IN, lastLoginIdPAliasStr);
        }
        List<RciamIdentityProvider> promotedProviders = session.identityProviders().getAllStream(searchOptionsPromoted, null, null).map(idp -> createIdentityProvider(realm, uri, idp)).collect(Collectors.toList());

        return new PromotedBean(promotedProviders, lastLoginIdPs);

    }

    //needed private methods of IdentityProviderBean
    private RciamIdentityProvider createIdentityProvider(RealmModel realm, URI baseURI, IdentityProviderModel identityProvider) {
        String loginUrl = Urls.identityProviderAuthnRequest(baseURI, identityProvider.getAlias(), realm.getName()).toString();
        String displayName = KeycloakModelUtils.getIdentityProviderDisplayName(session, identityProvider);
        Map<String, String> themeConfig = identityProvider.getConfig().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(IDP_THEME_CONFIG_PREFIX))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(IDP_THEME_CONFIG_PREFIX.length()),
                        Map.Entry::getValue
                ));
        return new RciamIdentityProvider(identityProvider.getAlias(),
                displayName, identityProvider.getProviderId(), loginUrl,
                identityProvider.getConfig().get("guiOrder"), getLoginIconClasses(identityProvider), themeConfig, identityProvider.getConfig().get("logoUri"));
    }

    private String getLoginIconClasses(IdentityProviderModel identityProvider) {
        try {
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Optional<String> classesFromTheme = Optional.ofNullable(getLogoIconClass(identityProvider, theme.getProperties()));
            Optional<String> classesFromModel = Optional.ofNullable(identityProvider.getDisplayIconClasses());
            return classesFromTheme.orElse(classesFromModel.orElse(""));
        } catch (IOException e) {
            //NOP
        }
        return "";
    }

    private String getLogoIconClass(IdentityProviderModel identityProvider, Properties themeProperties) throws IOException {
        String iconClass = themeProperties.getProperty(ICON_THEME_PREFIX + identityProvider.getAlias());

        if (iconClass == null) {
            return themeProperties.getProperty(ICON_THEME_PREFIX + identityProvider.getProviderId());
        }

        return iconClass;
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
                return providers.filter(p -> !Objects.equals(p.getAlias(), idp.getAlias())).collect(Collectors.toList());
            }
        }

        return providers.collect(Collectors.toList());
    }


    private boolean isRealmManager(HttpHeaders headers){
        AdminAuth adminAuth = authenticateRealmAdminRequest(headers);
        AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, session.getContext().getRealm(), adminAuth);
        return realmAuth.realm().canManageRealm();
    }

    /**
     * This snippet is from AdminRoot.authenticateRealmAdminRequest()
     */
    protected AdminAuth authenticateRealmAdminRequest(HttpHeaders headers) {
        RealmModel originalRealm = session.getContext().getRealm();
        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) throw new NotAuthorizedException("Bearer");
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotAuthorizedException("Unknown realm in token");
        }
        session.getContext().setRealm(realm);

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            logger.debug("Token not valid");
            throw new NotAuthorizedException("Bearer");
        }

        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null) {
            throw new NotFoundException("Could not find client for authorization");

        }

        AdminAuth adminAuth = new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client);
        session.getContext().setRealm(originalRealm);
        return adminAuth;
    }

    private class PromotedBean {
        public PromotedBean(){}
        public PromotedBean(List<RciamIdentityProvider> promotedIdPs, List<RciamIdentityProvider> lastLoginIdPs ){
            this.promotedIdPs = promotedIdPs;
            this.lastLoginIdPs = lastLoginIdPs;
        }
        private List<RciamIdentityProvider> promotedIdPs;
        private List<RciamIdentityProvider> lastLoginIdPs;

        public List<RciamIdentityProvider> getPromotedIdPs() {
            return promotedIdPs;
        }

        public void setPromotedIdPs(List<RciamIdentityProvider> promotedIdPs) {
            this.promotedIdPs = promotedIdPs;
        }

        public List<RciamIdentityProvider> getLastLoginIdPs() {
            return lastLoginIdPs;
        }

        public void setLastLoginIdPs(List<RciamIdentityProvider> lastLoginIdPs) {
            this.lastLoginIdPs = lastLoginIdPs;
        }

    }

    //based on IdentityProviderBean.IdentityProvider with adding logoUri
    public class RciamIdentityProvider implements OrderedModel {

        private final String alias;
        private final String providerId; // This refers to providerType (facebook, google, etc.)
        private final String loginUrl;
        private final String guiOrder;
        private final String displayName;
        private final String iconClasses;
        private final Map<String, String> themeConfig;
        private final String logoUri;

        public RciamIdentityProvider(String alias, String displayName, String providerId, String loginUrl, String guiOrder) {
            this(alias, displayName, providerId, loginUrl, guiOrder, "", null, null);
        }

        public RciamIdentityProvider(String alias, String displayName, String providerId, String loginUrl, String guiOrder, String iconClasses, Map<String, String> themeConfig, String logoUri) {
            this.alias = alias;
            this.displayName = displayName;
            this.providerId = providerId;
            this.loginUrl = loginUrl;
            this.guiOrder = guiOrder;
            this.iconClasses = iconClasses;
            this.themeConfig = themeConfig;
            this.logoUri = logoUri;
        }

        public String getAlias() {
            return alias;
        }

        public String getLoginUrl() {
            return loginUrl;
        }

        public String getProviderId() {
            return providerId;
        }

        @Override
        public String getGuiOrder() {
            return guiOrder;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconClasses() {
            return iconClasses;
        }

        public Map<String, String> getThemeConfig() {
            return themeConfig;
        }

        public String getLogoUri() {
            return logoUri;
        }
    }

}
