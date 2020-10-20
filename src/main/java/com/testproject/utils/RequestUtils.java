package com.testproject.utils;

import lombok.extern.log4j.Log4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

@Log4j
public class RequestUtils {
    /**
     * Extracts the value of the specified query param
     *
     * @param requestURI URI
     * @param paramName  param name
     * @return value of the query param
     */
    public static int getRequestedQueryParameter(URI requestURI, String paramName) {
        String query = requestURI.getRawQuery();
        int result = -1;
        if (query == null || "".equals(query)) {
            return result;
        }
        String[] queries = query.split("&");
        for (String q : queries) {
            if (q.contains(paramName)) {
                result = Integer.parseInt(q.split("=")[1]);
            }
        }
        return result;
    }

    /**
     * Reads data from connection
     *
     * @param connection connection
     * @return response
     */
    public static String readResponse(HttpURLConnection connection) {
        String readLine;
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            while ((readLine = in.readLine()) != null) {
                response.append(readLine);
            }
        } catch (IOException e) {
            log.error("Cannot read response");
            e.printStackTrace();
        }
        return response.toString();
    }

}
