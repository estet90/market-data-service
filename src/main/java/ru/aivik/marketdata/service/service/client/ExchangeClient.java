package ru.aivik.marketdata.service.service.client;

import com.google.protobuf.ByteString;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import ru.aivik.marketdata.MarketData;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public interface ExchangeClient {

    Closeable subscribeToAggTradeEvent(List<ByteString> instruments, BlockingQueue<MarketData.Trade> trades);

    default Closeable getCloseable(WebSocketListener listener, WebSocket websocket) {
        return () -> {
            var code = 1000;
            var reason = "canceled";
            listener.onClosing(websocket, code, reason);
            websocket.close(code, reason);
            listener.onClosed(websocket, code, reason);
        };
    }

    int getExchange();

}
