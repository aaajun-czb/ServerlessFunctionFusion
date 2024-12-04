package com.openfaas.function.handler;

import com.openfaas.function.entity.Seat;
import com.openfaas.function.service.SeatService;
import com.openfaas.function.service.SeatServiceImpl;

import java.util.Map;

public class Handler {
    private SeatService seatService = new SeatServiceImpl();

    public Map<String, Object> Handle(Seat seatRequest) {
        return seatService.getLeftTicketOfInterval(seatRequest);
    }
}