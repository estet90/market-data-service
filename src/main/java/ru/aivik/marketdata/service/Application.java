package ru.aivik.marketdata.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aivik.marketdata.service.controller.GrpcController;
import ru.aivik.marketdata.service.module.ExchangeClientModule;
import ru.aivik.marketdata.service.module.GrpcModule;
import ru.aivik.marketdata.service.module.PropertyModule;

import java.io.IOException;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        var propertyResolver = new PropertyModule().propertyResolver();
        var exchangeClientMap = new ExchangeClientModule(propertyResolver).resolveExchangeClientMap();
        var controller = new GrpcController(exchangeClientMap);
        var server = new GrpcModule(propertyResolver).grpcServer(controller);
        server.start();
        logger.info("started");
        server.awaitTermination();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("stop");
            server.shutdown();
        }));
    }

}
