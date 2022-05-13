package io.github.rciam.keycloak.resolver.stubs.cache;

public class CacheKey {

    String realmName;
    String resourceName;

    public CacheKey(String realmName, String resourceName){
        this.realmName = realmName;
        this.resourceName = resourceName;
    }

    public String getRealmName() {
        return realmName;
    }

    public String getResourceName() {
        return resourceName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((realmName == null) ? 0 : realmName.hashCode());
        result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CacheKey other = (CacheKey) obj;
        if (realmName == null) {
            if (other.realmName != null)
                return false;
        } else if (!realmName.equals(other.realmName))
            return false;
        if (resourceName == null) {
            if (other.resourceName != null)
                return false;
        } else if (!resourceName.equals(other.resourceName))
            return false;
        return true;
    }

//        @Override
//        public String toString(){
//            return realmName + CACHE_KEY_DELIMITER + resourceName;
//        }

}
