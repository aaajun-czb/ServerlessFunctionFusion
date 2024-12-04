package com.openfaas.function.service;

import java.util.Map;
import com.openfaas.function.entity.Seat;

/**
 * @author fdse
 */
public interface SeatService {
    Map<String, Object> getLeftTicketOfInterval(Seat seatRequest);
}
