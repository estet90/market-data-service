package ru.aivik.marketdata.service.module;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import ru.aivik.marketdata.service.controller.GrpcController;
import ru.aivik.marketdata.service.util.PropertyResolver;

public class GrpcModule {

    private final PropertyResolver propertyResolver;

    public GrpcModule(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    public Server grpcServer(GrpcController controller) {
        var port = propertyResolver.getIntProperty("server.port");
        return ServerBuilder.forPort(port)
                .addService(controller)
                .build();
    }

}
