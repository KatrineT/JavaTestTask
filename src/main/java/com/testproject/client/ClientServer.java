package com.testproject.client;

import com.sun.net.httpserver.HttpServer;
import lombok.extern.log4j.Log4j;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.testproject.utils.ResponseUtils.sendResponse;

@Log4j
public class ClientServer {
    private static final int SERVER_PORT = 8001;
    private static final String SERVER_PATH = "http://localhost:8000/test";

    private final Client client;

    public ClientServer(String baseUrl) {
        client = new Client(baseUrl);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            setFetchEndpoint(server);
            setResultEndpoint(server);
            setStopEndpoint(server);
            server.setExecutor(null);
            server.start();
            log.info("Client started on port: " + SERVER_PORT);
        } catch (IOException e) {
            log.error("Cannot create client");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ClientServer(SERVER_PATH);
    }

    /**
     * Sets sleeping time **FOR TEST PURPOSE ONLY**
     *
     * @param sleepTime sleep time
     */
    public void setSleepTime(long sleepTime) {
        client.setSleepTime(sleepTime);
    }

    private void setFetchEndpoint(HttpServer server) {
        server.createContext("/fetch/data", (exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                client.fetchData(exchange);
                sendResponse(exchange, "Request sent");
            }
            exchange.close();
        }));
    }

    private void setResultEndpoint(HttpServer server) {
        server.createContext("/get/estimation", (exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                String result = client.getEstimation(exchange);
                sendResponse(exchange, result);
            }
            exchange.close();
        }));
    }

    private void setStopEndpoint(HttpServer server) {
        server.createContext("/stop/fetch", (exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                String result = client.stopEstimation(exchange);
                sendResponse(exchange, result);
            }
            exchange.close();
        }));
    }

}
