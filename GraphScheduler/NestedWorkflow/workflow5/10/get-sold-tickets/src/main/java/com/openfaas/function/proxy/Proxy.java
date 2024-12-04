package com.openfaas.function.proxy;

import com.openfaas.function.handler.Handler;
import com.openfaas.function.entity.Seat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Date;

import edu.fudan.common.util.JsonUtils;

public class Proxy {

    public static void main(String[] args) {
        // 记录启动时间
        long startTime = System.currentTimeMillis();

        // 检查命令行参数
        if (args.length < 5) {
            System.out.println("Usage: java com.openfaas.function.proxy.Proxy <travelDate> <trainNumber> <startStation> <destStation> <seatType>");
            return;
        }

        // 获取命令行参数
        String travelDateStr = args[0];
        String trainNumber = args[1];
        String startStation = args[2];
        String destStation = args[3];
        int seatType = Integer.parseInt(args[4]);

        // 将 travelDate 转换为 Date 对象
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date travelDate;
        try {
            travelDate = dateFormat.parse(travelDateStr);
        } catch (ParseException e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd HH:mm:ss.");
            return;
        }

        // 创建 Seat 对象
        Seat seat = new Seat();
        seat.setTravelDate(travelDate);
        seat.setTrainNumber(trainNumber);
        seat.setStartStation(startStation);
        seat.setDestStation(destStation);
        seat.setSeatType(seatType);

        // 创建Handler实例
        Handler handler = new Handler();

        // 调用 handle 方法
        Map<String, Object> response = handler.Handle(seat);

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