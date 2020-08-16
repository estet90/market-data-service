package ru.aivik.marketdata.service.module;

import dagger.Module;
import dagger.Provides;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import ru.aivik.marketdata.service.controller.GrpcController;
import ru.aivik.marketdata.service.util.PropertyResolver;

import javax.inject.Singleton;

@Module
public class GrpcModule {

    @Provides
    @Singleton
    Server grpcServer(GrpcController controller, PropertyResolver propertyResolver) {
        var port = propertyResolver.getIntProperty("server.port");
        return ServerBuilder.forPort(port)
                .addService(controller)
                .build();
    }

}
