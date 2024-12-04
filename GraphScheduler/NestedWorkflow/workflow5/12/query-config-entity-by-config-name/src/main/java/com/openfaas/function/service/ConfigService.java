package com.openfaas.function.service;

import java.util.Map;
import com.openfaas.function.entity.Config;


/**
 * @author fdse
 */
public interface ConfigService {
    Map<String, Object> query(String name);

}
