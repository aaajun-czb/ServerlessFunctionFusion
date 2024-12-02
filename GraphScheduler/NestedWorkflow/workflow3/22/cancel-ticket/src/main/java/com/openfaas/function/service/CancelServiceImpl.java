package com.openfaas.function.service;

import com.openfaas.function.entity.Order;
import com.openfaas.function.entity.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.fudan.common.util.JsonUtils;
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
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CancelServiceImpl implements CancelService {
    private final OkHttpClient client;

    public CancelServiceImpl() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    private static final String ORDER_STATUS_CANCEL_NOT_PERMITTED = "Order Status Cancel Not Permitted";

    @Override
    public Map<String, Object> cancelOrder(String orderId, String loginId) {
        Map<String, Object> response = new HashMap<>();
        // System.out.println(orderId);
        Map<String, Object> orderResult = getOrderByIdFromOrder(orderId);
        if ((int) orderResult.get("status") == 1) {
            Order order = JsonUtils.conveterObject(orderResult.get("data"), Order.class);
            if (order.getStatus() == OrderStatus.NOTPAID.getCode()
                    || order.getStatus() == OrderStatus.PAID.getCode()
                    || order.getStatus() == OrderStatus.CHANGE.getCode()) {
                order.setStatus(OrderStatus.CANCEL.getCode());
                Map<String, Object> changeOrderResult = cancelFromOrder(order);
                // 0 -- not find order   1 - cancel success
                if ((int) changeOrderResult.get("status") == 1) {
                    // Draw back money
                    double money = calculateRefund(order);
                    boolean status = drawbackMoney(money, loginId);
                    if (status) {
                        response.put("status", 1);
                        response.put("message", "Success.");
                        response.put("data", null);
                    } else {
                        response.put("status", 0);
                        response.put("message", "Drawback failed.");
                        response.put("data", null);
                    }
                } else {
                    response.put("status", 0);
                    response.put("message", changeOrderResult.get("message"));
                    response.put("data", null);
                }
            } else {
                response.put("status", 0);
                response.put("message", ORDER_STATUS_CANCEL_NOT_PERMITTED);
                response.put("data", null);
            }
        } else {
            response.put("status", 0);
            response.put("message", "Order Not Found.");
            response.put("data", null);
        }

        return response;
    }

    private double calculateRefund(Order order) {
        if (order.getStatus() == OrderStatus.NOTPAID.getCode()) {
            return 0.00;
        }
        Date nowDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(order.getTravelDate());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(order.getTravelTime());
        int hour = cal2.get(Calendar.HOUR);
        int minute = cal2.get(Calendar.MINUTE);
        int second = cal2.get(Calendar.SECOND);
        Date startTime = new Date(year, month, day, hour, minute, second);
        if (nowDate.after(startTime)) {
            return 0.0;
        } else {
            double totalPrice = Double.parseDouble(order.getPrice());
            double price = totalPrice * 0.8;
            return price;
        }
    }

    // func_29
    private Map<String, Object> cancelFromOrder(Order order) {
        File jarFile = new File("./save-order-info.jar");
        if (jarFile.exists()) {
            try {
                String jarOutput = runJarAndGetOutput(jarFile, JsonUtils.object2Json(order));
                return parseJarOutput(jarOutput);
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.singletonMap("status", 0);
            }
        } else {
            return cancelFromOrderViaHttp(order);
        }
    }

    private Map<String, Object> cancelFromOrderViaHttp(Order order) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(order);
            String requestBody = "{\"container_name\": \"29\", \"jar_name\": \"save-order-info.jar\",\"data\": " + json + "}";
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
        return result;
    }

    // func_28
    public boolean drawbackMoney(double money, String userId) {
        File jarFile = new File("./drawback.jar");
        if (jarFile.exists()) {
            try {
                String jarOutput = runJarAndGetOutput(jarFile, userId + " " + money);
                Map<String, Object> result = parseJarOutput(jarOutput);
                return (int) result.get("status") == 1;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return drawbackMoneyViaHttp(money, userId);
        }
    }

    private boolean drawbackMoneyViaHttp(double money, String userId) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 创建一个包含 (userID,money) 的列表
            List<String> dataList = new ArrayList<>();
            dataList.add(userId);
            dataList.add(String.valueOf(money));
            String json = mapper.writeValueAsString(dataList);
            String requestBody = "{\"container_name\": \"28\", \"jar_name\": \"drawback.jar\",\"data\": " + json + "}";
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
        return (int) result.get("status") == 1;
    }

    // func_23
    private Map<String, Object> getOrderByIdFromOrder(String orderId) {
        File jarFile = new File("./get-order-by-id.jar");
        if (jarFile.exists()) {
            try {
                String jarOutput = runJarAndGetOutput(jarFile, orderId);
                return parseJarOutput(jarOutput);
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.singletonMap("status", 0);
            }
        } else {
            return getOrderByIdViaHttp(orderId);
        }
    }

    private Map<String, Object> getOrderByIdViaHttp(String orderId) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(orderId);
            String requestBody = "{\"container_name\": \"23\", \"jar_name\": \"get-order-by-id.jar\",\"data\": " + json + "}";
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
        return result;
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