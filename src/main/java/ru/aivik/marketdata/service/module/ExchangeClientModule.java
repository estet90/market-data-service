package ru.aivik.marketdata.service.module;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.service.dto.binance.AggTradeEvent;
import ru.aivik.marketdata.service.service.client.BinanceClient;
import ru.aivik.marketdata.service.service.client.ExchangeClient;
import ru.aivik.marketdata.service.util.PropertyResolver;

import javax.inject.Singleton;
import java.util.function.Function;

@Module
public class ExchangeClientModule {

    @Provides
    @Singleton
    @IntoSet
    ExchangeClient BinanceClient(PropertyResolver propertyResolver) {
        Function<AggTradeEvent, MarketData.Trade> tradeBuilder = event -> MarketData.Trade.newBuilder()
                .setInstrument(event.symbol())
                .setTime(event.tradeTime())
                .setBuy(event.isBuyerMaker())
                .setPrice(event.price())
                .setVolume(event.quantity())
                .build();
        return new BinanceClient(propertyResolver, tradeBuilder);
    }

}
