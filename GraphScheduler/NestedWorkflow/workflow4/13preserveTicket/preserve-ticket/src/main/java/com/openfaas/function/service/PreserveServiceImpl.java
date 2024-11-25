package com.openfaas.function.service;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import com.openfaas.function.entity.*;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

import java.util.Date;
import java.util.UUID;

/**
 * @author fdse
 */

public class PreserveServiceImpl implements PreserveService {

    private OkHttpClient client = new OkHttpClient();
    
    public static String function14_URI = "gateway.openfaas:8080/function/check-security.openfaas-fn";
	public static void updateFunctionURI(String _function14_URI){
		function14_URI = _function14_URI;
	}

    @Override
    public mResponse preserve(OrderTicketsInfo oti) {

        //1.detect ticket scalper

        mResponse result = checkSecurity(oti.getAccountId());
        if (result.getStatus() == 0) {
            return new mResponse<>(0, result.getMsg(), null);
        }else{
            return new mResponse<>(1, result.getMsg(), null);
        }
    }

    private mResponse checkSecurity(String accountId) {
        String ret = "";
        try {
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("http://" + function14_URI + "/accountId/" + accountId)
                    .get()
                    .build();

            okhttp3.Response response = client.newCall(request).execute();
            ret = response.body().string();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return JsonUtils.json2Object(ret, mResponse.class);
    }

}
