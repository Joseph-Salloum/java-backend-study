package com.joseph.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UsersHandler implements HttpHandler {
    private static List<User> users = new ArrayList<>();
    private static int nextId = 0;

    @Override
    public void handle(HttpExchange exchange) throws IOException{
        switch (exchange.getRequestMethod()) {
            case "GET":
                getHandler(exchange);
                break;
            case "POST":
                postHandler(exchange);
                break;
            default:
                defaultHandler(exchange);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void getHandler(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String response = "";
        int statusCode = 200;

        if (requestPath.equals("/users")) {
            StringBuilder sb = new StringBuilder();
            for (User user : users) {
                sb.append(user.toString());
                sb.append(", \n");
            }
            sb.deleteCharAt(sb.toString().lastIndexOf(','));

            response = String.format("""
                    [
                        %s
                    ]
                    """, sb.toString());
            sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            return;
        } else if (requestPath.matches("/users/[0-9a-zA-Z]+")) {
            try {
                int userId = Integer.parseInt(requestPath.split("/")[2]);
                User foundUser = null;
                for (User user : users) {
                    if (user.id == userId) {
                        foundUser = user;
                        break;
                    }
                }

                if (foundUser == null) {
                    response = String.format("""
                            {
                                "message": "No user with the provided id: %d"
                            }
                            """, userId);
                    statusCode = 404;
                    sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
                    return;
                }

                response = foundUser.toString();
                sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            } catch (NumberFormatException e) {
                response = String.format("""
                        {
                            "message": "%s is invalid, id must be an Integer"
                        }
                        """, requestPath.split("/")[2]);
                statusCode = 400;
                sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            }
        } else {
            response = """
                    {
                        "message": "Invalid request path"
                    }
                    """;
            statusCode = 400;
            sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
        }
    }
    @SuppressWarnings("unchecked")
    private void postHandler(HttpExchange exchange) throws IOException {
        String response = "";
        int statusCode = 201;

        if (!exchange.getRequestURI().getPath().equals("/users")) {
            response = """
                    {
                        "message": "No such endpoint"
                    }
                    """;
            statusCode = 404;
            sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            return;
        }

        InputStream is = exchange.getRequestBody();
        String requestbody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();

        if (requestbody.isBlank()) {
            response = """
                    {
                        "message": "Request body cannot be empty"
                    }
                    """;
            statusCode = 400;
            sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            return;
        }

        String userName = extractUserName(requestbody);

        if (userName == null) {
            response = """
                    {
                        "message": "Username is not provided or is empty"
                    }
                    """;
            statusCode = 400;
            sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            return;
        }

        User newUser = new User(nextId++, userName);
        users.add(newUser);

        response = String.format("""
                {
                    "user": {"id": %d, "name": "%s"}
                }
                """, newUser.id, newUser.name);

        sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
    }
    @SuppressWarnings("unchecked")
    private void defaultHandler(HttpExchange exchange) throws IOException {
        String response = """
                {
                    "message": "This HTTP method is not supported"
                }
                """;
        int statusCode = 405;
        sendResponse(exchange, response, statusCode,
            new Pair<>("Content-Type", "application/json"), 
            new Pair<>("Allow", "GET, POST")
        );
    }

    @SuppressWarnings("unchecked")
    private void sendResponse(HttpExchange exchange, String response, int statusCode, Pair<String, String> ... headers) throws IOException {
        for (Pair<String, String> header : headers) {
            exchange.getResponseHeaders().set(header.getL(), header.getR());
        }

        exchange.sendResponseHeaders(statusCode, response.getBytes().length);

        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    private String extractUserName(String requestBody) {
        int nameIndex = requestBody.indexOf("\"name\":");
        StringBuilder sb = new StringBuilder();

        if (nameIndex == -1) {
            return null;
        }

        for (int i = nameIndex + 9; i < requestBody.length(); i++) {
            if (requestBody.charAt(i) == '\"') {
                break;
            }

            sb.append(requestBody.charAt(i));
        }

        if (sb.toString().isBlank()) {
            return null;
        }

        return sb.toString();
    }

    private static class User {
        int id;
        String name;

        User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return String.format("{\"id\": %d, \"name\": \"%s\"}", this.id, this.name);
        }
    }
}
