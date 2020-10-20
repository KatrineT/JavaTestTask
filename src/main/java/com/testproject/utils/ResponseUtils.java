package com.testproject.utils;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

public class ResponseUtils {

    /**
     * Sends the response back
     *
     * @param exchange the connection to the server
     * @param response response
     * @throws IOException trows in case of connection problems
     */
    public static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream output = exchange.getResponseBody();
        output.write(response.getBytes());
        output.flush();
    }
}
