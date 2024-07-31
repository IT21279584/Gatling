package com.example.Gatling;

import com.opencsv.bean.CsvToBeanBuilder;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.yaml.snakeyaml.constructor.Constructor;

public class ConfigLoader {

    private static final String CONFIG_FILE_PATH = "application.yaml";
    private List<EndpointConfig> endpointConfigs;

    public ConfigLoader() {
        loadConfig();
    }

//    private void loadConfig() {
//        try (FileReader reader = new FileReader(CSV_FILE_PATH)) {
//            endpointConfigs = new CsvToBeanBuilder<EndpointConfig>(reader)
//                    .withType(EndpointConfig.class)
//                    .withIgnoreLeadingWhiteSpace(true)
//                    .build()
//                    .parse();
//
//            // Optional: Log loaded configurations
//            for (EndpointConfig config : endpointConfigs) {
//                System.out.println("Loaded EndpointConfig: " + config);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new RuntimeException("Error loading CSV file: " + CSV_FILE_PATH, e);
//        }
//    }

//    private void loadConfig() {
//        // Create LoaderOptions for the constructor
//        LoaderOptions loaderOptions = new LoaderOptions();
//        loaderOptions.setAllowDuplicateKeys(false); // Optionally set other loader options
//
//        // Create Constructor with the correct type and LoaderOptions
//        Constructor constructor = new Constructor(EndpointsWrapper.class, loaderOptions);
//        Yaml yaml = new Yaml(constructor);
//
//        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(YAML_FILE_PATH)) {
//            if (inputStream == null) {
//                throw new RuntimeException("YAML file not found: " + YAML_FILE_PATH);
//            }
//            EndpointsWrapper wrapper = yaml.load(inputStream);
//            endpointConfigs = wrapper.getEndpoints();
//
//            // Optional: Log loaded configurations
//            for (EndpointConfig config : endpointConfigs) {
//                System.out.println("Loaded EndpointConfig: " + config);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Error loading YAML file: " + YAML_FILE_PATH, e);
//        }
//    }

    private void loadConfig() {
        if (CONFIG_FILE_PATH.endsWith(".yaml") || CONFIG_FILE_PATH.endsWith(".yml")) {
            loadYamlConfig();
        } else if (CONFIG_FILE_PATH.endsWith(".csv")) {
            loadCsvConfig();
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + CONFIG_FILE_PATH);
        }
    }

    private void loadYamlConfig() {
        // Create LoaderOptions for the constructor
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false); // Optionally set other loader options

        // Create Constructor with the correct type and LoaderOptions
        Constructor constructor = new Constructor(EndpointsWrapper.class, loaderOptions);
        Yaml yaml = new Yaml(constructor);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH)) {
            if (inputStream == null) {
                throw new RuntimeException("YAML file not found: " + CONFIG_FILE_PATH);
            }
            EndpointsWrapper wrapper = yaml.load(inputStream);
            endpointConfigs = wrapper.getEndpoints();

            // Optional: Log loaded configurations
            for (EndpointConfig config : endpointConfigs) {
                System.out.println("Loaded EndpointConfig: " + config);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading YAML file: " + CONFIG_FILE_PATH, e);
        }
    }

    private void loadCsvConfig() {
        try (FileReader reader = new FileReader(CONFIG_FILE_PATH)) {
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
            throw new RuntimeException("Error loading CSV file: " + CONFIG_FILE_PATH, e);
        }
    }

    public List<EndpointConfig> getEndpointConfigs() {
        return endpointConfigs;
    }

    public static class EndpointsWrapper {
        private List<EndpointConfig> endpoints;

        public List<EndpointConfig> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(List<EndpointConfig> endpoints) {
            this.endpoints = endpoints;
        }
    }
}
