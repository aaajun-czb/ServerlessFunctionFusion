package com.openfaas.function.service;

import com.openfaas.function.entity.Route;
import com.openfaas.function.entity.Trip;
import com.openfaas.function.entity.TripId;
import com.openfaas.function.repository.TripRepository;
import com.openfaas.function.repository.TripRepositoryImpl;
import edu.fudan.common.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TravelServiceImpl implements TravelService {
    private TripRepository repository = new TripRepositoryImpl();
    private final OkHttpClient client;
    public TravelServiceImpl() {
        client = new OkHttpClient.Builder()
                .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }
    String success = "Success";
    String noContent = "No Content";

    @Override
    public Map<String, Object> getRouteByTripId(TripId tripId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Route route = null;
            if (tripId != null) {
                Trip trip = repository.findByTripId(tripId);
                if (trip != null) {
                    route = getRouteByRouteId(trip.getRouteId());
                }
            }
            if (route != null) {
                response.put("status", 1);
                response.put("message", success);
                response.put("data", route);
            } else {
                response.put("status", 0);
                response.put("message", noContent);
                response.put("data", null);
            }
        } catch (Exception e) {
            response.put("status", 0);
            response.put("message", e.getMessage());
            response.put("data", null);
        }
        return response;
    }

    private Route getRouteByRouteId(String routeId) {
        File jarFile = new File("./get-route-by-routeid.jar");
        if (jarFile.exists()) {
            try {
                String jarOutput = runJarAndGetOutput(jarFile, routeId);
                Map<String, Object> result = parseJarOutput(jarOutput);
                if ((int) result.get("status") == 1) {
                    return JsonUtils.conveterObject(result.get("data"), Route.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getRouteByRouteIdViaHttp(routeId);
    }

    private Route getRouteByRouteIdViaHttp(String routeId) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> dataList = new ArrayList<>();
            dataList.add(routeId);
            String json = mapper.writeValueAsString(dataList);
            // 读取环境变量 CALLING_CONTAINER
            String callingContainer = System.getenv("CALLING_CONTAINER");
            String container_name = System.getenv("03");
            String requestBody = "{\"container_name\": \"" + container_name + "\", \"jar_name\": \"get-route-by-routeid.jar\", \"calling_container\": \"" + callingContainer + "\" , \"data\": " + json + "}";
             RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), requestBody);
            Request request = new Request.Builder()
                    .url("http://127.0.0.1:5000/invoke")
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            ret = response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Object> routeRes = JsonUtils.json2Object(ret, Map.class);

        Route route = null;
        if ((int) routeRes.get("status") == 1) {
            route = JsonUtils.conveterObject(routeRes.get("data"), Route.class);
        }
        return route;
    }

    private String runJarAndGetOutput(File jarFile, String args) throws Exception {
        String command = "java -jar " + jarFile.getAbsolutePath() + " " + args;
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        }
    }

    public Map<String, Object> parseJarOutput(String jarOutput) {
        Map<String, Object> result = new HashMap<>();
        Pattern statusPattern = Pattern.compile("Response Status: (\\d+)");
        Matcher statusMatcher = statusPattern.matcher(jarOutput);
        if (statusMatcher.find()) {
            result.put("status", Integer.parseInt(statusMatcher.group(1)));
        }

        Pattern messagePattern = Pattern.compile("Response Message: (.+)");
        Matcher messageMatcher = messagePattern.matcher(jarOutput);
        if (messageMatcher.find()) {
            result.put("message", messageMatcher.group(1));
        }

        Pattern dataPattern = Pattern.compile("Response Data: (.+)");
        Matcher dataMatcher = dataPattern.matcher(jarOutput);
        if (dataMatcher.find()) {
            String responseDataStr = dataMatcher.group(1).trim();
            try {
                result.put("data", JsonUtils.json2Object(responseDataStr, Map.class));
            } catch (Exception e) {
                result.put("data", responseDataStr);
            }
        }

        return result;
    }
}