package org.eosc.kc.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This class loads additional configuration for different projects (i.e. different icons, different header and footer captions, etc.)
 */
public class ThemeConfig {

    private Map<String, String> config;

    public ThemeConfig() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("configuration.json");
        config = new ObjectMapper().readValue(is, new TypeReference<HashMap<String, String>>() {});
    }

    public String get(String configParam){
        return config.get(configParam);
    }

    public Map<String, String> getConfig() {
        return config;
    }

}

