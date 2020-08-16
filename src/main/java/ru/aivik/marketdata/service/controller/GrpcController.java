package ru.aivik.marketdata.service.controller;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.MarketDataServiceGrpc;
import ru.aivik.marketdata.service.service.client.ExchangeClient;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class GrpcController extends MarketDataServiceGrpc.MarketDataServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(GrpcController.class);

    private final Map<Integer, ExchangeClient> exchangeClientMap;

    public GrpcController(Map<Integer, ExchangeClient> exchangeClientMap) {
        this.exchangeClientMap = exchangeClientMap;
    }

    @Override
    public void getHistoryBarsAndSubscribeTrades(MarketData.GetTradesRequest request,
                                                 StreamObserver<MarketData.GetTradesResponse> responseObserver) {
        try {
            MDC.put("requestId", UUID.randomUUID().toString());
            MDC.put("exchange", String.valueOf(request.getExchange()));
            var point = "GrpcController.getHistoryBarsAndSubscribeTrades";
            logger.info("{}.in\n\trequest=[{}]", point, request.toString());
            var instruments = request.getInstrumentList().asByteStringList();
            var trades = new LinkedBlockingQueue<MarketData.Trade>();
            var client = exchangeClientMap.get(request.getExchange());
            try (var ignored = client.subscribeToAggTradeEvent(instruments, trades)) {
                while (!Context.current().isCancelled()) {
                    if (!trades.isEmpty()) {
                        var trade = trades.poll();
                        var response = MarketData.GetTradesResponse.newBuilder()
                                .setTrade(trade)
                                .build();
                        responseObserver.onNext(response);
                        logger.info("{}.out response=[{}]", point, response.toString());
                    }
                }
            } catch (IOException e) {
                logger.error("{}.thrown", point, e);
            }
        } finally {
            responseObserver.onCompleted();
            MDC.clear();
        }
    }

}
