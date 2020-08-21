package ru.aivik.marketdata.service.controller;

import com.google.protobuf.ByteString;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.MarketDataServiceGrpc;
import ru.aivik.marketdata.service.dto.TradeSubscriber;
import ru.aivik.marketdata.service.service.aggregator.TradeDataAggregator;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
        var point = "GrpcController.getHistoryBarsAndSubscribeTrades";
        var trades = new LinkedBlockingQueue<MarketData.Trade>();
        var executor = Executors.newScheduledThreadPool(1);
        var context = Context.current();
        var future = executor.scheduleWithFixedDelay(() -> {
            if (!context.isCancelled()) {
                while (!trades.isEmpty()) {
                    var trade = trades.poll();
                    var response = MarketData.GetTradesResponse.newBuilder()
                            .setTrade(trade)
                            .build();
                    responseObserver.onNext(response);
                    logger.debug("{}.out\nresponse=[{}]", point, response.toString());
                }
            }
        }, 0, 10, TimeUnit.MILLISECONDS);
        try {
            var exchange = request.getExchange();
            MDC.put("exchange", String.valueOf(exchange));
            MDC.put("requestId", UUID.randomUUID().toString());
            logger.info("{}.in\nrequest=[{}]", point, request.toString());
            var subscribers = request.getInstrumentList().asByteStringList().stream()
                    .map(ByteString::toStringUtf8)
                    .map(instrument -> buildTradeSubscriber(exchange, trades, instrument))
                    .collect(Collectors.toSet());
            try (var ignored = tradeDataAggregator.aggregate(subscribers, exchange)) {
                future.get();
            } catch (IOException | InterruptedException | ExecutionException e) {
                logger.error("{}.thrown", point, e);
            }
        } finally {
            responseObserver.onCompleted();
            MDC.clear();
        }
    }

    @NotNull
    private TradeSubscriber buildTradeSubscriber(MarketData.GetTradesRequest.Exchange exchange, LinkedBlockingQueue<MarketData.Trade> trades, String instrument) {
        return new TradeSubscriber(
                exchange,
                instrument,
                UUID.randomUUID().toString(),
                trades
        );
    }

}
