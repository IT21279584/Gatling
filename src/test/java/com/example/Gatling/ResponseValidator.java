package com.example.Gatling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gatling.javaapi.core.ChainBuilder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static io.gatling.javaapi.core.CoreDsl.exec;

public class ResponseValidator {
    public ChainBuilder logResponseAndValidate(String requestName, EndpointConfig endpoint) {
        return exec(session -> {
            String responseBody = session.get("postData");
            String expectedResponse = endpoint.getExpectedResponse();
            String response = session.get("responseBody");

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

                // Check if the response contains a token
                boolean containsToken = response.toString().contains("token");

                if (containsToken) {
                    // Log that the response contains a token
                    System.out.println(requestName + " - Response contains token as expected.");
                } else {
                    // Perform full validation
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


    public ChainBuilder logDeleteResponseAndValidate(String requestName, EndpointConfig endpoint) {
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
}
