package ru.aivik.marketdata.service;

import dagger.Component;
import io.grpc.Server;
import ru.aivik.marketdata.service.module.ExchangeClientModule;
import ru.aivik.marketdata.service.module.GrpcModule;
import ru.aivik.marketdata.service.module.PropertyModule;

import javax.inject.Singleton;

@Component(modules = {
        GrpcModule.class,
        PropertyModule.class,
        ExchangeClientModule.class
})
@Singleton
public interface ApplicationComponent {

    Server grpcServer();

}
