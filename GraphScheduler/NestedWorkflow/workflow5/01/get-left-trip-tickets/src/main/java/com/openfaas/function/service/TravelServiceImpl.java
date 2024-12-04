package com.openfaas.function.service;

import com.openfaas.function.repository.TripRepositoryImpl;
import com.openfaas.function.entity.*;
import com.openfaas.function.repository.TripRepository;

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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TravelServiceImpl implements TravelService {
    private TripRepository repository = new TripRepositoryImpl();
    private final OkHttpClient client;
    public TravelServiceImpl() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Map<String, Object> query(TripInfo info) {
        // 首先检查日期是否在今天之后
        if (!afterToday(info.getDepartureTime())) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", 0);
            result.put("message", "Departure time is not after today.");
            result.put("data", null);
            return result;
        }

        String startingPlaceName = info.getStartingPlace();
        String endPlaceName = info.getEndPlace();
        String startingPlaceId = queryForStationId(startingPlaceName);
        String endPlaceId = queryForStationId(endPlaceName);

        List<TripResponse> list = new ArrayList<>();
        List<Trip> allTripList = repository.findAll();
        for (Trip tempTrip : allTripList) {
            Route tempRoute = getRouteByRouteId(tempTrip.getRouteId());
            if (tempRoute.getStations().contains(startingPlaceId) &&
                    tempRoute.getStations().contains(endPlaceId) &&
                    tempRoute.getStations().indexOf(startingPlaceId) < tempRoute.getStations().indexOf(endPlaceId)) {
                int first = getRestTicketNumber(info.getDepartureTime(), tempTrip.getTripId().toString(),
                        startingPlaceName, endPlaceName, SeatClass.FIRSTCLASS.getCode());

                int second = getRestTicketNumber(info.getDepartureTime(), tempTrip.getTripId().toString(),
                        startingPlaceName, endPlaceName, SeatClass.SECONDCLASS.getCode());
                TripResponse response = new TripResponse();
                response.setConfortClass(first);
                response.setEconomyClass(second);
                response.setStartingStation(startingPlaceName);
                response.setTerminalStation(endPlaceName);
                list.add(response);
            }
        }
        // 如果查询到结果，返回结果列表和状态码
        if (!list.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", 1);
            result.put("message", "Query successful.");
            result.put("data", list);
            return result;
        } else {
            // 如果没有查询到结果，返回状态码
            Map<String, Object> result = new HashMap<>();
            result.put("status", 0);
            result.put("message", "No available trips found.");
            result.put("data", null);
            return result;
        }
    }

    private static boolean afterToday(Date date) {
        Calendar calDateA = Calendar.getInstance();
        Date today = new Date();
        calDateA.setTime(today);

        Calendar calDateB = Calendar.getInstance();
        calDateB.setTime(date);

        if (calDateA.get(Calendar.YEAR) > calDateB.get(Calendar.YEAR)) {
            return false;
        } else if (calDateA.get(Calendar.YEAR) == calDateB.get(Calendar.YEAR)) {
            if (calDateA.get(Calendar.MONTH) > calDateB.get(Calendar.MONTH)) {
                return false;
            } else if (calDateA.get(Calendar.MONTH) == calDateB.get(Calendar.MONTH)) {
                return calDateA.get(Calendar.DAY_OF_MONTH) <= calDateB.get(Calendar.DAY_OF_MONTH);
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private String queryForStationId(String stationName) {
        File jarFile = new File("./query-for-station-id-by-station-name.jar");
        if (jarFile.exists()) {
            try {
                List<String> args = new ArrayList<>();
                args.add(stationName);
                String jarOutput = runJarAndGetOutput(jarFile, args);
                return parseJarOutput(jarOutput).get("data").toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        } else {
            return queryForStationIdViaHttp(stationName);
        }
    }

    private String queryForStationIdViaHttp(String stationName) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> dataList = new ArrayList<>();
            dataList.add(stationName);
            String json = mapper.writeValueAsString(dataList);
            String requestBody = "{\"container_name\": \"07\", \"jar_name\": \"query-for-station-id-by-station-name.jar\",\"data\": " + json + "}";
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
        return JsonUtils.json2Object(ret, Map.class).get("data").toString();
    }

    private Route getRouteByRouteId(String routeId) {
        File jarFile = new File("./get-route-by-routeid.jar");
        if (jarFile.exists()) {
            try {
                List<String> args = new ArrayList<>();
                args.add(routeId);
                String jarOutput = runJarAndGetOutput(jarFile, args);
                return JsonUtils.conveterObject(parseJarOutput(jarOutput).get("data"), Route.class);
            } catch (Exception e) {
                e.printStackTrace();
                return new Route();
            }
        } else {
            return getRouteByRouteIdViaHttp(routeId);
        }
    }

    private Route getRouteByRouteIdViaHttp(String routeId) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> dataList = new ArrayList<>();
            dataList.add(routeId);
            String json = mapper.writeValueAsString(dataList);
            String requestBody = "{\"container_name\": \"03\", \"jar_name\": \"get-route-by-routeid.jar\",\"data\": " + json + "}";
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

        return JsonUtils.conveterObject(JsonUtils.json2Object(ret, Map.class).get("data"), Route.class);
    }

    private int getRestTicketNumber(Date travelDate, String trainNumber, String startStationName, String endStationName, int seatType) {
        Seat seatRequest = new Seat();

        String fromId = queryForStationId(startStationName);
        String toId = queryForStationId(endStationName);

        seatRequest.setDestStation(toId);
        seatRequest.setStartStation(fromId);
        seatRequest.setTrainNumber(trainNumber);
        seatRequest.setTravelDate(travelDate);
        seatRequest.setSeatType(seatType);

        File jarFile = new File("./get-left-ticket-of-interval.jar");
        if (jarFile.exists()) {
            try {
                List<String> args = new ArrayList<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                args.add(dateFormat.format(seatRequest.getTravelDate()));
                args.add(seatRequest.getTrainNumber());
                args.add(seatRequest.getStartStation());
                args.add(seatRequest.getDestStation());
                args.add(String.valueOf(seatRequest.getSeatType()));
                String jarOutput = runJarAndGetOutput(jarFile, args);
                return Integer.parseInt(parseJarOutput(jarOutput).get("data").toString());
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        } else {
            return getRestTicketNumberViaHttp(seatRequest);
        }
    }

    private int getRestTicketNumberViaHttp(Seat seatRequest) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> dataList = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dataList.add(dateFormat.format(seatRequest.getTravelDate()));
            dataList.add(seatRequest.getTrainNumber());
            dataList.add(seatRequest.getStartStation());
            dataList.add(seatRequest.getDestStation());
            dataList.add(String.valueOf(seatRequest.getSeatType()));
            String json = mapper.writeValueAsString(dataList);
            String requestBody = "{\"container_name\": \"08\", \"jar_name\": \"get-left-ticket-of-interval.jar\",\"data\": " + json + "}";
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
        return Integer.parseInt(JsonUtils.json2Object(ret, Map.class).get("data").toString());
    }

    private String runJarAndGetOutput(File jarFile, List<String> args) throws Exception {
        // 构建命令
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(jarFile.getAbsolutePath());
        // 使用 for 循环逐个添加 args 列表中的元素
        for (String arg : args) {
            command.add(arg);
        }
        // 使用 ProcessBuilder 构建命令
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
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
                // 检查 responseDataStr 是否为有效的 JSON 格式
                if (responseDataStr.startsWith("{") && responseDataStr.endsWith("}")) {
                    result.put("data", JsonUtils.json2Object(responseDataStr, Map.class));
                } else {
                    // 去掉双引号并放入 data 中
                    if (responseDataStr.startsWith("\"") && responseDataStr.endsWith("\"")) {
                        responseDataStr = responseDataStr.substring(1, responseDataStr.length() - 1);
                    }
                    result.put("data", responseDataStr);
                }
            } catch (Exception e) {
                result.put("data", responseDataStr);
            }
        }

        return result;
    }
}