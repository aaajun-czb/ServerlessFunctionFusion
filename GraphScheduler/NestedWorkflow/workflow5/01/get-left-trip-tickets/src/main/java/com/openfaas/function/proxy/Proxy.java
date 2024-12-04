package com.openfaas.function.proxy;

import com.openfaas.function.handler.Handler;
import com.openfaas.function.entity.TripInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import edu.fudan.common.util.JsonUtils;

public class Proxy {

    public static void main(String[] args) {
        // 记录启动时间
        long startTime = System.currentTimeMillis();

        // 检查命令行参数
        if (args.length < 3) {
            System.out.println("Usage: java com.openfaas.function.proxy.Proxy <startingPlace> <endPlace> <departureTime>");
            return;
        }

        // 获取命令行参数
        String startingPlace = args[0];
        String endPlace = args[1];
        String departureTimeStr = args[2];

        // 将 departureTime 转换为 Date 对象
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date departureTime;
        try {
            departureTime = dateFormat.parse(departureTimeStr);
        } catch (ParseException e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd HH:mm:ss.");
            return;
        }

        // 创建 TripInfo 对象
        TripInfo tripInfo = new TripInfo();
        tripInfo.setStartingPlace(startingPlace);
        tripInfo.setEndPlace(endPlace);
        tripInfo.setDepartureTime(departureTime);

        // 创建Handler实例
        Handler handler = new Handler();

        // 调用 handle 方法
        Map<String, Object> response = handler.Handle(tripInfo);

        // 输出响应结果
        System.out.println("Response Status: " + response.get("status"));
        System.out.println("Response Message: " + response.get("message"));
        System.out.println("Response Data: " + JsonUtils.object2Json(response.get("data")));

        // 输出执行时间
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Execution Time: " + executionTime + " ms");
    }
}