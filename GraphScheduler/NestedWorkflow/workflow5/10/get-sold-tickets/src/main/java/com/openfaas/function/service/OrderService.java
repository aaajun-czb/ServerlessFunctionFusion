package com.openfaas.function.service;

import java.util.Map;
import com.openfaas.function.entity.*;

/**
 * @author fdse
 */
public interface OrderService {

    Map<String, Object> getSoldTickets(Seat seatRequest);

}
