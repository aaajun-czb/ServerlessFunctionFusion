package com.openfaas.function.service;

import com.openfaas.function.entity.*;
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

/**
 * @author fdse
 */
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
    String success = "Success";
    String noContent = "No Content";

    @Override
    public Map<String, Object> getTrainTypeByTripId(TripId tripId) {
        TrainType trainType = null;
        Trip trip = repository.findByTripId(tripId);
        if (trip != null) {
            trainType = getTrainType(trip.getTrainTypeId());
        }

        Map<String, Object> response = new HashMap<>();
        if (trainType != null) {
            response.put("status", 1);
            response.put("message", success);
            response.put("data", trainType);
        } else {
            response.put("status", 0);
            response.put("message", noContent);
            response.put("data", null);
        }
        return response;
    }

    private TrainType getTrainType(String trainTypeId) {
        File jarFile = new File("./get-traintype-by-traintypeid.jar");
        if (jarFile.exists()) {
            try {
                String jarOutput = runJarAndGetOutput(jarFile, trainTypeId);
                Map<String, Object> result = parseJarOutput(jarOutput);
                if ((int) result.get("status") == 1) {
                    return JsonUtils.conveterObject(result.get("data"), TrainType.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getTrainTypeViaHttp(trainTypeId);
    }

    private TrainType getTrainTypeViaHttp(String trainTypeId) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> dataList = new ArrayList<>();
            dataList.add(trainTypeId);
            String json = mapper.writeValueAsString(dataList);
            String requestBody = "{\"container_name\": \"04\", \"jar_name\": \"get-traintype-by-traintypeid.jar\",\"data\": " + json + "}";
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
        Map<String, Object> TrainTypeRes = JsonUtils.json2Object(ret, Map.class);

        TrainType trainType = null;
        if ((int) TrainTypeRes.get("status") == 1) {
            trainType = JsonUtils.conveterObject(TrainTypeRes.get("data"), TrainType.class);
        }
        return trainType;
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