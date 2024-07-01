package com.example.Gatling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class TestSimulation extends Simulation {

    private final HttpProtocolBuilder httpProtocol = http
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private final ConfigLoader configLoader = new ConfigLoader();
    private final List<EndpointConfig> endpointConfigs = configLoader.getEndpointConfigs();

    private ChainBuilder logResponse(String requestName) {
        return exec(session -> {
            String responseBody = session.getString("responseBody");
            int statusCode = session.getInt("statusCode");

            CustomResponse customResponse = new CustomResponse(statusCode, removeFields(responseBody));
            System.out.println("Response for " + requestName + ": " + customResponse);

            return session;
        });
    }

    private String removeFields(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);

            // Remove fields that are not needed in the response
            ((ObjectNode) jsonNode).remove("createdAt");
            ((ObjectNode) jsonNode).remove("updatedAt");
            ((ObjectNode) jsonNode).remove("id");


            return mapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            e.printStackTrace();
            return responseBody; // Return original body in case of exception
        }
    }

    private ChainBuilder executeRequests(String method) {
        ChainBuilder chain = exec(flushHttpCache(), flushSessionCookies());

        for (EndpointConfig endpoint : endpointConfigs) {
            if (endpoint.getMethod().equalsIgnoreCase(method)) {
                HttpRequestActionBuilder request = null;

                switch (method.toUpperCase()) {
                    case "GET":
                        request = http("Request - " + method + " " + endpoint.getUrl())
                                .get(session -> session.getString("sessionUrl"));
                        break;
                    case "POST":
                        request = http("Request - " + method + " " + endpoint.getUrl())
                                .post(endpoint.getUrl())
                                .body(StringBody(endpoint.getBody())).asJson();
                        break;
                    case "PUT":
                        request = http("Request - " + method + " " + endpoint.getUrl())
                                .put(session -> session.getString("sessionUrl"))
                                .body(StringBody(endpoint.getBody())).asJson();
                        break;
                    case "PATCH":
                        request = http("Request - " + method + " " + endpoint.getUrl())
                                .patch(session -> session.getString("sessionUrl"))
                                .body(StringBody(endpoint.getBody())).asJson();
                        break;
                    case "DELETE":
                        request = http("Request - " + method + " " + endpoint.getUrl())
                                .delete(session -> session.getString("sessionUrl"));
                        break;
                }

                chain = chain.exec(session -> {
                    if ("POST".equalsIgnoreCase(method)) {
                        // Generate random ID below 100 for POST requests
                        Random random = new Random();
                        int newId = random.nextInt(10) + 1;
                        session = session.set("newId", newId);
                    }
                    return session;
                }).exec(session -> {
                    String url = endpoint.getUrl();
                    if (url.contains("${newId}")) {
                        int newId = session.getInt("newId");
                        url = url.replace("${newId}", String.valueOf(newId));
                    }
                    session = session.set("sessionUrl", url);
                    System.out.println("Request URL: " + url + " Method " + method); // Log the URL
                    return session;
                }).exec(
                        request
                                .check(status().is(endpoint.getExpectedStatus()))
                                .check(bodyString().saveAs("responseBody"))
                                .check(status().saveAs("statusCode"))
                ).exec(session -> {
                    String responseBody = session.getString("responseBody");
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode responseJson = null;
                    try {
                        responseJson = mapper.readTree(responseBody);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    if ("POST".equalsIgnoreCase(method)) {
                        // Store POST response data in session, including newId
                        responseJson = ((ObjectNode) responseJson).put("id", session.getInt("newId"));
                        try {
                            session = session.set("postData", mapper.writeValueAsString(responseJson));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    } else if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                        // Update specific field in the session data for PUT/PATCH request
                        String postData = session.getString("postData");
                        responseJson = ((ObjectNode) responseJson).put("id", session.getInt("newId"));
                        session = session.set("updatedData", updateSessionData(postData, responseJson.toString()));
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        // Remove data from session for DELETE request
                        session = session.remove("postData").remove("updatedData");
                    }
                    return session;
                }).pause(1).exec(logResponse(method.toUpperCase() + " " + endpoint.getUrl()));
            }
        }
        return chain;
    }

    private String updateSessionData(String originalData, String newData) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode originalJson = mapper.readTree(originalData);
            JsonNode newJson = mapper.readTree(newData);

            // Update original JSON with new values from new JSON
            ((ObjectNode) originalJson).setAll((ObjectNode) newJson);

            return mapper.writeValueAsString(originalJson);
        } catch (IOException e) {
            e.printStackTrace();
            return originalData; // Return original data in case of exception
        }
    }

    private final ScenarioBuilder users = scenario("Users")
            .exec(flushHttpCache(), flushSessionCookies())
            .exec(executeRequests("POST"))
            .exec(executeRequests("GET"))
            .exec(executeRequests("PUT"))
            .exec(executeRequests("PATCH"))
            .exec(executeRequests("DELETE"));

    {
        setUp(
                users.injectOpen(rampUsers(1).during(10))
        ).protocols(httpProtocol);
    }
}
