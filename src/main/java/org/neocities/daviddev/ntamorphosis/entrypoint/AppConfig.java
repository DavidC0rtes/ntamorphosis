package org.neocities.daviddev.ntamorphosis.entrypoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();
    private final Properties properties = new Properties();

    private AppConfig() {
        // Load properties from app.properties file
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("app.properties")) {
            properties.load(input);
        } catch (IOException e) {
            // Handle the exception (e.g., log it) or set default values.
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static AppConfig getInstance() { return INSTANCE; }
    public String getBisimPath() { return properties.getProperty("bisimulationpath"); }
    public String getVerifyTAPath() { return properties.getProperty("verifytapath"); }
}
