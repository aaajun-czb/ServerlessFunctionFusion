package com.openfaas.function.handler;

import com.openfaas.function.service.InsidePaymentService;
import com.openfaas.function.service.InsidePaymentServiceImpl;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

public class Handler{
    private InsidePaymentService insidePaymentService = new InsidePaymentServiceImpl();

    public mResponse Handle(Request req) {
        List<String> segmentsList = req.url().pathSegments();
        String userId = segmentsList.get(segmentsList.size()-3);
        String money = segmentsList.get(segmentsList.size()-1);
        System.out.println("userId: " + userId + " money: " + money);

        mResponse mRes = insidePaymentService.drawBack(userId,money);

	    return mRes;
    }
}
