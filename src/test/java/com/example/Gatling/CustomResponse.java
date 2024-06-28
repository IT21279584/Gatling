package com.example.Gatling;

public class CustomResponse {
    private int statusCode;
    private String body;

    public CustomResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;

    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "CustomResponse{" +
                "statusCode=" + statusCode +
                ", body='" + body + '\'' +

                '}';
    }
}
