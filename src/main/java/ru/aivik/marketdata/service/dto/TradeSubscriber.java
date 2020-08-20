package ru.aivik.marketdata.service.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aivik.marketdata.MarketData;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;

public class TradeSubscriber implements Flow.Subscriber<MarketData.Trade> {

    private final BlockingQueue<MarketData.Trade> trades;
    private final Map.Entry<MarketData.GetTradesRequest.Exchange, String> subscriptionKey;
    private final String subscriberId;
    private final Logger logger;

    private Flow.Subscription subscription;

    public TradeSubscriber(MarketData.GetTradesRequest.Exchange exchange,
                           String instrument,
                           String subscriberId,
                           BlockingQueue<MarketData.Trade> trades) {
        this.trades = trades;
        this.subscriptionKey = Map.entry(exchange, instrument);
        this.subscriberId = subscriberId;
        var loggerNameStringBuilder = "ru.aivik.marketdata.service.subscriber." +
                exchange.name() +
                "_" +
                instrument +
                "_" +
                this.subscriberId;
        this.logger = LoggerFactory.getLogger(loggerNameStringBuilder);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
        logger.info("TradeSubscriber.onSubscribe");
    }

    @Override
    public void onNext(MarketData.Trade item) {
        trades.add(item);
        this.subscription.request(1);
        logger.debug("TradeSubscriber.onNext");
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("TradeSubscriber.onError", throwable);
    }

    @Override
    public void onComplete() {
        this.subscription.cancel();
        logger.info("TradeSubscriber.onComplete");
    }

    public Map.Entry<MarketData.GetTradesRequest.Exchange, String> getSubscriptionKey() {
        return subscriptionKey;
    }

    public String getSubscriberId() {
        return subscriberId;
    }
}
