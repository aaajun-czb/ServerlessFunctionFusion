package com.openfaas.function.service;

import com.openfaas.function.entity.*;
import edu.fudan.common.util.JsonUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import java.util.*;

/**
 * @author fdse
 */
public class CancelServiceImpl implements CancelService {
    private final OkHttpClient client;
    // 构造函数名称应与类名相同
    public CancelServiceImpl() {
        // 创建 OkHttpClient 实例，并设置超时时间为 30 秒
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Map<String, Object> calculateRefund(String orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 获取订单信息
            Map<String, Object> orderResult = getOrderByIdFromOrder(orderId);
            // System.out.println("orderResult: " + orderResult);
            if (orderResult.get("status").equals(1)) {
                Order order = JsonUtils.conveterObject(orderResult.get("data"), Order.class);
                if (order.getStatus() == OrderStatus.NOTPAID.getCode() || order.getStatus() == OrderStatus.PAID.getCode()) {
                    if (order.getStatus() == OrderStatus.NOTPAID.getCode()) {
                        response.put("status", 1);
                        response.put("message", "Success. Refund 0");
                        response.put("data", 0);
                    } else {
                        response.put("status", 1);
                        response.put("message", "Success.");
                        response.put("data", calculateRefund(order));
                    }
                } else {
                    response.put("status", 0);
                    response.put("message", "Order Status Cancel Not Permitted, Refund error");
                }
            } else {
                response.put("status", 0);
                response.put("message", "Order Not Found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", 0);
            response.put("message", e.getMessage());
        }
        return response;
    }

    private String calculateRefund(Order order) {
        if (order.getStatus() == OrderStatus.NOTPAID.getCode()) {
            return "0.00";
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
            return "0";
        } else {
            double totalPrice = Double.parseDouble(order.getPrice());
            double price = totalPrice * 0.8;
            DecimalFormat priceFormat = new java.text.DecimalFormat("0.00");
            return priceFormat.format(price);
        }
    }

    private Map<String, Object> getOrderByIdFromOrder(String orderId) {
        // 检查本地是否有JAR包
        File jarFile = new File("./get-order-by-id.jar");
        if (jarFile.exists()) {
            // 如果有JAR包，运行JAR包并解析输出结果
            try {
                // 运行JAR包并获取输出结果
                String jarOutput = runJarAndGetOutput(jarFile, orderId);
                // 解析JAR包的输出结果
                return parseJarOutput(jarOutput);
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.singletonMap("status", 0);
            }
        } else {
            // 如果没有JAR包，则通过HTTP请求获取数据
            return getOrderByIdViaHttp(orderId);
        }
    }

    private String runJarAndGetOutput(File jarFile, String orderId) throws Exception {
        // 构建命令行参数
        String command = "java -jar " + jarFile.getAbsolutePath() + " " + orderId;
        // 运行命令并获取输出
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        // 读取输出结果
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");  // 添加换行符
            }
            return output.toString();
        }
    }

    public Map<String, Object> parseJarOutput(String jarOutput) {
        Map<String, Object> result = new HashMap<>();
        // System.out.println(jarOutput);

        // 提取 Response Status
        Pattern statusPattern = Pattern.compile("Response Status: (\\d+)");
        Matcher statusMatcher = statusPattern.matcher(jarOutput);
        if (statusMatcher.find()) {
            result.put("status", Integer.parseInt(statusMatcher.group(1)));
        }

        // 提取 Response Message
        Pattern messagePattern = Pattern.compile("Response Message: (.+)");
        Matcher messageMatcher = messagePattern.matcher(jarOutput);
        if (messageMatcher.find()) {
            result.put("message", messageMatcher.group(1));
        }

        // 提取 Response Data
        Pattern dataPattern = Pattern.compile("Response Data: (.+)");
        Matcher dataMatcher = dataPattern.matcher(jarOutput);
        if (dataMatcher.find()) {
            String responseDataStr = dataMatcher.group(1).trim();
            try {
                JSONObject jsonObject = new JSONObject(responseDataStr);
                result.put("data", jsonObject.toMap());
            } catch (Exception e) {
                // 如果解析失败，直接存储原始字符串
                result.put("data", responseDataStr);
            }
        }

        return result;
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
            return Collections.singletonMap("status", 0);
        }
        // System.out.println(ret);
        return JsonUtils.json2Object(ret, Map.class);
    }
}