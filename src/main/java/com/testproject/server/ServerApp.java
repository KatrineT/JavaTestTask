package com.testproject.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import com.testproject.model.FilmPage;
import lombok.extern.log4j.Log4j;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static com.testproject.utils.RequestUtils.getRequestedQueryParameter;

@Log4j
public class ServerApp {
    private static final int SERVER_PORT = 8000;

    public ServerApp() {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            server.createContext("/test", (exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    ObjectMapper mapper = new ObjectMapper();
                    int page = getRequestedQueryParameter(exchange.getRequestURI(), "page");
                    File file = new File(String.format("./src/test/resources/films%s.json", page));
                    String json = "";
                    if (!file.exists()) {
                        exchange.sendResponseHeaders(400, json.getBytes().length);
                    } else {
                        FilmPage filmPage = mapper.readValue(file, FilmPage.class);
                        json = mapper.writeValueAsString(filmPage);
                        Headers responseHeaders = exchange.getResponseHeaders();
                        responseHeaders.add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, json.getBytes().length);
                    }
                    OutputStream output = exchange.getResponseBody();
                    output.write(json.getBytes());
                    output.flush();
                }
                exchange.close();
            }));
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
           log.error("Cannot create server");
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {
        new ServerApp();
    }
}
