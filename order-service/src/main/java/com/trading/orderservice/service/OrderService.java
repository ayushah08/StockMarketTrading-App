package com.trading.orderservice.service;

import com.trading.orderservice.client.MarketDataClient;
import com.trading.orderservice.client.UserServiceClient;
import com.trading.orderservice.dto.OrderRequest;
import com.trading.orderservice.entity.Order;
import com.trading.orderservice.entity.OrderStatus;
import com.trading.orderservice.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final MarketDataClient marketDataClient;


    private final UserServiceClient userServiceClient;

    private static final String ORDER_EXECUTED_TOPIC = "order.executed";
    private static final String ORDER_FAILED_TOPIC = "order.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    /*.
    1 Get curreent price from the markwet
    2 calculate total amount
    3execute the order
    4publish order event to kafka
     */



    ;



    public Order placeOrder(@Valid OrderRequest request) {

        log.info("Placing {} order {} x {} for user : {}", request.getType(), request.getQuantity(), request.getSymbol(), request.getUserId());

        // S1
        Map<String, Object> priceData = marketDataClient.getStockPrice(request.getSymbol());

        BigDecimal currentPrice = new BigDecimal(priceData.get("price").toString());

        //s2
        BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        //create order
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setSymbol(request.getSymbol());
        order.setQuantity(request.getQuantity());
        order.setPrice(currentPrice);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        log.info("Order Created {}", savedOrder.getId());

        try {
            if (request.getType().equals("BUY")) {

                userServiceClient.deductFund(request.getUserId(), totalAmount);
            } else {
                userServiceClient.creditFunds(request.getUserId(), totalAmount);
            }

            savedOrder.setStatus(OrderStatus.EXECUTED);
            savedOrder.setExecutesAt(LocalDateTime.now());
            orderRepository.save(savedOrder);

            log.info("Order Executed : {} {} x {} at {}", savedOrder.getType(), request.getQuantity(), request.getSymbol(), currentPrice);

            publishOrderEvent(ORDER_EXECUTED_TOPIC, savedOrder, null);
        } catch (Exception e) {

            savedOrder.setStatus(OrderStatus.FAILED);
            savedOrder.setFailureReason(e.getMessage());

            log.error("Order failed: {}  reason: {}", savedOrder, e.getMessage());

            publishOrderEvent(ORDER_FAILED_TOPIC, savedOrder, e.getMessage());

        }

        return savedOrder;

    }

    private void publishOrderEvent(String topic, Order order, String reason) {

        Map<String, Object> orderEvents = new HashMap<>();
        orderEvents.put("orderId", order.getId());
        orderEvents.put("userId", order.getUserId());
        orderEvents.put("symbol", order.getSymbol());
        orderEvents.put("type", order.getType());
        orderEvents.put("quantity", order.getQuantity());
        orderEvents.put("price", order.getPrice());
        orderEvents.put("totalAmount", order.getTotalAmount());
        orderEvents.put("status", order.getStatus().name());
        if (reason != null) {
            orderEvents.put("reason", reason);
        }

        kafkaTemplate.send(topic, String.valueOf(order.getId()), orderEvents);

        log.info("Order Event Published: {} for order : {}", topic, order.getId());

    }

    public List<Order> getMyOrder(String userId) {

        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

    }
    public Order getOrder(String orderId) {

      return orderRepository.findById(orderId).orElseThrow(()-> new RuntimeException("Order Not Found : " + orderId));
    }
}
