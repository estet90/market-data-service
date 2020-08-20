package ru.aivik.marketdata.service.service.aggregator;

import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.service.dto.TradeSubscriber;
import ru.aivik.marketdata.service.service.client.ExchangeClient;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SubmissionPublisher;

public class TradeDataAggregator {

    private final Map<Map.Entry<MarketData.GetTradesRequest.Exchange, String>, SubmissionPublisher<MarketData.Trade>> publishers;
    private final Map<MarketData.GetTradesRequest.Exchange, ExchangeClient> exchangeClientMap;

    public TradeDataAggregator(Map<MarketData.GetTradesRequest.Exchange, ExchangeClient> exchangeClientMap) {
        this.publishers = new ConcurrentHashMap<>();
        this.exchangeClientMap = exchangeClientMap;
    }

    public Closeable aggregate(List<TradeSubscriber> subscribers, MarketData.GetTradesRequest.Exchange exchange) {
        var newPublishers = new HashMap<String, SubmissionPublisher<MarketData.Trade>>();
        subscribers.forEach(subscriber -> {
            var publisher = publishers.get(subscriber.getSubscriptionKey());
            if (publisher != null) {
                publisher.subscribe(subscriber);
            } else {
                var newPublisher = new SubmissionPublisher<MarketData.Trade>();
                newPublisher.subscribe(subscriber);
                newPublishers.put(subscriber.getSubscriptionKey().getValue(), newPublisher);
            }
        });
        if (!newPublishers.isEmpty()) {
            newPublishers.forEach((instrument, publisher) -> publishers.put(Map.entry(exchange, instrument), publisher));
            var client = exchangeClientMap.get(exchange);
            return client.subscribeToAggTradeEvent(newPublishers);
        }
        return null;
    }

}
