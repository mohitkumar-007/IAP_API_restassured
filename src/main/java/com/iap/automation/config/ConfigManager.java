package com.iap.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found in classpath");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String get(String key) {
        String envValue = System.getenv(key.replace(".", "_").toUpperCase());
        if (envValue != null) {
            return envValue;
        }
        String sysValue = System.getProperty(key);
        if (sysValue != null) {
            return sysValue;
        }
        return properties.getProperty(key);
    }

    public static String getBaseUrl() {
        return get("base.url");
    }

    public static String getAuthToken() {
        return get("auth.token");
    }

    public static String getTabBasePath() {
        return get("tab.base.path");
    }

    public static String getTabsFetchPath() {
        return get("tabs.fetch.path");
    }

    public static String getPassBasePath() {
        return get("pass.base.path");
    }

    public static String getProductId() {
        return get("product.id");
    }

    public static String getTabType() {
        return get("tab.type");
    }
}
