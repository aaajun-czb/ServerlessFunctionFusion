package com.openfaas.function.handler;

import com.openfaas.function.entity.Seat;
import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;

import java.util.Map;

public class Handler {
    private OrderService orderService = new OrderServiceImpl();

    public Map<String, Object> Handle(Seat seat) {
        return orderService.getSoldTickets(seat);
    }
}