package io.github.rciam.keycloak.cookie;

import org.keycloak.provider.Provider;

public interface RciamCookieProvider extends Provider {
    String get(String cookieName);

}