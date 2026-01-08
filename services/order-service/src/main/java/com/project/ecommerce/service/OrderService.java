package com.project.ecommerce.service;

import com.project.ecommerce.client.InventoryClient;
import com.project.ecommerce.dto.OrderRequest;
import com.project.ecommerce.event.OrderPlacedEvent;
import com.project.ecommerce.model.Order;
import com.project.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;


    public void placeOrder(OrderRequest orderRequest) {

        var isInStock = inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());

        if(isInStock) {
            Order order = new Order();
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setPrice(orderRequest.price().multiply(BigDecimal.valueOf(orderRequest.quantity())));
            order.setSkuCode(orderRequest.skuCode());
            order.setQuantity(orderRequest.quantity());
            orderRepository.save(order);

            OrderPlacedEvent event = new OrderPlacedEvent();
            event.setOrderNumber(orderRequest.skuCode());
            event.setFirstName(orderRequest.userDetails().firstName());
            event.setLastName(orderRequest.userDetails().lastName());
            event.setEmail(orderRequest.userDetails().email());
            log.info("Start - Sending OrderPlacedEvent {} to Kafka topic order-placed", event);
            kafkaTemplate.send("order-placed", event);
            log.info("End - Sending OrderPlacedEvent {} to Kafka topic order-placed", event);
        } else {
            throw new IllegalStateException(
                    "Product " + orderRequest.skuCode() + " is not in stock"
            );
        }
    }
}
