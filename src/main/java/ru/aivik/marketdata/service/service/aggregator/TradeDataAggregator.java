package ru.aivik.marketdata.service.service.aggregator;

import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.service.dto.TradeSubscriber;
import ru.aivik.marketdata.service.service.client.ExchangeClient;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.SubmissionPublisher;

public class TradeDataAggregator {

    private final Map<Map.Entry<MarketData.GetTradesRequest.Exchange, String>, SubmissionPublisher<MarketData.Trade>> publishers;
    private final Map<MarketData.GetTradesRequest.Exchange, ExchangeClient> exchangeClientMap;

    public TradeDataAggregator(Map<MarketData.GetTradesRequest.Exchange, ExchangeClient> exchangeClientMap) {
        this.publishers = new HashMap<>();
        this.exchangeClientMap = exchangeClientMap;
    }

    public synchronized Closeable aggregate(Set<TradeSubscriber> subscribers, MarketData.GetTradesRequest.Exchange exchange) {
        var newPublishers = new HashMap<String, SubmissionPublisher<MarketData.Trade>>();
        var oldUsedPublishers = new HashSet<SubmissionPublisher<MarketData.Trade>>();
        var instruments = new HashSet<String>();
        subscribers.forEach(subscriber -> {
            var subscriptionKey = subscriber.getSubscriptionKey();
            var publisher = publishers.get(subscriptionKey);
            if (publisher == null) {
                var newPublisher = new SubmissionPublisher<MarketData.Trade>();
                newPublisher.subscribe(subscriber);
                newPublishers.put(subscriptionKey.getValue().toUpperCase(), newPublisher);
                instruments.add(subscriptionKey.getValue());
            } else if (publisher.isClosed()) {
                publishers.remove(subscriptionKey);
                var newPublisher = new SubmissionPublisher<MarketData.Trade>();
                newPublisher.subscribe(subscriber);
                newPublishers.put(subscriptionKey.getValue().toUpperCase(), newPublisher);
                instruments.add(subscriptionKey.getValue());
            } else {
                publisher.subscribe(subscriber);
                oldUsedPublishers.add(publisher);
            }
        });
        if (!newPublishers.isEmpty()) {
            newPublishers.forEach((instrument, publisher) -> publishers.put(Map.entry(exchange, instrument), publisher));
            var client = exchangeClientMap.get(exchange);
            return client.subscribeToAggTradeEvent(newPublishers, instruments);
        }
        return () -> oldUsedPublishers.stream()
                .map(SubmissionPublisher::getSubscribers)
                .flatMap(List::stream)
                .map(TradeSubscriber.class::cast)
                .filter(subscribers::contains)
                .forEach(TradeSubscriber::onComplete);
    }

}
