package com.joseph.backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/hello", new HelloHandler());

        server.setExecutor(null);
        server.start();
        
        System.out.println("Server started on http://localhost:8080");
        System.out.flush();
    }

    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            switch (exchange.getRequestMethod()) {
                case "GET":
                    get(exchange);
                    break;
                default:
                    defaultAction(exchange);
                    break;
            }
            System.out.println(exchange.getRequestMethod() + " " + exchange.getRequestURI());
        }

        private void get(HttpExchange exchange) throws IOException {
            String response = """
                {
                  "message": "Hello from your backend"
                }
                """;
                    
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);

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
    }
}
