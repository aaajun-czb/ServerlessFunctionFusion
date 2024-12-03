package com.openfaas.function.service;

import java.util.Map;
import com.openfaas.function.entity.OrderTicketsInfo;

/**
 * @author fdse
 */
public interface PreserveService {

    Map<String, Object> preserve(OrderTicketsInfo oti);
}
