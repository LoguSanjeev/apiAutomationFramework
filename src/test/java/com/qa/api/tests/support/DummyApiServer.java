package com.qa.api.tests.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.api.commonUtils.TestData;
import com.qa.api.commonUtils.TestLogger;
import com.qa.api.pojo.UserRequest;
import com.qa.api.pojo.UserResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** Local sample API only. Real API tests use the client prepared by BaseTest. */
public class DummyApiServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    public void start(TestData testData) throws IOException {
        TestLogger.info("Starting local dummy API on an available loopback port");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            try (exchange) {
                if (!"/health".equals(exchange.getRequestURI().getPath())) {
                    sendJson(exchange, 404, "{\"message\":\"Not found\"}");
                } else if ("GET".equals(exchange.getRequestMethod())) {
                    sendJson(exchange, 200, "{\"status\":\"UP\"}");
                } else {
                    sendJson(exchange, 405, "{\"message\":\"Method not allowed\"}");
                }
            }
        });
        String name = testData.getString("createUser.name");
        String job = testData.getString("createUser.job");
        String id = testData.getString("existingUser.id");
        server.createContext("/users", exchange -> {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                if ("/users".equals(path) && "POST".equals(exchange.getRequestMethod())) {
                    UserRequest request = mapper.readValue(exchange.getRequestBody(), UserRequest.class);
                    sendJson(exchange, 201, mapper.writeValueAsString(user(id, request.getName(), request.getJob())));
                } else if (("/users/" + id).equals(path) && "GET".equals(exchange.getRequestMethod())) {
                    sendJson(exchange, 200, mapper.writeValueAsString(user(id, name, job)));
                } else {
                    sendJson(exchange, 404, "{\"message\":\"Not found\"}");
                }
            }
        });
        server.start();
        TestLogger.info("Local dummy API started: " + getBaseUrl());
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            TestLogger.info("Stopping local dummy API");
            server.stop(0);
            TestLogger.info("Local dummy API stopped");
        }
    }

    private UserResponse user(String id, String name, String job) {
        UserResponse user = new UserResponse();
        user.setId(id);
        user.setName(name);
        user.setJob(job);
        return user;
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
