package com.example.Gatling;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

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

            CustomResponse customResponse = new CustomResponse(statusCode, responseBody);
            System.out.println("Custom Response for " + requestName + ": " + customResponse);

            return session;
        });
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
                users.injectOpen(rampUsers(10).during(10))
        ).protocols(httpProtocol)
                .maxDuration(30);
    }
}
