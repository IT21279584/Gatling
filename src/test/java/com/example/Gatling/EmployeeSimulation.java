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

    private ChainBuilder executeGetRequests() {
        ChainBuilder chain = exec(flushHttpCache()).exec(flushSessionCookies());
        for (EndpointConfig endpoint : endpointConfigs) {
            if (endpoint.getMethod().equalsIgnoreCase("GET")) {
                chain = chain.exec(
                        http("Request - GET " + endpoint.getUrl())
                                .get(endpoint.getUrl())
                                .check(status().is(endpoint.getExpectedStatus()))
                ).pause(1);
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
                ).pause(2);
            }
        }
        return chain;
    }

    private ChainBuilder executePutRequests() {
        ChainBuilder chain = exec(flushHttpCache()).exec(flushSessionCookies());
        for (EndpointConfig endpoint : endpointConfigs) {
            if (endpoint.getMethod().equalsIgnoreCase("PUT")) {
                chain = chain.exec(
                        http("Request - PUT " + endpoint.getUrl().replace("${newEmployeeId}", "#{newEmployeeId}"))
                                .put(endpoint.getUrl().replace("${newEmployeeId}", "#{newEmployeeId}"))
                                .body(StringBody(endpoint.getBody())).asJson()
                                .check(status().is(endpoint.getExpectedStatus()))
                ).pause(1);
            }
        }
        return chain;
    }

    private ChainBuilder executeDeleteRequests() {
        ChainBuilder chain = exec(flushHttpCache()).exec(flushSessionCookies());
        for (EndpointConfig endpoint : endpointConfigs) {
            if (endpoint.getMethod().equalsIgnoreCase("DELETE")) {
                chain = chain.exec(
                        http("Request - DELETE " + endpoint.getUrl().replace("${newEmployeeId}", "#{newEmployeeId}"))
                                .delete(endpoint.getUrl().replace("${newEmployeeId}", "#{newEmployeeId}"))
                                .check(status().is(endpoint.getExpectedStatus()))
                ).pause(1);
            }
        }
        return chain;
    }

    ScenarioBuilder users = scenario("Users")
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