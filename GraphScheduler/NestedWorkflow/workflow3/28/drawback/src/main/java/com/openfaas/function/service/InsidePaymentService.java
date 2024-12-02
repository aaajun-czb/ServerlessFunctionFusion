package com.openfaas.function.service;

import java.util.Map;

/**
 * @author Administrator
 * @date 2017/6/20.
 */
public interface InsidePaymentService {
    Map<String, Object> drawBack(String userId, double money);

}
