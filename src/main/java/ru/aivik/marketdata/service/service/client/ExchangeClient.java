package ru.aivik.marketdata.service.service.client;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import ru.aivik.marketdata.MarketData;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.SubmissionPublisher;

public interface ExchangeClient {

    /**
     *
     * @param newPublishers - новые публикаторы для инструментов, ключ - имя инструмента в верхнем регистре (важно!)
     * @param instruments - список инструментов
     * @return {@link Closeable} - коллбэк, в котором должно быть описано закрытие подписок
     */
    Closeable subscribeToAggTradeEvent(Map<String, SubmissionPublisher<MarketData.Trade>> newPublishers, Set<String> instruments);

    default Closeable getCloseable(WebSocketListener listener,
                                   WebSocket websocket,
                                   Map<String, SubmissionPublisher<MarketData.Trade>> publishers) {
        return () -> {
            var code = 1000;
            var reason = "closed";
            listener.onClosing(websocket, code, reason);
            websocket.close(code, reason);
            listener.onClosed(websocket, code, reason);
            publishers.values().forEach(SubmissionPublisher::close);
        };
    }

    MarketData.GetTradesRequest.Exchange getExchange();

}
