package io.github.rciam.keycloak.cookie;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultRciamCookieProviderFactory  implements RciamCookieProviderFactory {

    @Override
    public RciamCookieProvider create(KeycloakSession session) {
        return new DefaultRciamCookieProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "default";
    }

}
