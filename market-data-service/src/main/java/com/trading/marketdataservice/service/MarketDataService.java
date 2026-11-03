package com.trading.marketdataservice.service;

import com.trading.marketdataservice.dto.StockPrice;
import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {

    private final StockRepository stockRepository;

    private final Random random = new Random();

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRICE_KEY_PREFIX = "stock:price:";

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PRICE_UPDATED_TOPIC = "stock.price.updated";
    /*
    Concurent bcoz thread Safety
     */
    private final Map<String, StockPrice> stockPrices = new ConcurrentHashMap<>();


    /*
    post construct runs when  the context is completely ready
     */
    @PostConstruct
    public void initializePrices() {
        log.info("initial stock data.........");
        List<Stock> stocks = stockRepository.findByActiveTrue();

        if (stocks.isEmpty()) {
            log.warn("No active Stocks found");
            return;
        }
        stocks.forEach(stock -> {
            StockPrice stockPrice = new StockPrice();
            stockPrice.setSymbol(stock.getSymbol());
            stockPrice.setCompanyName(stock.getCompanyName());
            stockPrice.setPrice(stock.getInitialPrice());
            stockPrice.setOpen(stock.getInitialPrice());
            stockPrice.setHigh(stock.getInitialPrice());
            stockPrice.setLow(stock.getInitialPrice());
            stockPrice.setChange(BigDecimal.ZERO);
            stockPrice.setChangePercent(BigDecimal.ZERO);
            stockPrice.setVolume(0L);
            stockPrice.setTimeStamp(LocalDateTime.now());

            stockPrices.put(stock.getSymbol(), stockPrice);

            log.info("Loaded Stock : {} at {}", stock.getSymbol(), stock.getInitialPrice());

        });

        log.info("Initialized {} Stocks", stocks.size());

    }


    /*
    Scheduled Price Update every 2 seconds
     */
    @Scheduled(fixedRateString =
            "{martket.price-update-interval}")
    public void updatePrices() {

        if (stockPrices.isEmpty()) {
            log.warn("No  Stocks loaded");
            return;
        }

        stockPrices.forEach((symbol, currentPrice) ->
        {
            BigDecimal oldPrice = currentPrice.getPrice();

            // +- 0.5 price movement
            double priceMovement = (random.nextDouble() - 0.5) * 0.5;

            BigDecimal priceChange = oldPrice.multiply(BigDecimal.valueOf(priceMovement / 100))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            BigDecimal newPrice = oldPrice.add(priceChange).setScale(2, BigDecimal.ROUND_HALF_UP);

            //Prevnt -ve Price
            if (newPrice.compareTo(BigDecimal.valueOf(1)) < 0) {
                newPrice = BigDecimal.valueOf(1);
            }

            BigDecimal changeAmount = newPrice.subtract(currentPrice.getOpen())
                    .setScale(2, BigDecimal.ROUND_HALF_UP);


            BigDecimal changePercent = changeAmount.divide(currentPrice.getOpen(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)
                            .setScale(2, BigDecimal.ROUND_HALF_UP));

            //update High Low
            BigDecimal newHigh = newPrice.compareTo(currentPrice.getHigh()) > 0 ? newPrice : currentPrice.getHigh();

            BigDecimal newLow = newPrice.compareTo(currentPrice.getLow()) < 0 ? newPrice : currentPrice.getLow();

            currentPrice.setPrice(newPrice);
            currentPrice.setHigh(newHigh);
            currentPrice.setLow(newLow);
            currentPrice.setChange(changeAmount);
            currentPrice.setChangePercent(changePercent);
            currentPrice.setTimeStamp(LocalDateTime.now());

            //1. Cache in redis
            redisTemplate.opsForValue().set(PRICE_KEY_PREFIX + symbol, currentPrice);

            //2Broadcast via web Socket
            simpMessagingTemplate.convertAndSend("/topic/prices" + symbol, currentPrice);

            //Publish To KAFKA
            Map<String, Object> priceUpdatedEvent = new HashMap<>();
            priceUpdatedEvent.put("symbol", symbol);
            priceUpdatedEvent.put("price", currentPrice.getPrice());
            priceUpdatedEvent.put("Timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(PRICE_UPDATED_TOPIC, symbol, priceUpdatedEvent);


        });

        log.info("Prices Updated for  {} Stocks", stockPrices.size());
    }



    /*
    Get All Active Stock Prices
     */

    public List<StockPrice> getAllPrices() {
return stockPrices.values().stream().toList();


    }


    public List<StockPrice> getPrice(String symbol) {

        Object cached = redisTemplate.opsForValue().get(PRICE_KEY_PREFIX + symbol);

        if (cached != null) {
            return Collections.singletonList((StockPrice) cached);
        }

        //FallBack
        StockPrice price = stockPrices.get(symbol.toUpperCase());

        if(price == null) {
            throw new RuntimeException("Stock Not Found: " +  symbol);
        }

        return Collections.singletonList(price);
    }


    /*
    To get All Active Stock Prices
     */
    public List<Stock> getAllStocks() {
        return stockRepository.findByActiveTrue();

    }
}
