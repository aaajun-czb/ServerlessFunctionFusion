package com.openfaas.function.service;

import com.openfaas.function.repository.OrderRepositoryImpl;
import com.openfaas.function.entity.*;
import com.openfaas.function.repository.OrderRepository;

import java.util.*;

/**
 * @author fdse
 */

public class OrderServiceImpl implements OrderService {

    private OrderRepository orderRepository = new OrderRepositoryImpl();

    String success = "Success";
    String orderNotFound = "Order Not Found";

    // 获取dateFrom-1小时之后的订单数量和已付款数量
    @Override
    public Map<String, Object> checkSecurityAboutOrder(Date dateFrom, String accountId) {
        OrderSecurity result = new OrderSecurity();
        ArrayList<Order> orders = orderRepository.findByAccountId(UUID.fromString(accountId));
        int countOrderInOneHour = 0;
        int countTotalValidOrder = 0;
        Calendar ca = Calendar.getInstance();
        ca.setTime(dateFrom);
        ca.add(Calendar.HOUR_OF_DAY, -1);
        dateFrom = ca.getTime();
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.NOTPAID.getCode() ||
                    order.getStatus() == OrderStatus.PAID.getCode() ||
                    order.getStatus() == OrderStatus.COLLECTED.getCode()) {
                countTotalValidOrder += 1;
            }
            if (order.getBoughtDate().after(dateFrom)) {
                countOrderInOneHour += 1;
            }
        }
        result.setOrderNumInLastOneHour(countOrderInOneHour);
        result.setOrderNumOfValidOrder(countTotalValidOrder);

        // 构建返回的Map
        Map<String, Object> response = new HashMap<>();
        response.put("status", 1);
        response.put("message", "Check Security Success");
        response.put("data", result);

        return response;
    }
}