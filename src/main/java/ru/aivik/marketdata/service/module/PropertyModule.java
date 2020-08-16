package ru.aivik.marketdata.service.module;

import dagger.Module;
import dagger.Provides;
import ru.aivik.marketdata.service.util.PropertyResolver;

import javax.inject.Singleton;

@Module
public class PropertyModule {

    @Provides
    @Singleton
    PropertyResolver propertyResolver() {
        return new PropertyResolver("classpath:/application.properties");
    }

}
