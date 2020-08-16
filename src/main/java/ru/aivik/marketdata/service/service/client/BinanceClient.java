package ru.aivik.marketdata.service.service.client;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.service.dto.binance.AggTradeEvent;
import ru.aivik.marketdata.service.util.PropertyResolver;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class BinanceClient implements ExchangeClient {

    private final OkHttpClient binanceOkHttpClient;
    private final String url;
    private final Gson gson;
    private final Function<AggTradeEvent, MarketData.Trade> tradeBuilder;

    public BinanceClient(PropertyResolver propertyResolver, Function<AggTradeEvent, MarketData.Trade> tradeBuilder) {
        this.binanceOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(propertyResolver.getIntProperty("socket.binance.connect-timeout.seconds"), TimeUnit.SECONDS)
                .readTimeout(propertyResolver.getIntProperty("socket.binance.read-timeout.seconds"), TimeUnit.SECONDS)
                .build();
        this.url = propertyResolver.getStringProperty("socket.binance.url");
        this.tradeBuilder = tradeBuilder;
        gson = new Gson();
    }

    public Closeable subscribeToAggTradeEvent(List<ByteString> instruments, BlockingQueue<MarketData.Trade> trades) {
        var url = resolveAggToEventUrl(instruments);
        var request = new Request.Builder()
                .url(url)
                .build();
        var listener = new BinanceWebSocketListener(gson, trades, tradeBuilder);
        var websocket = binanceOkHttpClient.newWebSocket(request, listener);
        return getCloseable(listener, websocket);
    }

    private String resolveAggToEventUrl(List<ByteString> instruments) {
        var aggToEventUrl = new StringBuilder(this.url).append("/");
        instruments.forEach(instrument -> aggToEventUrl.append(instrument.toStringUtf8())
                .append("@")
                .append("aggTrade")
                .append("/")
        );
        return aggToEventUrl.substring(0, aggToEventUrl.length() - 1);
    }

    @Override
    public int getExchange() {
        return 1;
    }

}
