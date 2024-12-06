package com.openfaas.function.service;

import com.openfaas.function.entity.*;
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

public class SeatServiceImpl implements SeatService {
    private final OkHttpClient client;
    public SeatServiceImpl() {
        client = new OkHttpClient.Builder()
                .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Map<String, Object> getLeftTicketOfInterval(Seat seatRequest) {
        int numOfLeftTicket = 0;
        Map<String, Object> response = new HashMap<>();

        try {
            // Call the micro service to query all the station information for the trains
            Map<String, Object> routeResult = getRouteByTripId(seatRequest.getTrainNumber());

            // Call the micro service to query for residual Ticket information
            Map<String, Object> mRes = getSoldTickets(seatRequest);
            LeftTicketInfo leftTicketInfo = JsonUtils.conveterObject(mRes.get("data"), LeftTicketInfo.class);

            // Call the microservice to query the total number of seats specified for that vehicle
            Map<String, Object> trainTypeResult = getTrainTypeByTripId(seatRequest.getTrainNumber());
            TrainType trainType = JsonUtils.conveterObject(trainTypeResult.get("data"), TrainType.class);

            // Counting the seats remaining in certain sections
            List<String> stationList = JsonUtils.conveterObject(routeResult.get("data"), Route.class).getStations();
            int seatTotalNum;
            if (seatRequest.getSeatType() == SeatClass.FIRSTCLASS.getCode()) {
                seatTotalNum = trainType.getConfortClass();
            } else {
                seatTotalNum = trainType.getEconomyClass();
            }

            int solidTicketSize = 0;
            if (leftTicketInfo != null) {
                String startStation = seatRequest.getStartStation();
                Set<Ticket> soldTickets = leftTicketInfo.getSoldTickets();
                solidTicketSize = soldTickets.size();
                for (Ticket soldTicket : soldTickets) {
                    String soldTicketDestStation = soldTicket.getDestStation();
                    if (stationList.indexOf(soldTicketDestStation) < stationList.indexOf(startStation)) {
                        numOfLeftTicket++;
                    }
                }
            }

            double direstPart = getDirectProportion();
            Route route = JsonUtils.conveterObject(routeResult.get("data"), Route.class);
            if (route.getStations().get(0).equals(seatRequest.getStartStation()) &&
                    route.getStations().get(route.getStations().size() - 1).equals(seatRequest.getDestStation())) {
                // do nothing
            } else {
                direstPart = 1.0 - direstPart;
            }

            int unusedNum = (int) (seatTotalNum * direstPart) - solidTicketSize;
            numOfLeftTicket += unusedNum;

            response.put("status", 1);
            response.put("message", "Get Left Ticket of Internal Success");
            response.put("data", numOfLeftTicket);
        } catch (Exception e) {
            response.put("status", 0);
            response.put("message", e.getMessage());
            response.put("data", null);
        }

        return response;
    }

    private double getDirectProportion() {
        String configName = "DirectTicketAllocationProportion";
        Map<String, Object> configValue = getConfigByName(configName);
        return Double.parseDouble(JsonUtils.conveterObject(configValue.get("data"), Config.class).getValue());
    }

    private Map<String, Object> getRouteByTripId(String tripId) {
        File jarFile = new File("./get-route-by-tripid.jar");
        if (jarFile.exists()) {
            try {
                String jarOutput = runJarAndGetOutput(jarFile, tripId);
                return parseJarOutput(jarOutput);
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.singletonMap("status", 0);
            }
        } else {
            return getRouteByTripIdViaHttp(tripId);
        }
    }

    private Map<String, Object> getRouteByTripIdViaHttp(String tripId) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> dataList = new ArrayList<>();
            dataList.add(tripId);
            String json = mapper.writeValueAsString(dataList);
            // 读取环境变量 CALLING_CONTAINER
            String callingContainer = System.getenv("CALLING_CONTAINER");
            String container_name = System.getenv("09");
            String requestBody = "{\"container_name\": \"" + container_name + "\", \"jar_name\": \"get-route-by-tripid.jar\", \"calling_container\": \"" + callingContainer + "\" , \"data\": " + json + "}";
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
        return JsonUtils.json2Object(ret, Map.class);
    }

    private Map<String, Object> getSoldTickets(Seat seatRequest) {
        File jarFile = new File("./get-sold-tickets.jar");
        if (jarFile.exists()) {
            try {
                String seatRequestStr = seatToString(seatRequest);
                String jarOutput = runJarAndGetOutput(jarFile, seatRequestStr);
                return parseJarOutput(jarOutput);
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.singletonMap("status", 0);
            }
        } else {
            return getSoldTicketsViaHttp(seatRequest);
        }
    }

    private Map<String, Object> getSoldTicketsViaHttp(Seat seatRequest) {
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
            // 读取环境变量 CALLING_CONTAINER
            String callingContainer = System.getenv("CALLING_CONTAINER");
            String container_name = System.getenv("10");
            String requestBody = "{\"container_name\": \"" + container_name + "\", \"jar_name\": \"get-sold-tickets.jar\", \"calling_container\": \"" + callingContainer + "\" , \"data\": " + json + "}";
            // System.out.println(requestBody);
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
        return JsonUtils.json2Object(ret, Map.class);
    }

    private Map<String, Object> getTrainTypeByTripId(String tripId) {
        File jarFile = new File("./get-traintype-by-tripid.jar");
        if (jarFile.exists()) {
            try {
                String jarOutput = runJarAndGetOutput(jarFile, tripId);
                return parseJarOutput(jarOutput);
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.singletonMap("status", 0);
            }
        } else {
            return getTrainTypeByTripIdViaHttp(tripId);
        }
    }

    private Map<String, Object> getTrainTypeByTripIdViaHttp(String tripId) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> dataList = new ArrayList<>();
            dataList.add(tripId);
            String json = mapper.writeValueAsString(dataList);
            // 读取环境变量 CALLING_CONTAINER
            String callingContainer = System.getenv("CALLING_CONTAINER");
            String container_name = System.getenv("11");
            String requestBody = "{\"container_name\": \"" + container_name + "\", \"jar_name\": \"get-traintype-by-tripid.jar\", \"calling_container\": \"" + callingContainer + "\" , \"data\": " + json + "}";
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
        return JsonUtils.json2Object(ret, Map.class);
    }

    private Map<String, Object> getConfigByName(String configName) {
        File jarFile = new File("./query-config-entity-by-config-name.jar");
        if (jarFile.exists()) {
            try {
                String jarOutput = runJarAndGetOutput(jarFile, configName);
                return parseJarOutput(jarOutput);
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.singletonMap("status", 0);
            }
        } else {
            return getConfigByNameViaHttp(configName);
        }
    }

    private Map<String, Object> getConfigByNameViaHttp(String configName) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> dataList = new ArrayList<>();
            dataList.add(configName);
            String json = mapper.writeValueAsString(dataList);
            // 读取环境变量 CALLING_CONTAINER
            String callingContainer = System.getenv("CALLING_CONTAINER");
            String container_name = System.getenv("12");
            String requestBody = "{\"container_name\": \"" + container_name + "\", \"jar_name\": \"query-config-entity-by-config-name.jar\", \"calling_container\": \"" + callingContainer + "\" , \"data\": " + json + "}";
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
        return JsonUtils.json2Object(ret, Map.class);
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

    private String seatToString(Seat seat) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(seat.getTravelDate()) + " " +
               seat.getTrainNumber() + " " +
               seat.getStartStation() + " " +
               seat.getDestStation() + " " +
               seat.getSeatType();
    }
}