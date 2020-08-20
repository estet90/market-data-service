package ru.aivik.marketdata.service.service.client;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import ru.aivik.marketdata.MarketData;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.SubmissionPublisher;

public interface ExchangeClient {

    Closeable subscribeToAggTradeEvent(Map<String, SubmissionPublisher<MarketData.Trade>> newPublishers);

    default Closeable getCloseable(WebSocketListener listener,
                                   WebSocket websocket,
                                   Map<String, SubmissionPublisher<MarketData.Trade>> newPublishers) {
        return () -> {
            var code = 1000;
            var reason = "canceled";
            listener.onClosing(websocket, code, reason);
            websocket.close(code, reason);
            listener.onClosed(websocket, code, reason);
            newPublishers.values().forEach(SubmissionPublisher::close);
        };
    }

    MarketData.GetTradesRequest.Exchange getExchange();

}
