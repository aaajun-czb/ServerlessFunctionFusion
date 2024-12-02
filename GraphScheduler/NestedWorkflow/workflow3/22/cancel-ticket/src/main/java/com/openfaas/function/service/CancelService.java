package com.openfaas.function.service;

import java.util.Map;

/**
 * @author fdse
 */
public interface CancelService {
    Map<String, Object> cancelOrder(String orderId, String loginId);

}
