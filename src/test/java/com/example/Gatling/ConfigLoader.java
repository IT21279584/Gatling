package com.example.Gatling;


import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {

    private static final String CONFIG_FILE_PATH = "src/test/resources/application.csv";
    private final List<EndpointConfig> endpointConfigs;

    public ConfigLoader() {
        endpointConfigs = new ArrayList<>();
        loadConfigurations();
    }

    private void loadConfigurations() {
        try (BufferedReader br = new BufferedReader(new FileReader(Paths.get(CONFIG_FILE_PATH).toFile()))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header line
                }
                String[] parts = line.split(",", 4); // Allow body to contain commas
                if (parts.length >= 3) {
                    String url = parts[0];
                    String method = parts[1];
                    int status = Integer.parseInt(parts[2]);
                    String body = parts.length == 4 ? parts[3] : "";
                    endpointConfigs.add(new EndpointConfig(url, method, status, body));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<EndpointConfig> getEndpointConfigs() {
        return endpointConfigs;
    }
}
