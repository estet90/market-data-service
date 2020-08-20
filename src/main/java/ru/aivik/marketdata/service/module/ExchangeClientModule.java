package ru.aivik.marketdata.service.module;

import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.service.dto.binance.AggTradeEvent;
import ru.aivik.marketdata.service.service.client.BinanceClient;
import ru.aivik.marketdata.service.service.client.ExchangeClient;
import ru.aivik.marketdata.service.util.PropertyResolver;

import java.util.Map;
import java.util.function.Function;

public class ExchangeClientModule {

    private final PropertyResolver propertyResolver;

    public ExchangeClientModule(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    public Map<MarketData.GetTradesRequest.Exchange, ExchangeClient> resolveExchangeClientMap() {
        var binanceClient = binanceClient();
        return Map.of(binanceClient.getExchange(), binanceClient);
    }

    private ExchangeClient binanceClient() {
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
