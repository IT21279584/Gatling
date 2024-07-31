package com.example.Gatling;

import io.gatling.javaapi.core.Session;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GlobalVariableUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Session extractAndSetProjectId(Session session, String responseBody) {
        try {
            JsonNode responseJson = mapper.readTree(responseBody);
            if (responseJson.has("projectID")) {
                int projectId = responseJson.get("projectID").asInt();
                session = session.set("projectId", projectId);
//                System.out.println("Extracted projectId: " + projectId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.markAsFailed();
        }
        return session;
    }

    public static Session updateSessionWithId(Session session, EndpointConfig endpoint) {
        String url = endpoint.getUrl();
        if (url.contains("{Id}")) {
            session = GlobalVariableUtil.extractAndSetProjectId(session, session.getString("responseBody"));
            url = url.replace("{Id}", String.valueOf(session.getInt("projectId")));
        }
        session = session.set("sessionUrl", url);
        System.out.println("Request URL: " + url + " Method: " + endpoint.getMethod()); // Log the URL
        return session;
    }
}
