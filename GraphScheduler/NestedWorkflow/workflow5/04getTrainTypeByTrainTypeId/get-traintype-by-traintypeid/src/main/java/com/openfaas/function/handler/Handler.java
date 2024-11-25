package com.openfaas.function.handler;

import com.openfaas.function.entity.TrainType;
import com.openfaas.function.service.TrainService;
import com.openfaas.function.service.TrainServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * function4 queryTrainType
 * FINISHED/UNTESTED
 * <p>
 * get train's info by id
 * Http Method : GET
 * <p>
 * 原API地址："http://ts-train-service:14567/api/v1/trainservice/trains/" + trainTypeId
 * <p>
 * 输入：(String)trainTypeId
 * 输出：(Object)TrainType(查不到的的话返回输入的ID)
 */


public class Handler{
    private TrainService trainService = new TrainServiceImpl();

    public mResponse Handle(Request req) {
        System.out.println("start handle");
        List<String> segmentsList = req.url().pathSegments();
        String trainTypeId = segmentsList.get(segmentsList.size()-1);

        TrainType trainType = trainService.retrieve(trainTypeId);
        mResponse mRes;

        if (trainType == null) {
            mRes=new mResponse(0, "here is no TrainType with the trainType id", trainTypeId);
        } else {
            mRes=new mResponse(1, "success", trainType);
        }

        return mRes;
    }
}
