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
import io.github.rciam.keycloak.resolver.stubs.Configuration;
import org.checkerframework.checker.units.qual.A;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThemeResourceProvider implements RealmResourceProvider {

    private static final Logger logger = Logger.getLogger(ThemeResourceProvider.class);

    @Context
    protected ClientConnection clientConnection;

    private KeycloakSession session;
    private static ThemeConfig themeConfig;
    private static TermsOfUse termsOfUse;

    public ThemeResourceProvider(KeycloakSession session) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        if (themeConfig == null) {
            themeConfig = new ThemeConfig(session);
        }
        if (termsOfUse == null) {
            termsOfUse = new TermsOfUse(session);
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


//    private static AdminAuth authenticateRealmAdminRequest(HttpHeaders headers, AdminRoot adminRoot) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        ResteasyProviderFactory.getInstance().injectProperties(adminRoot);
//        Method method = adminRoot.getClass().getDeclaredMethod("authenticateRealmAdminRequest", HttpHeaders.class);
//        method.setAccessible(true);
//        Object adminAuth = method.invoke(adminRoot, headers);
//        return (AdminAuth)adminAuth;
//    }

}
