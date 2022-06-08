//package io.github.rciam.keycloak.resolver.stubs.cluster;
//
//import org.infinispan.commons.marshall.Externalizer;
//import org.infinispan.commons.marshall.MarshallUtil;
//import org.infinispan.commons.marshall.SerializeWith;
//import org.keycloak.cluster.ClusterEvent;
//
//import java.io.IOException;
//import java.io.ObjectInput;
//import java.io.ObjectOutput;
//
//@SerializeWith(RealmCreatedEvent.ExternalizerImpl.class)
//public class RealmCreatedEvent implements ClusterEvent {
//
//    private String realmName;
//
//    public static RealmCreatedEvent create(String realmName) {
//        RealmCreatedEvent event = new RealmCreatedEvent();
//        event.realmName = realmName;
//        return event;
//    }
//
//    public String getRealmName(){
//        return realmName;
//    }
//
//    public static class ExternalizerImpl implements Externalizer<RealmCreatedEvent> {
//
//        @Override
//        public void writeObject(ObjectOutput output, RealmCreatedEvent obj) throws IOException {
//            MarshallUtil.marshallString(obj.realmName, output);
//        }
//
//        @Override
//        public RealmCreatedEvent readObject(ObjectInput input) throws IOException {
//            RealmCreatedEvent res = new RealmCreatedEvent();
//            res.realmName = MarshallUtil.unmarshallString(input);
//            return res;
//        }
//
//    }
//
//}
