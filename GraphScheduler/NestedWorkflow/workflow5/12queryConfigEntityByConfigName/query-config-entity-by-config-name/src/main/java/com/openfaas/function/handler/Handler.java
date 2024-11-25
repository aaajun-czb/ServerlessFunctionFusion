package com.openfaas.function.handler;

import com.openfaas.function.service.ConfigService;
import com.openfaas.function.service.ConfigServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * function12 queryConfigEntity
 *
 * query config entity by configName
 * Http Method : GET
 * <p>
 * 原API地址："http://ts-config-service:15679/api/v1/configservice/configs/" + configName
 * <p>
 * 输入：(String)configName
 * 输出：(Object)Config
 */

public class Handler{

    private ConfigService configService = new ConfigServiceImpl();

    public mResponse Handle(Request req) {
        System.out.println("start handle");
        List<String> segmentsList = req.url().pathSegments();
	    String configName = segmentsList.get(segmentsList.size()-1);
        
        mResponse mRes = configService.query(configName);

	    return mRes;
    }
}
