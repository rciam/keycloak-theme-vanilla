package org.eosc.kc.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eosc.kc.rest.ThemeResourceProvider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This class loads additional configuration for different projects (i.e. different icons, different header and footer captions, etc.)
 */
public class ThemeConfig {

    private static final Logger logger = Logger.getLogger(ThemeConfig.class);

    private Map<String, String> config;

    public ThemeConfig() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("configuration.json");
        try {
            config = new ObjectMapper().readValue(is, new TypeReference<HashMap<String, String>>() {});
        }
        catch(IOException ex){
            config = new HashMap<>();
            logger.error("Could not fetch the keycloak-theme-vanilla specific config. Expect some problems if this theme is used", ex);
        }
    }

    public String get(String configParam){
        return config.get(configParam);
    }

    public Map<String, String> getConfig() {
        return config;
    }

}

