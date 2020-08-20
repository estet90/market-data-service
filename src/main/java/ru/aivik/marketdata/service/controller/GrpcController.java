package ru.aivik.marketdata.service.controller;

import com.google.protobuf.ByteString;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.MarketDataServiceGrpc;
import ru.aivik.marketdata.service.dto.TradeSubscriber;
import ru.aivik.marketdata.service.service.aggregator.TradeDataAggregator;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class GrpcController extends MarketDataServiceGrpc.MarketDataServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(GrpcController.class);

    private final TradeDataAggregator tradeDataAggregator;

    public GrpcController(TradeDataAggregator tradeDataAggregator) {
        this.tradeDataAggregator = tradeDataAggregator;
    }

    @Override
    public void getHistoryBarsAndSubscribeTrades(MarketData.GetTradesRequest request,
                                                 StreamObserver<MarketData.GetTradesResponse> responseObserver) {
        try {
            MDC.put("requestId", UUID.randomUUID().toString());
            var exchange = request.getExchange();
            MDC.put("exchange", String.valueOf(exchange));
            var point = "GrpcController.getHistoryBarsAndSubscribeTrades";
            logger.info("{}.in\nrequest=[{}]", point, request.toString());
            var trades = new LinkedBlockingQueue<MarketData.Trade>();
            var subscribers = request.getInstrumentList().asByteStringList().stream()
                    .map(ByteString::toStringUtf8)
                    .map(instrument -> new TradeSubscriber(
                            exchange,
                            instrument,
                            UUID.randomUUID().toString(),
                            trades
                    ))
                    .collect(Collectors.toList());
            try (var ignored = tradeDataAggregator.aggregate(subscribers, exchange)) {
                while (!Context.current().isCancelled()) {
                    if (!trades.isEmpty()) {
                        var trade = trades.poll();
                        var response = MarketData.GetTradesResponse.newBuilder()
                                .setTrade(trade)
                                .build();
                        responseObserver.onNext(response);
                        logger.info("{}.out\nresponse=[{}]", point, response.toString());
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
