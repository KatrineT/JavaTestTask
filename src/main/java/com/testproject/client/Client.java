package com.testproject.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.testproject.model.Film;
import com.testproject.model.FilmPage;
import lombok.extern.log4j.Log4j;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.testproject.utils.RequestUtils.getRequestedQueryParameter;
import static com.testproject.utils.RequestUtils.readResponse;

@Log4j
public class Client {
    private static final String COOKIE_GET = "Cookie";
    private static final String GENRE_QUERY = "genre_id";

    private static Map<String, EstimationContext> resultBySession = new HashMap<>();

    private String baseUrl;
    private long sleepTime;

    Client(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Fetch data from the server
     *
     * @param exchange connection to the client to get session cookie
     */
    void fetchData(HttpExchange exchange) {
        String session = getSession(exchange);
        stopPreviousComputing(session);
        resultBySession.put(session, new EstimationContext());
        fetchData(session, getRequestedQueryParameter(exchange.getRequestURI(), GENRE_QUERY));
    }

    /**
     * Calculates estimation in background process
     *
     * @param exchange connection to the client to get session cookie
     */
    String getEstimation(HttpExchange exchange) {
        String result = "No request for computing was found";
        if (exchange.getRequestHeaders().containsKey(COOKIE_GET)) {
            String session = exchange.getRequestHeaders().get(COOKIE_GET).get(0).replace("JSESSIONID=", "");
            if (resultBySession.containsKey(session)) {
                EstimationContext estimationContext = resultBySession.get(session);
                result = estimationContext.getEstimation();
            }
        }
        return result;
    }

    /**
     * Stops calculation
     *
     * @param exchange connection to the client to get session cookie
     */
    String stopEstimation(HttpExchange exchange) {
        String result = "No request for computing was found";
        if (exchange.getRequestHeaders().containsKey(COOKIE_GET)) {
            String session = exchange.getRequestHeaders().get(COOKIE_GET).get(0).replace("JSESSIONID=", "");
            if (resultBySession.containsKey(session)) {
                resultBySession.get(session).setContinuing(false);
                result = "Successfully stopped";
            }
        }
        return result;
    }

    /**
     * Sets sleeping time **FOR TEST PURPOSE ONLY**
     *
     * @param sleepTime
     */
    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    private void stopPreviousComputing(String session) {
        if (resultBySession.containsKey(session)) {
            resultBySession.get(session).setContinuing(false);
        }
    }

    private String getSession(HttpExchange exchange) {
        String session;
        if (!exchange.getRequestHeaders().containsKey(COOKIE_GET)) {
            session = UUID.randomUUID().toString();
            exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=" + session);
        } else {
            session = exchange.getRequestHeaders().get(COOKIE_GET).get(0).replace("JSESSIONID=", "");
        }
        return session;
    }

    private void fetchData(String session, int id) {
        List<CompletableFuture<Double>> pages = new ArrayList<>();
        int pageNumber = 1;
        int countOfPages = getCountOfPages(baseUrl + "?page=1");
        CompletableFuture<Double> page;
        while (pageNumber <= countOfPages) {
            page = fetchOnePage(session, id, baseUrl + "?page=" + pageNumber);
            pages.add(page);
            pageNumber++;
        }

        combineAllResults(pages, resultBySession.get(session));
    }

    private int getCountOfPages(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(connection);
                ObjectMapper mapper = new ObjectMapper();
                FilmPage filmPage = mapper.readValue(response, FilmPage.class);
                return filmPage.getTotalPages();
            }
        } catch (IOException e) {
            log.error("Something went wrong with fetching data from the server");
            e.printStackTrace();
        }
        return 0;
    }

    private CompletableFuture<Double> fetchOnePage(String session, int id, String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(connection);
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(response, FilmPage.class);
                }
            } catch (IOException e) {
                log.error("Something went wrong with fetching data from the server");
                e.printStackTrace();
            }
            return null;
        }).thenApplyAsync((filmPage) -> processFilms(session, filmPage, id));
    }

    private double processFilms(String session, FilmPage filmPage, int id) {
        EstimationContext estimationContext = resultBySession.get(session);
        List<Film> films = filmPage.getContent();
        estimationContext.setSize(filmPage.getTotalElements());
        double estimation = 0;
        for (Film film : films) {
            if (estimationContext.isContinuing()) {
                if (film.getGenre() == id) {
                    // Just for test purpose...
                    sleep();
                    estimation += film.getEstimation();
                }
                estimationContext.getCount().incrementAndGet();
            } else {
                break;
            }
        }
        return estimation;
    }

    private void combineAllResults(List<CompletableFuture<Double>> pages, EstimationContext estimationContext) {
        CompletableFuture.runAsync(() -> {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    pages.toArray(new CompletableFuture[0])
            );
            CompletableFuture<Double> result = allFutures.thenApply(v -> pages.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()))
                    .thenApply(estimations -> estimations.stream().mapToDouble(f -> f).sum());
            try {
                estimationContext.setEstimation(result.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Cannot get final estimation");
                e.printStackTrace();
            }
        });
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
