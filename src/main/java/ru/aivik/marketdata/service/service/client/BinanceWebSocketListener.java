package ru.aivik.marketdata.service.service.client;

import com.google.gson.Gson;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aivik.marketdata.MarketData;
import ru.aivik.marketdata.service.dto.binance.AggTradeEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class BinanceWebSocketListener extends WebSocketListener {

    private final Gson gson;
    private final Map<String, SubmissionPublisher<MarketData.Trade>> newPublishers;
    private final Function<AggTradeEvent, MarketData.Trade> tradeBuilder;

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketListener.class);

    public BinanceWebSocketListener(Gson gson,
                                    Map<String, SubmissionPublisher<MarketData.Trade>> newPublishers,
                                    Function<AggTradeEvent, MarketData.Trade> tradeBuilder) {
        this.gson = gson;
        this.newPublishers = newPublishers;
        this.tradeBuilder = tradeBuilder;
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        logger.warn("BinanceWebSocketListener.onClosed reason={}", reason);
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        logger.warn("BinanceWebSocketListener.onClosing reason={}", reason);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        var point = "BinanceWebSocketListener.onFailure";
        var responseBody = ofNullable(response).map(Response::body).orElse(null);
        extractPayload(point, responseBody)
                .ifPresentOrElse(
                        payload -> logger.error("{}\n\tresponse={}\n", point, payload, t),
                        () -> logger.error(point, t)
                );
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        var event = gson.fromJson(text, AggTradeEvent.class);
        var trade = tradeBuilder.apply(event);
        var publisher = newPublishers.get(trade.getInstrument().toUpperCase());
        publisher.submit(trade);
        logger.trace("BinanceWebSocketListener.onMessage.string\n\tresponse={}", text);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        var text = bytes.hex();
        var event = gson.fromJson(text, AggTradeEvent.class);
        var trade = tradeBuilder.apply(event);
        var publisher = newPublishers.get(trade.getInstrument().toUpperCase());
        publisher.submit(trade);
        logger.trace("BinanceWebSocketListener.onMessage.bytes\n\tresponse={}", text);
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        var point = "BinanceWebSocketListener.onOpen";
        extractPayload(point, response.body())
                .ifPresentOrElse(
                        payload -> logger.info("{}\n\tresponse={}", point, payload),
                        () -> logger.info(point)
                );
    }

    private Optional<String> extractPayload(String point, ResponseBody responseBody) {
        return ofNullable(responseBody)
                .map(body -> {
                    try {
                        return body.bytes();
                    } catch (IOException e) {
                        logger.error("{}.thrown {}", point, e.getMessage());
                        return null;
                    }
                })
                .filter(bytes -> bytes.length > 0)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }
}
