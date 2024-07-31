package com.example.Gatling;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private final ResponseValidator responseValidator = new ResponseValidator();

    private HttpRequestActionBuilder Checks(HttpRequestActionBuilder request, EndpointConfig endpoint) {
        return request
                .check(status().is(endpoint.getExpectedStatus()))
                .check(bodyString().saveAs("responseBody"))
                .check(status().saveAs("statusCode"));
    }

    private ChainBuilder executeRequests() {
        ChainBuilder chain = exec(flushHttpCache(), flushSessionCookies());

        if (endpointConfigs != null) {
            for (EndpointConfig endpoint : endpointConfigs) {
                String method = endpoint.getMethod().toUpperCase();
                if (endpoint.getMethod().equalsIgnoreCase(method)) {
                    HttpRequestActionBuilder request = null;

                    switch (method.toUpperCase()) {
                        case "POST":
                            request = http("Request - " + method + " " + endpoint.getUrl())
                                    .post(endpoint.getUrl())
                                    .body(StringBody(endpoint.getBody())).asJson();
                            chain = chain
                                    .exec(session -> {
                                        // Generate random ID between 1 and 10 for POST requests
                                        Random random = new Random();
                                        int newId = random.nextInt(10) + 1;
                                        session = session.set("newId", newId);
                                        return session;
                                    })
                                    .exec(session -> GlobalVariableUtil.updateSessionWithId(session, endpoint))
                                    .exec(Checks(request, endpoint)
                                    )
                                    .exec(session -> {
                                        String responseBody = session.getString("responseBody");
                                        ObjectMapper mapper = new ObjectMapper();
                                        JsonNode responseJson = null;
                                        try {
                                            responseJson = mapper.readTree(responseBody);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        // Store POST response data in session, including newId
                                        responseJson = ((ObjectNode) responseJson).put("id", session.getInt("newId"));
                                        try {
                                            session = session.set("postData", mapper.writeValueAsString(responseJson));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        if (responseJson.has("token")) {
                                            String token = responseJson.get("token").asText();
                                            session = session.set("authToken", token);

                                        }

                                        return session;
                                    })
                                    .exec(responseValidator.logResponseAndValidate("Request - " + method + " " + endpoint.getUrl(), endpoint))
                                    .pause(5);
                            break;
                        case "GET":
                            request = http("Request - " + method + " " + endpoint.getUrl())
                                    .get(session -> session.getString("sessionUrl"))
                                    .header("Authorization", session -> "Bearer " + session.getString("authToken"))
                                    .body(StringBody(endpoint.getBody())).asJson();
                            chain = chain
                                    .exec(session -> GlobalVariableUtil.updateSessionWithId(session, endpoint))
                                    .exec(Checks(request, endpoint)
                                    )
                                    .exec(session -> {
                                        // Set the session URL to include the ID for GET requests
                                        String postData = session.getString("postData");
                                        if (postData != null && postData.contains("id")) {
                                            JsonNode postJson = null;
                                            try {
                                                postJson = new ObjectMapper().readTree(postData);
                                            } catch (JsonProcessingException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                        return session;
                                    })
                                    .exec(responseValidator.logResponseAndValidate("Request - " + method, endpoint))
                                    .pause(5);
                            break;
                        case "PUT":
                            request = http("Request - " + method + " " + endpoint.getUrl())
                                    .put(session -> session.getString("sessionUrl"))
                                    .header("Authorization", session -> "Bearer " + session.getString("authToken"))
                                    .body(StringBody(endpoint.getBody())).asJson();
                            chain = chain
                                    .exec(session -> GlobalVariableUtil.updateSessionWithId(session, endpoint))
                                    .exec(Checks(request, endpoint)
                                    )
                                    .exec(session -> {
                                        // Update specific field in the session data for PUT request
                                        String postData = session.getString("postData");
                                        ObjectMapper mapper = new ObjectMapper();
                                        JsonNode responseJson = null;
                                        try {
                                            responseJson = mapper.readTree(session.getString("responseBody"));
                                            responseJson = ((ObjectNode) responseJson).put("id", session.getInt("newId"));
                                            session = session.set("postData", updateSessionData(postData, responseJson.toString()));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        return session;
                                    })
                                    .exec(responseValidator.logResponseAndValidate("Request - " + method, endpoint))
                                    .pause(5);
                            break;
                        case "PATCH":
                            request = http("Request - " + method + " " + endpoint.getUrl())
                                    .patch(session -> session.getString("sessionUrl"))
                                    .header("Authorization", session -> "Bearer " + session.getString("authToken"))
                                    .body(StringBody(endpoint.getBody())).asJson();
                            chain = chain
                                    .exec(session -> GlobalVariableUtil.updateSessionWithId(session, endpoint))
                                    .exec(Checks(request, endpoint)
                                    )
                                    .exec(session -> {
                                        // Update specific field in the session data for PATCH request
                                        String postData = session.getString("postData");
                                        ObjectMapper mapper = new ObjectMapper();
                                        JsonNode responseJson = null;
                                        try {
                                            responseJson = mapper.readTree(session.getString("responseBody"));
                                            responseJson = ((ObjectNode) responseJson).put("id", session.getInt("newId"));
                                            session = session.set("postData", updateSessionData(postData, responseJson.toString()));
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        return session;
                                    })
                                    .exec(responseValidator.logResponseAndValidate("Request - " + method, endpoint))
                                    .pause(5);
                            break;
                        case "DELETE":
                            request = http("Request - " + method + " " + endpoint.getUrl())
                                    .delete(session -> session.getString("sessionUrl")).header("Authorization", session -> "Bearer " + session.getString("authToken"))
                                    .header("Authorization", session -> "Bearer " + session.getString("authToken"));

                            chain = chain
                                    .exec(session -> GlobalVariableUtil.updateSessionWithId(session, endpoint))
                                    .exec(
                                            request
                                                    .check(status().is(endpoint.getExpectedStatus()))
                                                    .check(status().saveAs("statusCode"))
                                    )
                                    .exec(session -> {
                                        session = session.set("removePostData", session.getString("sessionUrl"));
                                        session = session.remove("removePostData");
                                        return session;
                                    })
                                    .exec(responseValidator.logDeleteResponseAndValidate("Request - " + method, endpoint))
                                    .pause(5);
                            break;
                    }
                }
            }
        }
        return chain;
    }

    private String updateSessionData(String originalData, String updateData) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode originalJson = mapper.readTree(originalData);
            JsonNode updateJson = mapper.readTree(updateData);

            if (originalJson.isObject() && updateJson.isObject()) {
                ((ObjectNode) originalJson).setAll((ObjectNode) updateJson);
            }
            return originalJson.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    {
        ScenarioBuilder scn = scenario("API Test Scenario")
                .exec(executeRequests());
        setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
    }
}