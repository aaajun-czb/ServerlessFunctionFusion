package com.openfaas.function.service;
import com.openfaas.function.entity.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author fdse
 */
public interface OrderService {

    Map<String, Object> queryOrders(OrderInfo qi, String accountId);

    Map<String, Object> queryOrdersForRefresh(OrderInfo qi, String accountId);

}
