package com.example.Gatling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.io.IOException;
import java.util.List;

public class EmployeeSimulation extends Simulation {

    ConfigLoader config = new ConfigLoader();
    List<EndpointConfig> endpointConfigs = config.getEndpointConfigs();

    HttpProtocolBuilder httpProtocol = http
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private ChainBuilder logResponse(String requestName) {
        return exec(session -> {
            String responseBody = session.getString("responseBody");
            int statusCode = session.getInt("statusCode");

            // Construct CustomResponse excluding createdAt
            CustomResponse customResponse = new CustomResponse(statusCode, removeCreatedAt(responseBody));
            System.out.println("Custom Response for " + requestName + ": " + customResponse);

            return session;
        });
    }

    // Helper method to remove createdAt field from JSON body
    private String removeCreatedAt(String responseBody) {
        try {
            // Parse JSON and remove createdAt
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);
            ((ObjectNode) jsonNode).remove("createdAt");
            ((ObjectNode) jsonNode).remove("updatedAt");
            return mapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            e.printStackTrace();
            return responseBody; // Return original body if parsing fails
        }
    }


    private ChainBuilder executeGetRequests() {
        ChainBuilder chain = exec(flushHttpCache()).exec(flushSessionCookies());
        for (EndpointConfig endpoint : endpointConfigs) {
            if (endpoint.getMethod().equalsIgnoreCase("GET")) {
                chain = chain.exec(
                                http("Request - GET " + endpoint.getUrl())
                                        .get(endpoint.getUrl())
                                        .check(status().is(endpoint.getExpectedStatus()))
                                        .check(bodyString().saveAs("responseBody"))
                                        .check(status().saveAs("statusCode"))
                        ).pause(1)
                        .exec(logResponse("GET " + endpoint.getUrl()));
            }
        }
        return chain;
    }

    private ChainBuilder executePostRequests() {
        ChainBuilder chain = exec(flushHttpCache()).exec(flushSessionCookies());
        for (EndpointConfig endpoint : endpointConfigs) {
            if (endpoint.getMethod().equalsIgnoreCase("POST")) {
                chain = chain.exec(
                                http("Request - POST " + endpoint.getUrl())
                                        .post(endpoint.getUrl())
                                        .body(StringBody(endpoint.getBody())).asJson()
                                        .check(status().is(endpoint.getExpectedStatus()))
                                        .check(jsonPath("$.id").saveAs("newEmployeeId"))
                                        .check(bodyString().saveAs("responseBody"))
                                        .check(status().saveAs("statusCode"))
                        ).pause(2)
                        .exec(logResponse("POST " + endpoint.getUrl()));
            }
        }
        return chain;
    }

    private ChainBuilder executePutRequests() {
        ChainBuilder chain = exec(flushHttpCache()).exec(flushSessionCookies());
        for (EndpointConfig endpoint : endpointConfigs) {
            if (endpoint.getMethod().equalsIgnoreCase("PUT")) {
                chain = chain.exec(
                                http("Request - PUT " + endpoint.getUrl())
                                        .put(session -> endpoint.getUrl().replace("${newEmployeeId}", session.getString("newEmployeeId")))
                                        .body(StringBody(endpoint.getBody())).asJson()
                                        .check(status().is(endpoint.getExpectedStatus()))
                                        .check(bodyString().saveAs("responseBody"))
                                        .check(status().saveAs("statusCode"))
                        ).pause(1)
                        .exec(logResponse("PUT " + endpoint.getUrl()));
            }
        }
        return chain;
    }

    private ChainBuilder executeDeleteRequests() {
        ChainBuilder chain = exec(flushHttpCache()).exec(flushSessionCookies());
        for (EndpointConfig endpoint : endpointConfigs) {
            if (endpoint.getMethod().equalsIgnoreCase("DELETE")) {
                chain = chain.exec(
                                http("Request - DELETE " + endpoint.getUrl())
                                        .delete(session -> endpoint.getUrl().replace("${newEmployeeId}", session.getString("newEmployeeId")))
                                        .check(status().is(endpoint.getExpectedStatus()))
                                        .check(bodyString().saveAs("responseBody"))
                                        .check(status().saveAs("statusCode"))
                        ).pause(1)
                        .exec(logResponse("DELETE " + endpoint.getUrl()));
            }
        }
        return chain;
    }

    ScenarioBuilder users = scenario("Users")
            .exec(flushHttpCache()).exec(flushSessionCookies()) // Only flush once at the beginning
            .exec(executePostRequests())
            .exec(executePutRequests())
            .exec(executeDeleteRequests())
            .exec(executeGetRequests());

    {
        setUp(
                users.injectOpen(rampUsers(1).during(10))
        ).protocols(httpProtocol)
                .maxDuration(30);
    }
}