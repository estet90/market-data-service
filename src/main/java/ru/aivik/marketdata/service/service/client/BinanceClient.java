package ru.aivik.marketdata.service.service.client;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.service.dto.binance.AggTradeEvent;
import ru.aivik.marketdata.service.util.PropertyResolver;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.SubmissionPublisher;
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
                .pingInterval(1, TimeUnit.SECONDS)
                .build();
        this.url = propertyResolver.getStringProperty("socket.binance.url");
        this.tradeBuilder = tradeBuilder;
        gson = new Gson();
    }

    @Override
    public Closeable subscribeToAggTradeEvent(Map<String, SubmissionPublisher<MarketData.Trade>> newPublishers) {
        var url = resolveAggToEventUrl(newPublishers.keySet());
        var request = new Request.Builder()
                .url(url)
                .build();
        var listener = new BinanceWebSocketListener(gson, newPublishers, tradeBuilder);
        var websocket = binanceOkHttpClient.newWebSocket(request, listener);
        return getCloseable(listener, websocket, newPublishers);
    }

    private String resolveAggToEventUrl(Set<String> instruments) {
        var aggToEventUrl = new StringBuilder(this.url).append("/");
        instruments.forEach(instrument -> aggToEventUrl.append(instrument)
                .append("@aggTrade/")
        );
        return aggToEventUrl.substring(0, aggToEventUrl.length() - 1);
    }

    @Override
    public MarketData.GetTradesRequest.Exchange getExchange() {
        return MarketData.GetTradesRequest.Exchange.BINANCE;
    }

}
