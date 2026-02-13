package com.joseph.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UsersHandler implements HttpHandler {
    //Better performance on large-scale systems than compiling the regex each request
    private static final Pattern USER_ID_PATTERN = Pattern.compile("/users/\\d+");

    private static final List<User> users = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public void handle(HttpExchange exchange) throws IOException{
        switch (exchange.getRequestMethod()) {
            case "GET":
                getHandler(exchange);
                break;
            case "POST":
                postHandler(exchange);
                break;
            case "PUT":
                putHandler(exchange);
                break;
            case "DELETE":
                deleteHandler(exchange);
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
            if (users.isEmpty()) {
                response = "[]";
                sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
                return;
            }

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
        } else if (USER_ID_PATTERN.matcher(requestPath).matches()) {
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
        } else {
            response = """
                    {
                        "message": "Invalid request path"
                    }
                    """;
            statusCode = 404;
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

        User newUser = new User(nextId.getAndIncrement(), userName);
        users.add(newUser);

        response = String.format("""
                {
                    "user": {"id": %d, "name": "%s"}
                }
                """, newUser.id, newUser.name);

        sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
    }
    @SuppressWarnings("unchecked")
    private void putHandler(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String response = "";
        int statusCode = 200;

        if (!USER_ID_PATTERN.matcher(requestPath).matches()) {
            response = """
                    {
                        "message": "Invalid path"
                    }
                    """;
            statusCode = 404;
            sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();
        if (body.isBlank()) {
            response = """
                    {
                        "message": "Request body cannot be blank"
                    }
                    """;
            statusCode = 400;
            sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            return;
        }

        int userId = Integer.parseInt(requestPath.split("/")[2]);
        String newUserName = extractUserName(body);

        if (newUserName == null || newUserName.isBlank()) {
            response = """
                    {
                        "message": "Username cannot be empty"
                    }
                    """;
            statusCode = 400;
            sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            return;
        }

        User foundUser = null;

        for (User u : users) if (u.id == userId) {foundUser = u; break;}

        if (foundUser == null) {
            response = """
                    {
                        "message": "No user with the provided id"
                    }
                    """;
            statusCode = 404;
            sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
            return;
        }

        foundUser.name = newUserName;
        response = foundUser.toString();
        sendResponse(exchange, response, statusCode, new Pair<>("Content-Type", "application/json"));
    }
    @SuppressWarnings("unchecked")
    private void deleteHandler(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();

        if (!USER_ID_PATTERN.matcher(requestPath).matches()) {
            String response = """
                    {
                        "message": "Invalid path"
                    }
                    """;
            sendResponse(exchange, response, 404, new Pair<>("Content-Type", "application/json"));
            return;
        }

        int userId = Integer.parseInt(requestPath.split("/")[2]);
        
        if (users.removeIf(user -> user.id == userId)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        } else {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
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
            new Pair<>("Allow", "GET, POST, PUT, DELETE")
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
