package ru.aivik.marketdata.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aivik.marketdata.service.controller.GrpcController;
import ru.aivik.marketdata.service.module.ExchangeClientModule;
import ru.aivik.marketdata.service.module.GrpcModule;
import ru.aivik.marketdata.service.module.PropertyModule;
import ru.aivik.marketdata.service.service.client.ExchangeClient;
import ru.aivik.marketdata.service.util.PropertyResolver;

import java.io.IOException;
import java.util.Map;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        var propertyModule = new PropertyModule();
        var propertyResolver = propertyModule.propertyResolver();
        var exchangeClientMap = resolveExchangeClientMap(propertyResolver);
        var controller = new GrpcController(exchangeClientMap);
        var grpcModule = new GrpcModule(propertyResolver);
        var server = grpcModule.grpcServer(controller);
        server.start();
        logger.info("started");
        server.awaitTermination();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("stop");
            server.shutdown();
        }));
    }

    private static Map<Integer, ExchangeClient> resolveExchangeClientMap(PropertyResolver propertyResolver) {
        var exchangeClientModule = new ExchangeClientModule(propertyResolver);
        var binanceClient = exchangeClientModule.binanceClient();
        return Map.of(binanceClient.getExchange(), binanceClient);
    }

}
