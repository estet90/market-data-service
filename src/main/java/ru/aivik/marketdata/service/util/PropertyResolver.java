package ru.aivik.marketdata.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class PropertyResolver {

    private final Properties properties;

    private static final Logger logger = LoggerFactory.getLogger(PropertyResolver.class);

    public PropertyResolver(String propertyPath) {
        var isClassPath = propertyPath.startsWith("classpath:/");
        try (var inputStream = isClassPath
                ? getClass().getResourceAsStream(propertyPath.substring(10))
                : Files.newInputStream(Paths.get(propertyPath))) {
            this.properties = new Properties();
            properties.load(requireNonNull(inputStream));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getStringProperty(String key) {
        return getProperty(key, Function.identity());
    }

    public int getIntProperty(String key) {
        return getProperty(key, Integer::parseInt);
    }

    private <T> T getProperty(String key, Function<String, T> transformer) {
        try {
            requireNonNull(key);
            var value = requireNonNull(properties.getProperty(key));
            logger.info("PropertyResolver.getProperty key={} value={}", key, value);
            return transformer.apply(value);
        } catch (Exception e) {
            logger.error("PropertyResolver.getProperty.thrown key={}", key, e);
            throw e;
        }
    }

}
