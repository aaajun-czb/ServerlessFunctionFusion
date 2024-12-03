package com.openfaas.function.service;

import com.openfaas.function.entity.OrderTicketsInfo;
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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fdse
 */
public class PreserveServiceImpl implements PreserveService {
    private final OkHttpClient client;
    public PreserveServiceImpl() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Map<String, Object> preserve(OrderTicketsInfo oti) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. 检测票贩子
            Map<String, Object> result = checkSecurity(oti.getAccountId());
            if ((int) result.get("status") == 0) {
                response.put("status", 0);
                response.put("message", result.get("message"));
                response.put("data", null);
            } else {
                response.put("status", 1);
                response.put("message", result.get("message"));
                response.put("data", null);
            }
        } catch (Exception e) {
            response.put("status", 0);
            response.put("message", e.getMessage());
            response.put("data", null);
        }
        return response;
    }

    private Map<String, Object> checkSecurity(String accountId) {
        File jarFile = new File("./check-security.jar");
        if (jarFile.exists()) {
            try {
                String jarOutput = runJarAndGetOutput(jarFile, accountId);
                return parseJarOutput(jarOutput);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return checkSecurityViaHttp(accountId);
    }

    private Map<String, Object> checkSecurityViaHttp(String accountId) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> dataList = new ArrayList<>();
            dataList.add(accountId);
            String json = mapper.writeValueAsString(dataList);
            String requestBody = "{\"container_name\": \"14\", \"jar_name\": \"check-security.jar\",\"data\": " + json + "}";
            // System.out.println(requestBody);
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), requestBody);
            Request request = new Request.Builder()
                    .url("http://127.0.0.1:5000/invoke")
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            ret = response.body().string();
            // System.out.println(ret);
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
}