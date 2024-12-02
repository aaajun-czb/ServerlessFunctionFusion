package com.openfaas.function.service;

import edu.fudan.common.util.mResponse;
import com.openfaas.function.entity.*;

import java.util.Date;
import java.util.UUID;
import java.util.Map;

/**
 * @author fdse
 */
public interface OrderService {

    Map<String, Object> saveChanges(Order order);

}
