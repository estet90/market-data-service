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
            listener.onClosing(websocket, code, "canceled");
            websocket.close(code, "canceled");
            listener.onClosed(websocket, code, "canceled");
        };
    }

    int getExchange();

}
