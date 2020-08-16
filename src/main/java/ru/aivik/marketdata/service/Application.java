package ru.aivik.marketdata.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        var server = DaggerApplicationComponent.builder()
                .build()
                .grpcServer();
        server.start();
        logger.info("started");
        server.awaitTermination();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("stop");
            server.shutdown();
        }));
    }

}
