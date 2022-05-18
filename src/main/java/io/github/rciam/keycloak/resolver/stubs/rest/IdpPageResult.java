package io.github.rciam.keycloak.resolver.stubs.rest;

import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;

import java.util.List;


public class IdpPageResult {

    private List<IdentityProviderBean.IdentityProvider> identityProviders;
    private int hiddenIdps;  //hidden idps are the ones that are either hidden or disabled

    public IdpPageResult() {
    }

    public IdpPageResult(List<IdentityProviderBean.IdentityProvider> identityProviders, int hiddenIdps) {
        this.identityProviders = identityProviders;
        this.hiddenIdps = hiddenIdps;
    }

    public List<IdentityProviderBean.IdentityProvider> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<IdentityProviderBean.IdentityProvider> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public int getHiddenIdps() {
        return hiddenIdps;
    }

    public void setHiddenIdps(int hiddenIdps) {
        this.hiddenIdps = hiddenIdps;
    }
}
