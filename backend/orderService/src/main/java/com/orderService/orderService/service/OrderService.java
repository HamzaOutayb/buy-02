package com.orderService.orderService.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.orderService.orderService.config.OrderTopic;
import com.orderService.orderService.dto.CreateOrderReq;
import com.orderService.orderService.dto.BuyerAnalyticsRes;
import com.orderService.orderService.dto.BuyerProductAnalyticsRes;
import com.orderService.orderService.dto.DeliveryAddressRes;
import com.orderService.orderService.dto.OrderItemRes;
import com.orderService.orderService.dto.OrderRes;
import com.orderService.orderService.dto.OrderSummaryRes;
import com.orderService.orderService.dto.ProductInfo;
import com.orderService.orderService.dto.SellerSalesAnalyticsRes;
import com.orderService.orderService.enums.OrderStatus;
import com.orderService.orderService.enums.PaymentMethod;
import com.orderService.orderService.enums.PaymentStatus;
import com.orderService.orderService.events.OrderEvent;
import com.orderService.orderService.events.OrderItemEvent;
import com.orderService.orderService.model.DeliveryAddress;
import com.orderService.orderService.model.Order;
import com.orderService.orderService.model.OrderItem;
import com.orderService.orderService.repository.OrderRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MongoTemplate mongoTemplate;

    public OrderRes createOrder(CreateOrderReq req, Authentication auth) {
        String customerId = auth.getName();
        log.info("Creating order for customer: {}", customerId);

        if (req.items() == null || req.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must contain at least one item");
        }

        List<OrderItem> items = req.items().stream().map(itemReq -> {
            ProductInfo product = productClient.getProduct(itemReq.productId());
            if (product.quantity() == null || product.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product out of stock: " + product.name());
            }
            if (product.quantity() < itemReq.quantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for " + product.name() + " (requested: " + itemReq.quantity()
                                + ", available: " + product.quantity() + ")");
            }
            String imageUrl = (product.imageUrls() != null && product.imageUrls().length > 0)
                    ? product.imageUrls()[0]
                    : "";
            double subtotal = product.price() * itemReq.quantity();
            return new OrderItem(
                    product.id(),
                    product.userId(),
                    product.name(),
                    imageUrl,
                    product.price(),
                    itemReq.quantity(),
                    subtotal);
        }).collect(Collectors.toList());

        double subtotal = items.stream().mapToDouble(OrderItem::getSubtotal).sum();
        double shippingFee = 0.0;
        double total = subtotal + shippingFee;

        String orderReference = generateOrderReference();

        CreateOrderReq.DeliveryAddressReq addrReq = req.deliveryAddress();
        DeliveryAddress deliveryAddress = new DeliveryAddress(
                addrReq.recipientName(), addrReq.phone(), addrReq.city(), addrReq.fullAddress());

        Instant now = Instant.now();
        Order order = new Order();
        order.setOrderReference(orderReference);
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(items);
        order.setDeliveryAddress(deliveryAddress);
        order.setPaymentMethod(req.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotal(total);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        Order saved = orderRepository.save(order);
        publishEvent("ORDER_CREATED", saved);
        log.info("Order created successfully with id {} and reference {}", saved.getId(), saved.getOrderReference());
        return toOrderRes(saved);
    }

    public Page<OrderSummaryRes> getMyOrders(String customerId, OrderStatus status, Instant dateFrom, Instant dateTo,
            String orderReference, Pageable pageable) {
        Criteria criteria = Criteria.where("customerId").is(customerId);
        if (status != null) {
            criteria.and("status").is(status);
        }
        if (orderReference != null && !orderReference.isBlank()) {
            criteria.and("orderReference").regex(orderReference.trim(), "i");
        }
        if (dateFrom != null && dateTo != null) {
            criteria.and("createdAt").gte(dateFrom).lte(dateTo);
        } else if (dateFrom != null) {
            criteria.and("createdAt").gte(dateFrom);
        } else if (dateTo != null) {
            criteria.and("createdAt").lte(dateTo);
        }

        Pageable sortedPageable = ensureSort(pageable);
        Query query = new Query(criteria).with(sortedPageable);
        long total = mongoTemplate.count(new Query(criteria), Order.class);
        List<Order> orders = mongoTemplate.find(query, Order.class);
        return new PageImpl<>(orders, sortedPageable, total).map(this::toSummaryRes);
    }

    public BuyerAnalyticsRes getBuyerAnalytics(String customerId) {
        Query query = Query.query(Criteria.where("customerId").is(customerId)
                .and("status").ne(OrderStatus.CANCELLED));
        List<Order> orders = mongoTemplate.find(query, Order.class);
        Map<String, BuyerProductTotals> productTotals = new HashMap<>();
        double totalSpent = 0.0;

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            totalSpent += order.getTotal() != null ? order.getTotal() : 0.0;
            if (order.getItems() == null) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                if (item.getProductId() == null) {
                    continue;
                }
                BuyerProductTotals totals = productTotals.computeIfAbsent(item.getProductId(),
                        ignored -> new BuyerProductTotals(item.getProductName()));
                totals.quantity += item.getQuantity() == null ? 0 : item.getQuantity();
                totals.spent += item.getSubtotal() == null ? 0.0 : item.getSubtotal();
            }
        }

        List<BuyerProductAnalyticsRes> mostBoughtProducts = productTotals.entrySet().stream()
            .sorted(Map.Entry.<String, BuyerProductTotals>comparingByValue(
                Comparator.comparingLong((BuyerProductTotals totals) -> totals.quantity)
                    .reversed()
                    .thenComparing(Comparator.comparingDouble(
                        (BuyerProductTotals totals) -> totals.spent).reversed())))
                .map(entry -> new BuyerProductAnalyticsRes(entry.getKey(), entry.getValue().productName,
                        entry.getValue().quantity, entry.getValue().spent))
                .toList();
        return new BuyerAnalyticsRes(totalSpent, mostBoughtProducts);
    }

    public OrderRes getMyOrderById(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toOrderRes(order);
    }

    public OrderRes cancelMyOrder(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order cannot be cancelled in its current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);
        publishEvent("ORDER_CANCELLED", saved);
        return toOrderRes(saved);
    }

    public OrderRes reorder(String orderId, String customerId) {
        Order original = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!original.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        List<OrderItem> items = original.getItems().stream().map(origItem -> {
            ProductInfo product = productClient.getProduct(origItem.getProductId());
            if (product.quantity() == null || product.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product out of stock: " + product.name());
            }
            if (product.quantity() < origItem.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for " + product.name() + " (requested: " + origItem.getQuantity()
                                + ", available: " + product.quantity() + ")");
            }
            String imageUrl = (product.imageUrls() != null && product.imageUrls().length > 0)
                    ? product.imageUrls()[0]
                    : origItem.getImageUrl();
            double subtotal = product.price() * origItem.getQuantity();
            return new OrderItem(
                    product.id(),
                    product.userId(),
                    product.name(),
                    imageUrl,
                    product.price(),
                    origItem.getQuantity(),
                    subtotal);
        }).collect(Collectors.toList());

        double subtotal = items.stream().mapToDouble(OrderItem::getSubtotal).sum();
        double shippingFee = original.getShippingFee() != null ? original.getShippingFee() : 0.0;
        double total = subtotal + shippingFee;

        Instant now = Instant.now();
        Order newOrder = new Order();
        newOrder.setOrderReference(generateOrderReference());
        newOrder.setCustomerId(customerId);
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setItems(items);
        newOrder.setDeliveryAddress(original.getDeliveryAddress());
        newOrder.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        newOrder.setPaymentStatus(PaymentStatus.PENDING);
        newOrder.setSubtotal(subtotal);
        newOrder.setShippingFee(shippingFee);
        newOrder.setTotal(total);
        newOrder.setCreatedAt(now);
        newOrder.setUpdatedAt(now);

        Order saved = orderRepository.save(newOrder);
        publishEvent("ORDER_CREATED", saved);
        return toOrderRes(saved);
    }

    public Page<OrderSummaryRes> getSellerOrders(String sellerId, String customer, String orderReference,
            OrderStatus status, Instant dateFrom, Instant dateTo, Pageable pageable) {
        Criteria criteria = Criteria.where("items.sellerId").is(sellerId);
        if (customer != null && !customer.isBlank()) {
            criteria.and("customerId").regex(customer.trim(), "i");
        }
        if (orderReference != null && !orderReference.isBlank()) {
            criteria.and("orderReference").regex(orderReference.trim(), "i");
        }
        if (status != null) {
            criteria.and("status").is(status);
        }
        if (dateFrom != null && dateTo != null) {
            criteria.and("createdAt").gte(dateFrom).lte(dateTo);
        } else if (dateFrom != null) {
            criteria.and("createdAt").gte(dateFrom);
        } else if (dateTo != null) {
            criteria.and("createdAt").lte(dateTo);
        }

        Pageable sortedPageable = ensureSort(pageable);
        Query query = new Query(criteria).with(sortedPageable);
        long total = mongoTemplate.count(new Query(criteria), Order.class);
        List<Order> orders = mongoTemplate.find(query, Order.class);
        return new PageImpl<>(orders, sortedPageable, total).map(o -> toSellerSummaryRes(o, sellerId));
    }

    public SellerSalesAnalyticsRes getSellerAnalytics(String sellerId) {
        Query query = Query.query(Criteria.where("items.sellerId").is(sellerId));
        List<Order> orders = mongoTemplate.find(query, Order.class);
        long pendingOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .count();
        long completedOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .count();
        long unitsSold = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .filter(order -> order.getItems() != null)
                .flatMap(order -> order.getItems().stream())
                .filter(item -> sellerId.equals(item.getSellerId()))
                .mapToLong(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();
        double revenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .filter(order -> order.getItems() != null)
                .flatMap(order -> order.getItems().stream())
                .filter(item -> sellerId.equals(item.getSellerId()))
                .mapToDouble(item -> item.getSubtotal() == null ? 0.0 : item.getSubtotal())
                .sum();
        return new SellerSalesAnalyticsRes(orders.size(), pendingOrders, completedOrders, unitsSold, revenue);
    }

    public OrderRes getSellerOrderById(String orderId, String sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        List<OrderItem> sellerItems = order.getItems().stream()
                .filter(item -> sellerId.equals(item.getSellerId()))
                .collect(Collectors.toList());
        if (sellerItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not have items in this order");
        }

        double sellerSubtotal = sellerItems.stream().mapToDouble(OrderItem::getSubtotal).sum();
        List<OrderItemRes> itemResList = sellerItems.stream().map(this::toItemRes).collect(Collectors.toList());
        DeliveryAddress addr = order.getDeliveryAddress();
        DeliveryAddressRes addrRes = new DeliveryAddressRes(
                addr.getRecipientName(), addr.getPhone(), addr.getCity(), addr.getFullAddress());

        return new OrderRes(
                order.getId(),
                order.getOrderReference(),
                order.getCustomerId(),
                order.getStatus(),
                itemResList,
                addrRes,
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                sellerSubtotal,
                0.0,
                sellerSubtotal,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public OrderRes confirmOrder(String orderId, String sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        boolean hasSellersItems = order.getItems().stream()
                .anyMatch(item -> sellerId.equals(item.getSellerId()));
        if (!hasSellersItems) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order cannot be confirmed in its current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);
        publishEvent("ORDER_CONFIRMED", saved);
        publishInventoryEvents(saved);
        return toOrderRes(saved);
    }

    public OrderRes updateSellerOrderStatus(String orderId, String sellerId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        boolean hasSellersItems = order.getItems().stream()
                .anyMatch(item -> sellerId.equals(item.getSellerId()));
        if (!hasSellersItems) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        OrderStatus current = order.getStatus();
        boolean validTransition = (current == OrderStatus.CONFIRMED && newStatus == OrderStatus.PROCESSING)
                || (current == OrderStatus.PROCESSING && newStatus == OrderStatus.SHIPPED)
                || (current == OrderStatus.SHIPPED && newStatus == OrderStatus.DELIVERED);
        if (!validTransition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition from " + current + " to " + newStatus);
        }
        order.setStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);
        publishEvent("ORDER_STATUS_UPDATED", saved);
        return toOrderRes(saved);
    }

    public OrderRes cancelSellerOrder(String orderId, String sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        boolean hasSellersItems = order.getItems().stream()
                .anyMatch(item -> sellerId.equals(item.getSellerId()));
        if (!hasSellersItems) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        OrderStatus current = order.getStatus();
        if (current != OrderStatus.PENDING && current != OrderStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order cannot be cancelled in its current status: " + current);
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);
        publishEvent("ORDER_CANCELLED", saved);
        if (current == OrderStatus.CONFIRMED) {
            publishInventoryEvents(saved, "INCREASE");
        }
        return toOrderRes(saved);
    }

    private Pageable ensureSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return pageable;
    }

    private void publishEvent(String eventType, Order order) {
        try {
            OrderEvent event = new OrderEvent(eventType, order.getId(), order.getOrderReference(),
                    order.getCustomerId(), Instant.now());
            kafkaTemplate.send(OrderTopic.NAME, order.getId(), event);
        } catch (Exception e) {
            log.warn("Failed to publish Kafka event {}: {}", eventType, e.getMessage());
        }
    }

    private void publishInventoryEvents(Order order) {
        publishInventoryEvents(order, "DECREASE");
    }

    private void publishInventoryEvents(Order order, String operation) {
        if (order.getItems() == null) {
            return;
        }
        order.getItems().forEach(item -> kafkaTemplate.send(
                OrderTopic.INVENTORY_NAME,
                item.getProductId(),
                new OrderItemEvent(order.getId(), item.getProductId(), item.getQuantity(), operation)));
    }

    private String generateOrderReference() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private static class BuyerProductTotals {
        private final String productName;
        private long quantity;
        private double spent;

        private BuyerProductTotals(String productName) {
            this.productName = productName;
        }
    }

    private OrderRes toOrderRes(Order order) {
        List<OrderItemRes> itemResList = order.getItems().stream()
                .map(this::toItemRes).collect(Collectors.toList());
        DeliveryAddress addr = order.getDeliveryAddress();
        DeliveryAddressRes addrRes = addr != null
                ? new DeliveryAddressRes(addr.getRecipientName(), addr.getPhone(), addr.getCity(), addr.getFullAddress())
                : new DeliveryAddressRes("", "", "", "");
        return new OrderRes(
                order.getId(),
                order.getOrderReference(),
                order.getCustomerId(),
                order.getStatus(),
                itemResList,
                addrRes,
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private OrderSummaryRes toSummaryRes(Order order) {
        return new OrderSummaryRes(
                order.getId(),
                order.getOrderReference(),
                order.getCustomerId(),
                order.getStatus(),
                order.getItems() != null ? order.getItems().size() : 0,
                order.getTotal(),
                order.getPaymentMethod(),
                order.getCreatedAt());
    }

    private OrderSummaryRes toSellerSummaryRes(Order order, String sellerId) {
        List<OrderItem> sellerItems = order.getItems() != null
                ? order.getItems().stream().filter(item -> sellerId.equals(item.getSellerId())).collect(Collectors.toList())
                : List.of();
        double sellerTotal = sellerItems.stream().mapToDouble(OrderItem::getSubtotal).sum();
        return new OrderSummaryRes(
                order.getId(),
                order.getOrderReference(),
                order.getCustomerId(),
                order.getStatus(),
                sellerItems.size(),
                sellerTotal,
                order.getPaymentMethod(),
                order.getCreatedAt());
    }

    private OrderItemRes toItemRes(OrderItem item) {
        return new OrderItemRes(
                item.getProductId(),
                item.getSellerId(),
                item.getProductName(),
                item.getImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal());
    }
}
