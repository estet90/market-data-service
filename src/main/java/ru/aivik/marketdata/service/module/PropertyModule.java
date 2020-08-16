package ru.aivik.marketdata.service.module;

import ru.aivik.marketdata.service.util.PropertyResolver;

public class PropertyModule {

    public PropertyResolver propertyResolver() {
        return new PropertyResolver("classpath:/application.properties");
    }

}
