package io.github.rciam.keycloak.resolver.stubs.cluster;

import org.keycloak.cluster.ClusterEvent;
import java.io.Serializable;

public class RealmCreatedEvent implements ClusterEvent, Serializable {
    private static final long serialVersionUID = 1L;

    private String realmName;

    // Default constructor for Jackson
    public RealmCreatedEvent() {
    }

    public RealmCreatedEvent(String realmName) {
        this.realmName = realmName;
    }

    public static RealmDeletedEvent create(String realmName) {
        return new RealmDeletedEvent(realmName);
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }
}
