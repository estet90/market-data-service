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
import ru.aivik.marketdata.service.error.exception.ApplicationException;
import ru.aivik.marketdata.service.service.aggregator.TradeDataAggregator;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static ru.aivik.marketdata.service.error.exception.ApplicationException.Type.Execution;

public class GrpcController extends MarketDataServiceGrpc.MarketDataServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(GrpcController.class);

    private final TradeDataAggregator tradeDataAggregator;
    private final ScheduledExecutorService executor;

    public GrpcController(TradeDataAggregator tradeDataAggregator) {
        this.tradeDataAggregator = tradeDataAggregator;
        this.executor = Executors.newScheduledThreadPool(10);
    }

    @Override
    public void getHistoryBarsAndSubscribeTrades(MarketData.GetTradesRequest request,
                                                 StreamObserver<MarketData.GetTradesResponse> responseObserver) {
        var point = "GrpcController.getHistoryBarsAndSubscribeTrades";
        var trades = new LinkedBlockingQueue<MarketData.Trade>();
        var context = Context.current();
        var future = createFuture(responseObserver, point, trades, context);
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
            } catch (IOException e) {
                future.cancel(true);
                logger.error("{}.thrown", point, e);
            } catch (InterruptedException | ExecutionException e) {
                future.cancel(true);
                if (e.getCause() instanceof ApplicationException applicationException && Execution.equals(applicationException.getType())) {
                    logger.info("{}.out", point);
                } else {
                    logger.error("{}.thrown", point, e);
                }
            }
        } finally {
            responseObserver.onCompleted();
            MDC.clear();
        }
    }

    private ScheduledFuture<?> createFuture(StreamObserver<MarketData.GetTradesResponse> responseObserver, String point, LinkedBlockingQueue<MarketData.Trade> trades, Context context) {
        return executor.scheduleWithFixedDelay(() -> {
            if (!context.isCancelled()) {
                while (!trades.isEmpty()) {
                    var trade = trades.poll();
                    var response = MarketData.GetTradesResponse.newBuilder()
                            .setTrade(trade)
                            .build();
                    responseObserver.onNext(response);
                    logger.debug("{}.out\nresponse=[{}]", point, response.toString());
                }
            } else {
                throw new ApplicationException("Обработка завершена", Execution);
            }
        }, 0, 10, TimeUnit.MILLISECONDS);
    }

    private TradeSubscriber buildTradeSubscriber(MarketData.GetTradesRequest.Exchange exchange, LinkedBlockingQueue<MarketData.Trade> trades, String instrument) {
        return new TradeSubscriber(
                exchange,
                instrument,
                UUID.randomUUID().toString(),
                trades
        );
    }

}
