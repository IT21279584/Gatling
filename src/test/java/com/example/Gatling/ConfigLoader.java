package com.example.Gatling;

import com.opencsv.bean.CsvToBean;
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
            CsvToBean<EndpointConfig> csvToBean = new CsvToBeanBuilder<EndpointConfig>(reader)
                    .withType(EndpointConfig.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            endpointConfigs = csvToBean.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<EndpointConfig> getEndpointConfigs() {
        return endpointConfigs;
    }
}
