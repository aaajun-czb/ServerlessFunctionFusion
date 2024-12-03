package com.openfaas.function.service;

import com.openfaas.function.repository.SecurityRepositoryImpl;
import edu.fudan.common.util.JsonUtils;
import com.openfaas.function.entity.*;
import com.openfaas.function.repository.SecurityRepository;
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
public class SecurityServiceImpl implements SecurityService {
    private final OkHttpClient client;
    private SecurityRepository securityRepository = new SecurityRepositoryImpl();
    public SecurityServiceImpl() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    String success = "Success";

    // 判断某个accountid在一个小时内的订单数量是在限制范围内
    @Override
    public Map<String, Object> check(String accountId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Date checkDate = new Date();
            //1.获取自己过去一小时的订单数和总有效票数
            OrderSecurity orderResult = getSecurityOrderInfoFromOrder(accountId, checkDate);
            int orderInOneHour = orderResult.getOrderNumInLastOneHour();
            int totalValidOrder = orderResult.getOrderNumOfValidOrder();

            //2.获取关键配置信息
            SecurityConfig configMaxInHour = securityRepository.findByName("max_order_1_hour");
            SecurityConfig configMaxNotUse = securityRepository.findByName("max_order_not_use");
            int oneHourLine = Integer.parseInt(configMaxInHour.getValue());
            int totalValidLine = Integer.parseInt(configMaxNotUse.getValue());
            if (orderInOneHour > oneHourLine || totalValidOrder > totalValidLine) {
                response.put("status", 0);
                response.put("message", "Too much order in last one hour or too much valid order");
                response.put("data", accountId);
            } else {
                response.put("status", 1);
                response.put("message", "Success");
                response.put("data", accountId);
            }
        } catch (Exception e) {
            response.put("status", 0);
            response.put("message", e.getMessage());
            response.put("data", null);
        }
        return response;
    }

    private OrderSecurity getSecurityOrderInfoFromOrder(String accountId, Date checkDate) {
        File jarFile = new File("./check-security-about-order.jar");
        if (jarFile.exists()) {
            try {
                // 将日期格式化为 yyyy-MM-dd 格式
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String checkDateStr = dateFormat.format(checkDate);
                String jarOutput = runJarAndGetOutput(jarFile, accountId + " " + checkDateStr);
                Map<String, Object> result = parseJarOutput(jarOutput);
                if ((int) result.get("status") == 1) {
                    return JsonUtils.conveterObject(result.get("data"), OrderSecurity.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getSecurityOrderInfoFromOrderViaHttp(accountId, checkDate);
    }

    private OrderSecurity getSecurityOrderInfoFromOrderViaHttp(String accountId, Date checkDate) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 将日期格式化为 yyyy-MM-dd 格式
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String checkDateStr = dateFormat.format(checkDate);
            List<String> dataList = new ArrayList<>();
            dataList.add(accountId);
            dataList.add(checkDateStr);
            String json = mapper.writeValueAsString(dataList);
            String requestBody = "{\"container_name\": \"21\", \"jar_name\": \"check-security-about-order.jar\",\"data\": " + json + "}";
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
        Map<String, Object> result = JsonUtils.json2Object(ret, Map.class);
        return JsonUtils.conveterObject(result.get("data"), OrderSecurity.class);
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