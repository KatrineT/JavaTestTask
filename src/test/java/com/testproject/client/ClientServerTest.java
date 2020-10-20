package com.testproject.client;


import com.testproject.server.ServerApp;
import com.testproject.utils.RequestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;

public class ClientServerTest {
    private static final String FETCH_ENDPOINT = "http://localhost:8001/fetch/data";
    private static final String GET_ENDPOINT = "http://localhost:8001/get/estimation";
    private static final String STOP_ENDPOINT = "http://localhost:8001/stop/fetch";

    private static ServerApp testServer;
    private static ClientServer clientServer;

    @BeforeClass
    public static void setup() {
        testServer = new ServerApp();
        clientServer = new ClientServer("http://localhost:8000/test");
    }

    @Test
    public void testFullResult() throws IOException, InterruptedException {
        HttpURLConnection fetchConnection = sendFetchRequest();
        HttpURLConnection getRequest = sendRequestWithCookie(fetchConnection.getHeaderField("Set-Cookie"), GET_ENDPOINT);
        String estimation = RequestUtils.readResponse(getRequest);
        assertEquals("13149.0", estimation);
    }

    @Test
    public void testGetIntermediateResult() throws IOException, InterruptedException {
        clientServer.setSleepTime(100);
        HttpURLConnection fetchConnection = sendFetchRequest();
        HttpURLConnection getRequest = sendRequestWithCookie(fetchConnection.getHeaderField("Set-Cookie"), GET_ENDPOINT);
        String estimation = RequestUtils.readResponse(getRequest);
        assertNotEquals("13149.0", estimation);
        assertNotEquals("0.0", estimation);
        assertTrue(estimation.contains("%"));
    }

    @Test
    public void testStopComputing() throws IOException, InterruptedException {
        clientServer.setSleepTime(100);
        HttpURLConnection fetchConnection = sendFetchRequest();
        String cookie = fetchConnection.getHeaderField("Set-Cookie");
        sendRequestWithCookie(cookie, STOP_ENDPOINT);
        Thread.sleep(1000);

        HttpURLConnection getRequest = sendRequestWithCookie(cookie, GET_ENDPOINT);
        String firstEstimation = RequestUtils.readResponse(getRequest);
        Thread.sleep(2000);

        getRequest = sendRequestWithCookie(cookie, GET_ENDPOINT);
        String secondEstimation = RequestUtils.readResponse(getRequest);

        assertEquals(firstEstimation, secondEstimation);
        assertTrue(secondEstimation.contains("%"));
    }

    private HttpURLConnection sendRequestWithCookie(String cookie, String url) throws IOException {
        HttpURLConnection request = (HttpURLConnection) new URL(url).openConnection();
        request.setRequestProperty("Cookie", cookie);
        assertEquals(HttpURLConnection.HTTP_OK, request.getResponseCode());
        return request;
    }

    private HttpURLConnection sendFetchRequest() throws IOException, InterruptedException {
        HttpURLConnection fetchConnection = (HttpURLConnection) new URL(FETCH_ENDPOINT + "?genre_id=1").openConnection();
        assertEquals(HttpURLConnection.HTTP_OK, fetchConnection.getResponseCode());
        Thread.sleep(1000);
        return fetchConnection;
    }
}
