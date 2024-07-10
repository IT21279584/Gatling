package com.example.Gatling;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
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

    private ChainBuilder logResponseAndValidate(String requestName, EndpointConfig endpoint) {
        return exec(session -> {
            String responseBody = session.get("postData");
            String removeResponse = session.get("removePostData");
            String expectedResponse = endpoint.getExpectedResponse();

            ObjectMapper mapper = new ObjectMapper();
            boolean validationFailed = false;
            try {
                // Parse expected response JSON
                JsonNode expectedJson = mapper.readTree(expectedResponse);

                // Parse actual response JSON
                JsonNode actualJson = mapper.readTree(responseBody);

                // Remove fields if they exist
                if (actualJson.isObject()) {
                    ((ObjectNode) actualJson).remove("createdAt");
                    ((ObjectNode) actualJson).remove("updatedAt");
                    ((ObjectNode) actualJson).remove("id");
                }

                // Compare JSON objects
                try {
                    JSONAssert.assertEquals(expectedJson.toString(), actualJson.toString(), JSONCompareMode.LENIENT);
                    System.out.println(requestName + " - Response matches expected.");
                } catch (AssertionError e) {
                    System.out.println(requestName + " - Response does not match expected.");
                    System.out.println("Expected: " + expectedResponse);
                    System.out.println("Actual: " + responseBody);
                    validationFailed = true;

                    if (validationFailed) {
                        return session.markAsFailed(); // Mark the session as failed
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                validationFailed = true;
            }

            if (validationFailed) {
                return session.markAsFailed(); // Mark the session as failed
            }

            return session;
        });
    }

    private ChainBuilder logDeleteResponseAndValidate(String requestName, EndpointConfig endpoint) {
        return exec(session -> {
            String removeResponse = session.getString("removePostData");
            String expectedResponse = endpoint.getExpectedResponse();

            boolean validationFailed = false;
            try {
                if (removeResponse == null || removeResponse.isEmpty()) {
                    System.out.println(requestName + " - Response matches expected.");
                } else {
                    System.out.println(requestName + " - Response is not empty.");
                    System.out.println("Expected: <empty>");
                    System.out.println("Actual: " + removeResponse);
                    validationFailed = true;
                }

                if (validationFailed) {
                    return session.markAsFailed(); // Mark the session as failed
                }

            } catch (Exception e) {
                e.printStackTrace();
                validationFailed = true;
            }

            if (validationFailed) {
                return session.markAsFailed(); // Mark the session as failed
            }

            return session;
        });
    }


    private ChainBuilder executeRequests(String method) {
        ChainBuilder chain = exec(flushHttpCache(), flushSessionCookies());

        if (endpointConfigs != null) {
            for (EndpointConfig endpoint : endpointConfigs) {
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
                                    .exec(session -> {
                                        String url = endpoint.getUrl();
                                        if (url.contains("${newId}")) {
                                            int newId = session.getInt("newId");
                                            url = url.replace("${newId}", String.valueOf(newId));
                                        }
                                        session = session.set("sessionUrl", url);
                                        System.out.println("Request URL: " + url + " Method: " + method); // Log the URL
                                        return session;
                                    })
                                    .exec(
                                            request
                                                    .check(status().is(endpoint.getExpectedStatus()))
                                                    .check(bodyString().saveAs("responseBody"))
                                                    .check(status().saveAs("statusCode"))
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
                                        return session;
                                    })
                                    .exec(logResponseAndValidate("Request - " + method + " " + endpoint.getUrl(), endpoint));
                            break;
                        case "GET":
                            request = http("Request - " + method + " " + endpoint.getUrl())
                                    .get(session -> session.getString("sessionUrl"))
                                    .body(StringBody(endpoint.getBody())).asJson();
                            chain = chain
                                    .exec(session -> {
                                        String url = endpoint.getUrl();
                                        if (url.contains("${newId}")) {
                                            int newId = session.getInt("newId");
                                            url = url.replace("${newId}", String.valueOf(newId));
                                        }
                                        session = session.set("sessionUrl", url);
                                        System.out.println("Request URL: " + url + " Method: " + method); // Log the URL
                                        return session;
                                    })
                                    .exec(
                                            request
                                                    .check(status().is(endpoint.getExpectedStatus()))
                                                    .check(bodyString().saveAs("responseBody"))
                                                    .check(status().saveAs("statusCode"))
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
                                            if (postJson.has("id")) {
                                                int id = postJson.get("id").asInt();
                                                session = session.set("sessionUrl", endpoint.getUrl().replace("${newId}", String.valueOf(id)));
                                            }
                                        }
                                        return session;
                                    })
                                    .exec(logResponseAndValidate("Request - " + method + " " + endpoint.getUrl(), endpoint));
                            break;
                        case "PUT":
                            request = http("Request - " + method + " " + endpoint.getUrl())
                                    .put(session -> session.getString("sessionUrl"))
                                    .body(StringBody(endpoint.getBody())).asJson();
                            chain = chain
                                    .exec(session -> {
                                        String url = endpoint.getUrl();
                                        if (url.contains("${newId}")) {
                                            int newId = session.getInt("newId");
                                            url = url.replace("${newId}", String.valueOf(newId));
                                        }
                                        session = session.set("sessionUrl", url);
                                        System.out.println("Request URL: " + url + " Method: " + method); // Log the URL
                                        return session;
                                    })
                                    .exec(
                                            request
                                                    .check(status().is(endpoint.getExpectedStatus()))
                                                    .check(bodyString().saveAs("responseBody"))
                                                    .check(status().saveAs("statusCode"))
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
                                    .exec(logResponseAndValidate("Request - " + method + " " + endpoint.getUrl(), endpoint));
                            break;
                        case "PATCH":
                            request = http("Request - " + method + " " + endpoint.getUrl())
                                    .patch(session -> session.getString("sessionUrl"))
                                    .body(StringBody(endpoint.getBody())).asJson();
                            chain = chain
                                    .exec(session -> {
                                        String url = endpoint.getUrl();
                                        if (url.contains("${newId}")) {
                                            int newId = session.getInt("newId");
                                            url = url.replace("${newId}", String.valueOf(newId));
                                        }
                                        session = session.set("sessionUrl", url);
                                        System.out.println("Request URL: " + url + " Method: " + method); // Log the URL
                                        return session;
                                    })
                                    .exec(
                                            request
                                                    .check(status().is(endpoint.getExpectedStatus()))
                                                    .check(bodyString().saveAs("responseBody"))
                                                    .check(status().saveAs("statusCode"))
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
                                    .exec(logResponseAndValidate("Request - " + method + " " + endpoint.getUrl(), endpoint));
                            break;
                        case "DELETE":
                            request = http("Request - " + method + " " + endpoint.getUrl())
                                    .delete(session -> session.getString("sessionUrl"));
                            chain = chain
                                    .exec(session -> {
                                        String url = endpoint.getUrl();
                                        if (url.contains("${newId}")) {
                                            int newId = session.getInt("newId");
                                            url = url.replace("${newId}", String.valueOf(newId));
                                        }
                                        session = session.set("sessionUrl", url);
                                        System.out.println("Request URL: " + url + " Method: " + method); // Log the URL
                                        return session;
                                    })
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
                                    .exec(logDeleteResponseAndValidate("Request - " + method + " " + endpoint.getUrl(), endpoint));
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
                .exec(executeRequests("POST"))
                .exec(executeRequests("GET"))
                .exec(executeRequests("PUT"))
                .exec(executeRequests("PATCH"))
                .exec(executeRequests("DELETE"));

        setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
    }
}
