package io.github.rciam.keycloak.cookie;

import jakarta.ws.rs.core.Cookie;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

public class DefaultRciamCookieProvider implements RciamCookieProvider {

    private final KeycloakSession session;

    private final Map<String, Cookie> cookies;

    public DefaultRciamCookieProvider(KeycloakSession session) {
        this.session = session;
        this.cookies = session.getContext().getRequestHeaders().getCookies();
    }

    @Override
    public String get(String cookieName) {
        Cookie cookie = cookies.get(cookieName);
        return cookie != null ? cookie.getValue() : null;
    }

    @Override
    public void close() {

    }
}