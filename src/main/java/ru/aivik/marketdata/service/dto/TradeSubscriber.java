package ru.aivik.marketdata.service.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aivik.marketdata.MarketData;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;

public class TradeSubscriber implements Flow.Subscriber<MarketData.Trade> {

    private final BlockingQueue<MarketData.Trade> trades;
    private final Map.Entry<MarketData.GetTradesRequest.Exchange, String> subscriptionKey;
    private final String subscriberId;
    private final Logger logger;

    private Flow.Subscription subscription;

    /**
     * Подписка на информацию об инструменте
     *
     * @param exchange - биржа
     * @param instrument - инструмент
     * @param subscriberId - уникальный идентификатор, рекомендуется использовать {@link java.util.UUID},
     *                     т.к. это единственное поле, которое проверяется в методах equals, hashcode
     * @param trades - очередь, в которую будут складывваться данные
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (TradeSubscriber) o;
        return Objects.equals(subscriberId, that.subscriberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriberId);
    }
}
