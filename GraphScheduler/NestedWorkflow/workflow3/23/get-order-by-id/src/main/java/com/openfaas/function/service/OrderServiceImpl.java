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

    @Override
    public Map<String, Object> getOrderById(String orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Order order = orderRepository.findById(UUID.fromString(orderId));
            if (order == null) {
                response.put("status", 0);
                response.put("message", orderNotFound);
            } else {
                response.put("status", 1);
                response.put("message", success);
                response.put("data", order);
            }
        } catch (Exception e) {
            response.put("status", 0);
            response.put("message", e.getMessage());
        }
        return response;
    }
}