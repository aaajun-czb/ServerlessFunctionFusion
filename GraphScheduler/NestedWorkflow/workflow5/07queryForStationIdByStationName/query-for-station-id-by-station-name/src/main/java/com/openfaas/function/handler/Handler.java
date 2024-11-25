package com.openfaas.function.handler;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.openfaas.function.service.StationService;
import com.openfaas.function.service.StationServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import java.util.List;

import org.bson.Document;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * FINISHED
 * function7
 * query-stationID-by-stationName
 * Http Method : GET
 * <p>
 * 原API地址："http://ts-station-service:12345/api/v1/stationservice/stations/id/" + stationName
 * <p>
 * 输入：(String)stationName
 * 输出：(String)stationID
 */

public class Handler{

    private StationService stationService = new StationServiceImpl();

    public mResponse Handle(Request req) {
        System.out.println("start handle");

    	//    System.out.println("start");
        // MongoClient mongoClient = MongoClients.create("mongodb://ts-station-mongo.default:27017");
        // MongoDatabase database = mongoClient.getDatabase("ts");
        // MongoCollection<Document> collection = database.getCollection("station");
        // System.out.println("success");

        List<String> segmentsList = req.url().pathSegments();
	    String stationName = segmentsList.get(segmentsList.size()-1);
        
        mResponse mRes = stationService.queryForId(stationName);

        return mRes;
    }
}
