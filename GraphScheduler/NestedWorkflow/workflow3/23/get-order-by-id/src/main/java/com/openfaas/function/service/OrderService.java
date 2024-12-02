package com.openfaas.function.service;
import com.openfaas.function.entity.*;

import java.util.Map;

/**
 * @author fdse
 */
public interface OrderService {

    Map<String, Object> getOrderById(String orderId);

}
