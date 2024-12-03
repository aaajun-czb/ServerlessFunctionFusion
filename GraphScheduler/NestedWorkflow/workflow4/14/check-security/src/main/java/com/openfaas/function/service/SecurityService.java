package com.openfaas.function.service;

import java.util.Map;
import com.openfaas.function.entity.*;

/**
 * @author fdse
 */
public interface SecurityService {

    Map<String, Object> check(String accountId);

}
