package io.github.rciam.keycloak.resolver;

import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Commons {

    private static final Logger logger = Logger.getLogger(Commons.class);

    private static String basePath;
    public static final String THEME_WORKING_FOLDER = "standalone/theme-config";
    public static final String TERMS_OF_USE_FOLDER = "terms-of-use";
    public static final String CONFIGURATIONS_FOLDER = "configurations";
    public static final String RESOURCES_FOLDER = "resources";

    /**
     * Uses either "JBOSS_HOME" or "QUARKUS_HOME", and if none of them available, uses "HOME"
     */
    public static String getBasePath(){
        if(basePath != null)
            return basePath;
        basePath = System.getenv("JBOSS_HOME");
        if(basePath == null || basePath.isEmpty())
            basePath = System.getenv("QUARKUS_HOME");
        if(basePath == null || basePath.isEmpty())
            basePath = System.getenv("HOME");
        basePath = basePath.endsWith(File.separator) ? basePath.substring(0, basePath.length() - 1) : basePath;
        logger.info("keycloak-theme-vanilla will use the following base path for its config files: " + basePath);
        return basePath;
    }


    public static void writeFile(String filepath, String content) throws IOException {
        writeFile(filepath, content.getBytes());
    }

    public static String readFile(String filepath) throws IOException {
        return Files.lines(Paths.get(filepath)).collect(Collectors.joining(System.lineSeparator()));
    }

    public static String removeScriptTags(String htmlStr){
        StringBuilder regex = new StringBuilder("<script[^>]*>(.*?)</script>");
        int flags = Pattern.MULTILINE | Pattern.DOTALL| Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(regex.toString(), flags);
        Matcher matcher = pattern.matcher(htmlStr);
        return matcher.replaceAll("");
    }

    public static void writeFile(String filepath, byte[] content) throws IOException {
        Files.write(Paths.get(filepath), content);
    }

    public static byte[] readRawFile(String filepath) throws IOException {
        return new File(filepath).exists() ? Files.readAllBytes(Paths.get(filepath)) : null;
    }


    /**
     * USE WITH EXTREME CARE
     */
    public static boolean deleteFolderAndContents(File folder) {
        File[] allContents = folder.listFiles();
        if (allContents != null)
            for (File file : allContents)
                deleteFolderAndContents(file);
        return folder.delete();
    }

}
