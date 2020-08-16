package ru.aivik.marketdata.service;

import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.MarketDataServiceGrpc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) throws InterruptedException {
        var channel = ManagedChannelBuilder.forTarget("localhost:8080")
                .usePlaintext()
                .build();
        var request = MarketData.GetTradesRequest.newBuilder()
                .setExchange(1)
                .addInstrument("btcusdt")
                .build();

        var streamApi = MarketDataServiceGrpc.newStub(channel);
        var responseObserver = new ResponseObserver();
        streamApi.getHistoryBarsAndSubscribeTrades(request, responseObserver);
        while (true) {
            while (!responseObserver.responses.isEmpty()) {
                var response = responseObserver.responses.take();
                logger.info("extract response: {}", response.toString());
            }
            responseObserver.onCompleted();
            Thread.sleep(5000);
        }
    }

    private static class ResponseObserver implements StreamObserver<MarketData.GetTradesResponse> {

        private final BlockingQueue<MarketData.GetTradesResponse> responses = new LinkedBlockingQueue<>();

        @Override
        public void onNext(MarketData.GetTradesResponse value) {
            responses.offer(value);
            logger.info("onNext");
        }

        @Override
        public void onError(Throwable t) {
            logger.error(t.getMessage());
        }

        @Override
        public void onCompleted() {
            logger.info("onCompleted");
        }
    }

}
