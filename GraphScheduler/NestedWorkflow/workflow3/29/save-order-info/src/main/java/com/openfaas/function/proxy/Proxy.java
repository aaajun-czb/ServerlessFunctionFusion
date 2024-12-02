package com.openfaas.function.proxy;

import com.openfaas.function.handler.Handler;
import com.openfaas.function.entity.Order;
import edu.fudan.common.util.JsonUtils;

import java.util.Map;

public class Proxy {
    public static void main(String[] args) {
        // 记录启动时间
        long startTime = System.currentTimeMillis();

        // 创建Order对象，使用默认构造函数
        Order order = new Order();

        // 创建Handler实例
        Handler handler = new Handler();

        // 调用 handle 方法
        Map<String, Object> response = handler.handle(order);

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