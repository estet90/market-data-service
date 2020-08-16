package ru.aivik.marketdata.service.dto.binance;


import com.google.gson.annotations.SerializedName;

public record AggTradeEvent(@SerializedName("a")long aggregatedTradeId,
                            @SerializedName("p")float price,
                            @SerializedName("q")float quantity,
                            @SerializedName("f")long firstBreakdownTradeId,
                            @SerializedName("l")long lastBreakdownTradeId,
                            @SerializedName("T")long tradeTime,
                            @SerializedName("m")boolean isBuyerMaker,
                            @SerializedName("e")String eventType,
                            @SerializedName("E")long eventTime,
                            @SerializedName("s")String symbol) {
}
