package com.example.Gatling;

import com.opencsv.bean.CsvBindByName;

public class EndpointConfig {

    @CsvBindByName(column = "url")
    private String url;

    @CsvBindByName(column = "method")
    private String method;

    @CsvBindByName(column = "status")
    private int expectedStatus;

    @CsvBindByName(column = "body")
    private String body;

    @CsvBindByName(column = "expectedResponse")
    private String expectedResponse;

    // No-argument constructor required by OpenCSV
    public EndpointConfig() {
    }

    // Constructor with parameters for other use cases
    public EndpointConfig(String url, String method, int expectedStatus, String body, String expectedResponse) {
        this.url = url;
        this.method = method;
        this.expectedStatus = expectedStatus;
        this.body = body;
        this.expectedResponse = expectedResponse;
    }

    // Getters and setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(int expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getExpectedResponse() {
        return expectedResponse;
    }

    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }
}

