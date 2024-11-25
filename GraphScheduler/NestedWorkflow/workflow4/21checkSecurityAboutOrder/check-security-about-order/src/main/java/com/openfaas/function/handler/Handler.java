package com.openfaas.function.handler;

import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import java.util.Date;
import java.util.List;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Handler{

    private OrderService orderService=new OrderServiceImpl();
    public mResponse Handle(Request req) {
        List<String> segmentsList = req.url().pathSegments();
	    String checkDate = segmentsList.get(segmentsList.size()-3);
        String accountId = segmentsList.get(segmentsList.size()-1);
        System.out.println("accountId: "+accountId+" checkDate: "+checkDate);
        
        mResponse mRes = orderService.checkSecurityAboutOrder(new Date(checkDate),accountId);

	    return mRes;
    }
}
