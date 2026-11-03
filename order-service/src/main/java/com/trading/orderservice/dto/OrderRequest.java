package com.trading.orderservice.dto;

import com.trading.orderservice.entity.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    private String userId;

    @NotBlank(message = "Stock Symbol is required")
    private String symbol;

    @NotBlank(message = "Order type is required - BUY o Sell")
    private OrderType type;

    @Min(value = 1 , message = "Quantity must be at least 1")
    private int quantity;


}
