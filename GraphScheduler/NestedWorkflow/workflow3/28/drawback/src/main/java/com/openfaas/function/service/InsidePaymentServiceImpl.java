package com.openfaas.function.service;

import com.openfaas.function.repository.AddMoneyRepositoryImpl;
import com.openfaas.function.entity.*;
import com.openfaas.function.repository.AddMoneyRepository;

import java.util.Map;
import java.util.*;

/**
 * @author fdse
 */
public class InsidePaymentServiceImpl implements InsidePaymentService {
    public AddMoneyRepository addMoneyRepository = new AddMoneyRepositoryImpl();

    @Override
    public Map<String, Object> drawBack(String userId, double money) {
        Map<String, Object> response = new HashMap<>();

        if (addMoneyRepository.findByUserId(userId) != null) {
            // Money addMoney = new Money();
            // addMoney.setUserId(userId);
            // addMoney.setMoney(String.valueOf(money));
            // addMoney.setType(MoneyType.D);
            // addMoneyRepository.save(addMoney);
            // 模拟数据库插入操作
            simulateDatabaseInsert();

            response.put("status", 1);
            response.put("message", "Draw Back Money Success");
            response.put("data", null);
        } else {
            response.put("status", 0);
            response.put("message", "Draw Back Money Failed");
            response.put("data", null);
        }

        return response;
    }

    private void simulateDatabaseInsert() {
        // 随机生成50-100ms的睡眠时间
        Random random = new Random();
        int sleepTime = random.nextInt(51) + 50; // 50-100ms
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}