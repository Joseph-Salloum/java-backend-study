package com.joseph.backend;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class Main {
    public static void main( String[] args ) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/users", new UsersHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server started on http://localhost:8080");
        System.out.flush();
    }
}
