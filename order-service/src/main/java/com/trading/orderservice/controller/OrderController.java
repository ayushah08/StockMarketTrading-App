package com.trading.orderservice.controller;

import com.trading.orderservice.dto.OrderRequest;
import com.trading.orderservice.entity.Order;
import com.trading.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    //Place Buy or Sell Order
    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestHeader("X-User-Id") String userId, @Valid @RequestBody OrderRequest request) {

        request.setUserId(userId);//override
        return ResponseEntity.status(HttpStatus.CREATED ).body(orderService.placeOrder(request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable("X-User-Id") String orderId, @Valid @RequestHeader("X-User-Id") String userId) {
        Order order = orderService.getOrder(orderId);

        if(!order.getUserId().equals(userId)){
            return  ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrder(@RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(orderService.getMyOrder(userId));
    }

}
