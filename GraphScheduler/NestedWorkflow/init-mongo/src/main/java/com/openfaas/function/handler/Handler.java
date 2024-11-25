package com.openfaas.function.handler;

import com.openfaas.function.repository.AddMoneyRepository;
import com.openfaas.function.repository.AddMoneyRepositoryImpl;
import com.openfaas.function.repository.ConfigRepository;
import com.openfaas.function.repository.ConfigRepositoryImpl;
import com.openfaas.function.repository.ContactsRepository;
import com.openfaas.function.repository.ContactsRepositoryImpl;
import com.openfaas.function.repository.OrderRepository;
import com.openfaas.function.repository.OrderRepositoryImpl;
import com.openfaas.function.repository.PaymentRepository;
import com.openfaas.function.repository.PaymentRepositoryImpl;
import com.openfaas.function.repository.RouteRepository;
import com.openfaas.function.repository.RouteRepositoryImpl;
import com.openfaas.function.repository.SecurityRepository;
import com.openfaas.function.repository.SecurityRepositoryImpl;
import com.openfaas.function.repository.StationRepository;
import com.openfaas.function.repository.StationRepositoryImpl;
import com.openfaas.function.repository.TrainTypeRepository;
import com.openfaas.function.repository.TrainTypeRepositoryImpl;
import com.openfaas.function.repository.TripRepository;
import com.openfaas.function.repository.TripRepositoryImpl;

import edu.fudan.common.util.mResponse;
import okhttp3.Request;

public class Handler {
    // init-config-mongo
    private ConfigRepository configRepository = new ConfigRepositoryImpl();
    // init-contracts-mongo
    private ContactsRepository contactsRepository = new ContactsRepositoryImpl();

    
    // init-inside-payment-mongo
    private AddMoneyRepository addMoneyRepository = new AddMoneyRepositoryImpl();
    private PaymentRepository paymentRepository = new PaymentRepositoryImpl();
    // init-order-mongo
    private OrderRepository orderRepository = new OrderRepositoryImpl();
    // init-payment-mongo仅在25createThirdPartyPaymentAndPay中用到
    // init-security-mongo
    private SecurityRepository securityRepository = new SecurityRepositoryImpl();
    // init-station-mongo
    private StationRepository stationRepository = new StationRepositoryImpl();

    // init-route-mongo
    private RouteRepository routeRepository = new RouteRepositoryImpl();
    // init-train-mongo
    private TrainTypeRepository trainTypeRepository = new TrainTypeRepositoryImpl();
    // init-travel-mongo
    private TripRepository tripRepository = new TripRepositoryImpl();
    public mResponse Handle(Request req) {
        mResponse res = new mResponse();

        boolean flag = true;
        // init-station-mongo
        flag = stationRepository.init() && flag;
        // init-config-mongo
        flag = configRepository.init() && flag;
        // init-contracts-mongo
        flag = contactsRepository.init() && flag;
        // init-inside-payment-mongo
        flag = addMoneyRepository.init() && paymentRepository.init() && flag;
        // init-order-mongo
        flag = orderRepository.init() && flag;
        // init-security-mongo
        flag = securityRepository.init() && flag;

        // init-route-mongo
        flag = routeRepository.init() && flag;
        // init-train-mongo
        flag = trainTypeRepository.init() && flag;
        // init-travel-mongo
        flag = tripRepository.init() && flag;

        if (flag) {
            res.setStatus(1);
        }else {
            res.setStatus(0);
        }

        return res;
    }
}
