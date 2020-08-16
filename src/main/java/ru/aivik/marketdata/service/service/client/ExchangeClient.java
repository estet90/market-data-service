package ru.aivik.marketdata.service.service.client;

import com.google.protobuf.ByteString;
import ru.aivik.marketdata.MarketData;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public interface ExchangeClient {

    void subscribeToAggTradeEvent(List<ByteString> instruments, BlockingQueue<MarketData.Trade> trades);

    int getExchange();

}
