package com.openfaas.function.service;

import com.openfaas.function.repository.OrderRepositoryImpl;
import com.openfaas.function.entity.Order;
import com.openfaas.function.repository.OrderRepository;

import java.util.HashMap;
import java.util.Map;

public class OrderServiceImpl implements OrderService {

    private OrderRepository orderRepository = new OrderRepositoryImpl();

    private static final String SUCCESS = "Success";
    private static final String ORDER_NOT_FOUND = "Order Not Found";

    @Override
    public Map<String, Object> saveChanges(Order order) {
        Map<String, Object> response = new HashMap<>();

        Order oldOrder = orderRepository.findById(order.getId());
        if (oldOrder == null) {
            response.put("status", 0);
            response.put("message", ORDER_NOT_FOUND);
            response.put("data", null);
        } else {
            oldOrder.setAccountId(order.getAccountId());
            oldOrder.setBoughtDate(order.getBoughtDate());
            oldOrder.setTravelDate(order.getTravelDate());
            oldOrder.setTravelTime(order.getTravelTime());
            oldOrder.setCoachNumber(order.getCoachNumber());
            oldOrder.setSeatClass(order.getSeatClass());
            oldOrder.setSeatNumber(order.getSeatNumber());
            oldOrder.setFrom(order.getFrom());
            oldOrder.setTo(order.getTo());
            oldOrder.setStatus(order.getStatus());
            oldOrder.setTrainNumber(order.getTrainNumber());
            oldOrder.setPrice(order.getPrice());
            oldOrder.setContactsName(order.getContactsName());
            oldOrder.setContactsDocumentNumber(order.getContactsDocumentNumber());
            oldOrder.setDocumentType(order.getDocumentType());

            orderRepository.deleteById(order.getId());
            orderRepository.save(oldOrder);

            response.put("status", 1);
            response.put("message", SUCCESS);
            response.put("data", null);
        }

        return response;
    }
}