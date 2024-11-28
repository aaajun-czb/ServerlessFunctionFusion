package com.openfaas.function.service;

import java.util.Map;

/**
 * @author fdse
 */
public interface CancelService {
    /**
     * calculate refund by login id
     *
     * @param orderId order id
     * @return Response
     */
    Map<String, Object> calculateRefund(String orderId);

}
