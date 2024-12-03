package com.openfaas.function.handler;

import com.openfaas.function.service.SecurityService;
import com.openfaas.function.service.SecurityServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class Handler {
    private SecurityService securityService = new SecurityServiceImpl();

    public Map<String, Object> Handle(String accountId) {
        Map<String, Object> response = securityService.check(accountId);
        return response;
    }
}