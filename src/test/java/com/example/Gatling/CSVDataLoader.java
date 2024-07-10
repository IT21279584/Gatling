package com.example.Gatling;

import com.opencsv.bean.CsvToBeanBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class CSVDataLoader {

    public List<EndpointConfig> loadEndpointConfigs(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            return new CsvToBeanBuilder<EndpointConfig>(reader)
                    .withType(EndpointConfig.class)
                    .build()
                    .parse();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load CSV data", e);
        }
    }
}
