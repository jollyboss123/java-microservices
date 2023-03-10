package com.jolly.orderservice.service;

import com.jolly.orderservice.dto.InventoryResponse;
import com.jolly.orderservice.dto.OrderLineItemsDto;
import com.jolly.orderservice.dto.OrderRequest;
import com.jolly.orderservice.event.OrderPlacedEvent;
import com.jolly.orderservice.model.Order;
import com.jolly.orderservice.model.OrderLineItems;
import com.jolly.orderservice.repository.OrderRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList().stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

        log.info("Calling inventory service");

        // Customize span id
        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

        try (Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())) {
            // Call Inventory service and place order if product is in stock
            InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory", uriBuilder ->
                            uriBuilder.queryParam("skuCode", skuCodes)
                                    .build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            assert inventoryResponses != null;
            boolean allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);
            List<String> productsNotInStock = Arrays.stream(inventoryResponses)
                    .filter(inventoryResponse -> !inventoryResponse.isInStock())
                    .map(InventoryResponse::getSkuCode)
                    .toList();

            if (allProductsInStock) {
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderId()));
                return "Order Placed Successfully";
            } else {
                log.error("Products not in stock: {}", productsNotInStock);
                throw new IllegalArgumentException("Product is not in stock, please try again later.");
            }
        } finally {
            inventoryServiceLookup.end();
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
