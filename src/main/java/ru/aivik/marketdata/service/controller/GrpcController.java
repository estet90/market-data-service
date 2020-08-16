package ru.aivik.marketdata.service.controller;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.MarketDataServiceGrpc;
import ru.aivik.marketdata.service.service.client.ExchangeClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class GrpcController extends MarketDataServiceGrpc.MarketDataServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(GrpcController.class);

    private final Map<Integer, ExchangeClient> exchangeClientMap;

    @Inject
    public GrpcController(Set<ExchangeClient> exchangeClients) {
        this.exchangeClientMap = exchangeClients.stream()
                .collect(Collectors.toMap(ExchangeClient::getExchange, Function.identity()));
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
