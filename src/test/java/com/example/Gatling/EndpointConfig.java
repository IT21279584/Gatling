package com.example.Gatling;

public class EndpointConfig {
    private final String url;
    private final String method;
    private final int expectedStatus;
    private final String body;

    private String expectedResponse;

    public EndpointConfig(String url, String method, int expectedStatus, String body) {
        this.url = url;
        this.method = method;
        this.expectedStatus = expectedStatus;
        this.body = body;
    }

    public EndpointConfig(String url, String method, int expectedStatus, String body, String expectedResponse) {
        this.url = url;
        this.method = method;
        this.expectedStatus = expectedStatus;
        this.body = body;
        this.expectedResponse = expectedResponse;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public int getExpectedStatus() {
        return expectedStatus;
    }

    public String getBody() {
        return body;
    }

    public String getExpectedResponse() {
        return expectedResponse;
    }

    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }
}
