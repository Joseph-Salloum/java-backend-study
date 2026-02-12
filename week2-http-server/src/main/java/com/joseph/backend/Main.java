package com.joseph.backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/hello", new HelloHandler());
        server.createContext("/users", new UsersHandler());

        server.setExecutor(null);
        server.start();
        
        System.out.println("Server started on http://localhost:8080");
        System.out.flush();
    }

    static class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
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

        private void getHandler(HttpExchange exchange) throws IOException {
            String[] requestURI = exchange.getRequestURI().getPath().split("/");
            String response = "";
            int statusCode = 200;

            switch (requestURI.length) {
                case 2:
                    response = getAllUsers();
                    break;
                case 3:
                    String userJson = getUser(requestURI[2]);
                    if (userJson == null) {
                        sendFailureMessage("No user with the provided ID", 404, exchange);
                        return;
                    } else {
                        response = userJson;
                    }
                    break;
                default:
                    sendFailureMessage("No such endpoint", 404, exchange);
                    return;
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        private void postHandler(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String response = "";
            int statusCode = 200;

            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(body);
            is.close();

            if (!path.equals("/users")) {
                sendFailureMessage("No such endpoint", 404, exchange);
                return;
            } else if (body == null || body.isBlank()) {
                sendFailureMessage("Body cannot be empty", 400, exchange);
                return;
            } else {
                StringBuilder sb = new StringBuilder();
                int nameIndex = body.indexOf("\"name\":");
                if (nameIndex == -1) {
                    sendFailureMessage("No name provided", 400, exchange);
                    return;
                }
                for (int i = nameIndex + 9; i < body.length(); i++) {
                    if (body.charAt(i) == '\"') break;

                    sb.append(body.charAt(i));
                }
                if (sb.toString().isBlank()) {
                    sendFailureMessage("Name cannot be empty", 400, exchange);
                    return;
                }

                response = String.format("""
                            {
                                "message": "User %s created successfully"
                            }
                    """, sb.toString());
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        private void defaultHandler(HttpExchange exchange) throws IOException {
            String response = """
                    {
                        "message": "The only supported methods are GET and POST"
                    }
                    """;

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Allow", "GET, POST");
            exchange.sendResponseHeaders(405, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private String getAllUsers() {
            return """
                    [
                        {"id": 1, "name": "Joseph"},
                        {"id": 2, "name": "Diana"}
                    ]
                    """;
        }
        private String getUser(String id) {
            if (id.equals("1")) {
                return """
                        {"id": 1, "name": "Joseph"}
                        """;
            } else if (id.equals("2")) {
                return """
                        {"id": 2, "name": "Diana"}
                        """;
            } else {
                return null;
            }
        }
        private void sendFailureMessage(String message, int statusCode, HttpExchange exchange) throws IOException {
            String response = String.format("""
                    {
                        "message": "%s"
                    }
                """, message);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            switch (exchange.getRequestMethod()) {
                case "GET":
                    getAction(exchange);
                    break;
                default:
                    defaultAction(exchange);
                    break;
            }
            System.out.println(exchange.getRequestMethod() + " " + exchange.getRequestURI());
        }

        private void getAction(HttpExchange exchange) throws IOException {
            String[] requestURI = exchange.getRequestURI().getPath().split("/");
            String response = "";
            int statusCode = 200;

            switch (requestURI.length) {
                case 2:
                    response = getHelloResponse();
                    break;
                case 3:
                    response = getIdHelloResponse(requestURI[2], false);
                    break;
                case 4:
                    if (!requestURI[3].equals("details")) {
                        response = """
                                {
                                    "message": "No such endpoint"
                                }
                                """;
                        statusCode = 404;
                        break;
                    }
                    response = getIdHelloResponse(requestURI[2], true);
                    break;
                default:
                    response = """
                            {
                                "message": "No such endpoint"
                            }
                            """;
                    statusCode = 404;
                    break;
            }

                    
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        private void defaultAction(HttpExchange exchange) throws IOException {
            String response = """
                    {
                      "message": "The only supported HTTP method is GET"
                    }
                    """;
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Allow", "GET");
            exchange.sendResponseHeaders(405, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        private String getHelloResponse() {
            return """
                    {
                        "message": "Thanks for using our backend services"
                    }
                    """;
        }
        private String getIdHelloResponse(String id, boolean withDetails) {
            if (withDetails) {
                return String.format("""
                        {
                            "message": "Thanks for using our backend services Mr.%s",
                            "details": "The great backend engineer who wrote this code is Joseph"
                        }
                    """, id);
            }

            return String.format("""
                    {
                        "message": "Thanks for using our backend services Mr.%s"
                    }
                    """, id);
        }
    }
}
