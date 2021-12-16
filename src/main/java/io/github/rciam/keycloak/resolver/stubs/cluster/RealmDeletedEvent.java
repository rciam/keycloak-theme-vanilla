package io.github.rciam.keycloak.resolver.stubs.cluster;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.cluster.ClusterEvent;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@SerializeWith(RealmDeletedEvent.ExternalizerImpl.class)
public class RealmDeletedEvent implements ClusterEvent {

    private String realmName;

    public static RealmDeletedEvent create(String realmName) {
        RealmDeletedEvent event = new RealmDeletedEvent();
        event.realmName = realmName;
        return event;
    }

    public String getRealmName(){
        return realmName;
    }

    public static class ExternalizerImpl implements Externalizer<RealmDeletedEvent> {

        @Override
        public void writeObject(ObjectOutput output, RealmDeletedEvent obj) throws IOException {
            MarshallUtil.marshallString(obj.realmName, output);
        }

        @Override
        public RealmDeletedEvent readObject(ObjectInput input) throws IOException {
            RealmDeletedEvent res = new RealmDeletedEvent();
            res.realmName = MarshallUtil.unmarshallString(input);
            return res;
        }

    }

}
