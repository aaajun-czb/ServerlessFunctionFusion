package com.openfaas.function.service;

import com.openfaas.function.repository.OrderRepositoryImpl;
import edu.fudan.common.util.JsonUtils;
import com.openfaas.function.entity.*;
import com.openfaas.function.repository.OrderRepository;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @author fdse
 */

public class OrderServiceImpl implements OrderService {
    private OrderRepository orderRepository = new OrderRepositoryImpl();
    private OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    String success = "Success";
    String orderNotFound = "Order Not Found";

    @Override
    public Map<String, Object> queryOrders(OrderInfo qi, String accountId) {
        //1.Get all orders of the user
        ArrayList<Order> list = orderRepository.findByAccountId(UUID.fromString(accountId));
        //2.Check is these orders fit the requirement/
        if (qi.isEnableStateQuery() || qi.isEnableBoughtDateQuery() || qi.isEnableTravelDateQuery()) {
            ArrayList<Order> finalList = new ArrayList<>();
            for (Order tempOrder : list) {
                boolean statePassFlag = false;
                boolean boughtDatePassFlag = false;
                boolean travelDatePassFlag = false;
                //3.Check order state requirement.
                if (qi.isEnableStateQuery()) {
                    if (tempOrder.getStatus() != qi.getState()) {
                        statePassFlag = false;
                    } else {
                        statePassFlag = true;
                    }
                } else {
                    statePassFlag = true;
                }
                //4.Check order travel date requirement.
                if (qi.isEnableTravelDateQuery()) {
                    if (tempOrder.getTravelDate().before(qi.getTravelDateEnd()) &&
                            tempOrder.getTravelDate().after(qi.getBoughtDateStart())) {
                        travelDatePassFlag = true;
                    } else {
                        travelDatePassFlag = false;
                    }
                } else {
                    travelDatePassFlag = true;
                }
                //5.Check order bought date requirement.
                if (qi.isEnableBoughtDateQuery()) {
                    if (tempOrder.getBoughtDate().before(qi.getBoughtDateEnd()) &&
                            tempOrder.getBoughtDate().after(qi.getBoughtDateStart())) {
                        boughtDatePassFlag = true;
                    } else {
                        boughtDatePassFlag = false;
                    }
                } else {
                    boughtDatePassFlag = true;
                }
                //6.check if all requirement fits.
                if (statePassFlag && boughtDatePassFlag && travelDatePassFlag) {
                    finalList.add(tempOrder);
                }
            }
            return Collections.singletonMap("data", finalList);
        } else {
            return Collections.singletonMap("data", list);
        }
    }

    @Override
    public Map<String, Object> queryOrdersForRefresh(OrderInfo qi, String accountId) {
        Map<String, Object> response = queryOrders(qi, accountId);
        ArrayList<Order> orders = JsonUtils.conveterObject(response.get("data"), ArrayList.class);
        ArrayList<String> stationIds = new ArrayList<>();
        ArrayList<Order> result = new ArrayList<>();

        for (int i = 0; i < orders.size(); i++) {
            Order order = JsonUtils.conveterObject(orders.get(i), Order.class);
            result.add(order);
            stationIds.add(order.getFrom());
            stationIds.add(order.getTo());
        }

        Map<String, Object> stationResponse = queryForStationId(stationIds);
        // System.out.println(stationResponse);
        List<String> names = (List<String>) stationResponse.get("data");
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setFrom(names.get(i * 2));
            result.get(i).setTo(names.get(i * 2 + 1));
        }
        return Collections.singletonMap("data", result);
    }

    public Map<String, Object> queryForStationId(List<String> ids) {
        // 检查本地是否有JAR包
        File jarFile = new File("./get-stationid-list-by-name-list.jar");
        if (jarFile.exists()) {
            // 如果有JAR包，运行JAR包并解析输出结果
            try {
                // 运行JAR包并获取输出结果
                String jarOutput = runJarAndGetOutput(jarFile, ids);
                // System.out.println(jarOutput);

                // 解析JAR包的输出结果
                Map<String, Object> mRes = parseJarOutput(jarOutput);
                // System.out.println(mRes);
                if (mRes == null || mRes.get("data") == null) {
                    return Collections.singletonMap("data", Collections.emptyList());
                }

                // 直接提取 data 字段
                List<String> resList = (List<String>) mRes.get("data");
                return Collections.singletonMap("data", resList);
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.singletonMap("data", Collections.emptyList());
            }
        } else {
            // 如果没有JAR包，则通过HTTP请求获取数据
            return queryForStationIdViaHttp(ids);
        }
    }

    private static String buildCommand(String jarFilePath, String json) throws Exception {
        // 使用 Jackson 将 JSON 字符串解析为 List<String>
        ObjectMapper mapper = new ObjectMapper();
        List<String> idList = mapper.readValue(json, List.class);

        // 构建命令行参数
        StringBuilder commandBuilder = new StringBuilder("java -jar ").append(jarFilePath);
        for (String id : idList) {
            commandBuilder.append(" ").append(id);
        }

        return commandBuilder.toString();
    }

    private String runJarAndGetOutput(File jarFile, List<String> ids) throws Exception {
        // 构建命令行参数
        String json = mapper.writeValueAsString(ids);
        String command = buildCommand(jarFile.getAbsolutePath(), json);
        // System.out.println(command);

        // 运行命令并获取输出
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();

        // 读取输出结果
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            return output.toString();
        }
    }

    private Map<String, Object> parseJarOutput(String jarOutput) {
        Map<String, Object> result = new HashMap<>();

        // 使用正则表达式提取各部分内容
        Pattern pattern = Pattern.compile("Response Status: (\\d+)\\s*Response Message: (\\w+)\\s*Response Data: (.*)");
        Matcher matcher = pattern.matcher(jarOutput);

        if (matcher.find()) {
            result.put("status", Integer.parseInt(matcher.group(1)));
            result.put("message", matcher.group(2));
            try {
                List<String> data = mapper.readValue(matcher.group(3), List.class);
                result.put("data", data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public Map<String, Object> queryForStationIdViaHttp(List<String> ids) {
        String ret = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(ids);

            String requestBody = "{\"container_name\": \"44\", \"jar_name\": \"get-stationid-list-by-name-list.jar\",\"data\": " + json + "}";

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
            return Collections.singletonMap("data", Collections.emptyList());
        }

        try {
            Map<String, Object> mRes = JsonUtils.json2Object(ret, Map.class);
            if (mRes == null || mRes.get("data") == null) {
                return Collections.singletonMap("data", Collections.emptyList());
            }

            // 直接提取 data 字段
            List<String> resList = (List<String>) mRes.get("data");
            return Collections.singletonMap("data", resList);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonMap("data", Collections.emptyList());
        }
    }
}

