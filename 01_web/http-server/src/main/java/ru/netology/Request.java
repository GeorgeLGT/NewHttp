package ru.netology;

import java.util.*;
import java.util.stream.Collectors;

public class Request {
    private String method;
    private String path;
    private Map<String, List<String>> queryParams;

    public Request(String requestLine) {
        String[] parts = requestLine.split(" ");
        this.method = parts[0];

        String[] urlParts = parts[1].split("\\?");
        this.path = urlParts[0];

        if (urlParts.length > 1) {
            String queryString = urlParts[1];
            this.queryParams = parseQueryString(queryString);
        } else {
            this.queryParams = new HashMap<>();
        }
    }

    private Map<String, List<String>> parseQueryString(String queryString) {
        Map<String, List<String>> params = new HashMap<>();
        String[] pairs = queryString.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            String key = keyValue[0];
            String value = keyValue.length > 1 ? keyValue[1] : "";

            params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return params;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getQueryParam(String name) {
        return queryParams.getOrDefault(name, Collections.emptyList());
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }
}
