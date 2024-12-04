package com.openfaas.function.service;

import com.openfaas.function.entity.LeftTicketInfo;
import com.openfaas.function.entity.Order;
import com.openfaas.function.entity.Seat;
import com.openfaas.function.entity.Ticket;
import com.openfaas.function.repository.OrderRepository;
import com.openfaas.function.repository.OrderRepositoryImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrderServiceImpl implements OrderService {

    private OrderRepository orderRepository = new OrderRepositoryImpl();

    String success = "Success";
    String orderNotFound = "Order Not Found";

    @Override
    public Map<String, Object> getSoldTickets(Seat seatRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            ArrayList<Order> list = orderRepository.findByTravelDateAndTrainNumber(seatRequest.getTravelDate(),
                    seatRequest.getTrainNumber());
            if (list != null && !list.isEmpty()) {
                Set<Ticket> ticketSet = new HashSet<>();
                for (Order tempOrder : list) {
                    ticketSet.add(new Ticket(tempOrder.getSeatNumber(),
                            tempOrder.getFrom(), tempOrder.getTo()));
                }
                LeftTicketInfo leftTicketInfo = new LeftTicketInfo();
                leftTicketInfo.setSoldTickets(ticketSet);
                response.put("status", 1);
                response.put("message", success);
                response.put("data", leftTicketInfo);
            } else {
                response.put("status", 0);
                response.put("message", orderNotFound);
                response.put("data", null);
            }
        } catch (Exception e) {
            response.put("status", 0);
            response.put("message", e.getMessage());
            response.put("data", null);
        }
        return response;
    }
}