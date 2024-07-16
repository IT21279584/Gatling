package com.example.Gatling;

import com.opencsv.bean.CsvToBeanBuilder;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class ConfigLoader {

    private static final String CSV_FILE_PATH = "/home/hsudusinghe/Downloads/Gatling/src/test/resources/application.csv";
    private List<EndpointConfig> endpointConfigs;

    public ConfigLoader() {
        loadConfig();
    }

    private void loadConfig() {
        try (FileReader reader = new FileReader(CSV_FILE_PATH)) {
            endpointConfigs = new CsvToBeanBuilder<EndpointConfig>(reader)
                    .withType(EndpointConfig.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();

            // Optional: Log loaded configurations
            for (EndpointConfig config : endpointConfigs) {
                System.out.println("Loaded EndpointConfig: " + config);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading CSV file: " + CSV_FILE_PATH, e);
        }
    }

    public List<EndpointConfig> getEndpointConfigs() {
        return endpointConfigs;
    }
}
