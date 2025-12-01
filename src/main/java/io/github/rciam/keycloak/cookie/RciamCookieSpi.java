package io.github.rciam.keycloak.cookie;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RciamCookieSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "cookie";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RciamCookieProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RciamCookieProviderFactory.class;
    }
}
