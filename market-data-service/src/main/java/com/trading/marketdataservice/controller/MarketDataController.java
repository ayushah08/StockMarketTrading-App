package com.trading.marketdataservice.controller;

import com.trading.marketdataservice.dto.StockPrice;
import com.trading.marketdataservice.entity.Stock;
import com.trading.marketdataservice.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/market")
@Slf4j
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    /*
    GetA;; Stock with current prices
     */

    @GetMapping("/stocks")
    public ResponseEntity<List<StockPrice>> getAllStocks(){
        return ResponseEntity.ok(marketDataService.getAllPrices());
    }

    /*
    Get Current Price For Specific Stock
     */

    @GetMapping("/stocks/{symbol}")
    public ResponseEntity<List<StockPrice>> getStockPrice(@PathVariable String symbol){

        return ResponseEntity.ok(marketDataService.getPrice(
                symbol.toUpperCase()
        ));
    }

    public ResponseEntity<List<Stock>> getStockList(){

        return ResponseEntity.ok(marketDataService.getAllStocks());

    }


}


