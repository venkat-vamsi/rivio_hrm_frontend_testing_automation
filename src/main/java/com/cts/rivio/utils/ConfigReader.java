package com.cts.rivio.utils;

import com.cts.rivio.constants.AppConstants;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfigReader – loads key=value pairs from config.properties once,
 * then serves them through a static getter so any class can use them
 * without re-reading the file.
 */
public class ConfigReader {

    private static final Properties props = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream(AppConstants.CONFIG_PATH)) {
            props.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load config.properties from: "
                    + AppConstants.CONFIG_PATH, e);
        }
    }

    private ConfigReader() {}

    /** Returns the value for a given key, or null if the key does not exist. */
    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    /** Returns the value for a given key, or a default value if not found. */
    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}
