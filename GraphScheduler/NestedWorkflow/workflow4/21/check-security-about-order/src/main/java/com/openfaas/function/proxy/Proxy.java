package com.openfaas.function.proxy;

import com.openfaas.function.handler.Handler;

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
        if (args.length < 2) {
            System.out.println("Usage: java com.openfaas.function.proxy.Proxy <accountId> <checkDate>");
            return;
        }

        // 获取命令行参数
        String accountId = args[0];
        String checkDateStr = args[1];

        // 解析日期
        Date checkDate = parseDate(checkDateStr);
        if (checkDate == null) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd.");
            return;
        }

        // 创建Handler实例
        Handler handler = new Handler();
        // 调用 handle 方法
        Map<String, Object> response = handler.Handle(accountId, checkDate);

        // 输出响应结果
        System.out.println("Response Status: " + response.get("status"));
        System.out.println("Response Message: " + response.get("message"));
        System.out.println("Response Data: " + JsonUtils.object2Json(response.get("data")));

        // 输出执行时间
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Execution Time: " + executionTime + " ms");
    }

    private static Date parseDate(String dateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateFormat.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
}