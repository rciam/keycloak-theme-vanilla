package io.github.rciam.keycloak.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class loads additional configuration for different projects (i.e. different icons, different header and footer captions, etc.)
 */
public class ThemeConfig {

    private static final Logger logger = Logger.getLogger(ThemeConfig.class);

    private Map<String, List<String>> config;

    public ThemeConfig() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("configuration.json");
        try {
            config = new ObjectMapper().readValue(is, new TypeReference<HashMap<String, List<String>>>() {});
        }
        catch(IOException ex){
            config = new HashMap<>();
            logger.error("Could not fetch the keycloak-theme-vanilla specific config. Expect some problems if this theme is used", ex);
        }
    }

    public List<String> get(String configParam){
        return config.get(configParam);
    }

    public Map<String, List<String>> getConfig() {
        return config;
    }

}

