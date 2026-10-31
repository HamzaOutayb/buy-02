package com.example.orderservice.service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.dto.CheckoutRequest;
import com.example.orderservice.dto.DeliveryAddressRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.ProductSummary;
import com.example.orderservice.exception.ApiException;
import com.example.orderservice.model.Cart;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.repository.OrderRepository;

@Service
public class OrderService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final MongoTemplate mongoTemplate;

    public OrderService(
            CartRepository cartRepository,
            OrderRepository orderRepository,
            ProductClient productClient,
            MongoTemplate mongoTemplate) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.mongoTemplate = mongoTemplate;
    }

    public List<OrderResponse> checkout(String buyerId, CheckoutRequest request, String authorization) {
        List<Order> existing = orderRepository.findByBuyerIdAndCheckoutKey(buyerId, request.checkoutKey());
        if (!existing.isEmpty()) {
            return existing.stream().map(this::toResponse).toList();
        }

        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        List<ReservedItem> reservedItems = new ArrayList<>();
        List<Order> savedOrders = new ArrayList<>();
        try {
            for (Cart.CartLine line : cart.getItems()) {
                String reservationId = reservationId(buyerId, request.checkoutKey(), line.getProductId());
                ProductSummary product = reserve(line, reservationId);
                reservedItems.add(new ReservedItem(product, line.getQuantity(), reservationId));
            }

            Map<String, Order> ordersBySeller = new LinkedHashMap<>();
            for (ReservedItem reserved : reservedItems) {
                ProductSummary product = reserved.product();
                BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(reserved.quantity()));
                Order order = ordersBySeller.computeIfAbsent(product.sellerId(), sellerId ->
                        createOrder(buyerId, sellerId, request));
                order.getItems().add(new Order.OrderItem(
                        product.id(), product.name(), product.category(), product.sellerId(),
                        reserved.reservationId(), product.price(), reserved.quantity(), lineTotal));
                order.setTotal(order.getTotal().add(lineTotal));
            }

            for (Order order : ordersBySeller.values()) {
                savedOrders.add(orderRepository.save(order));
            }
            cartRepository.deleteByBuyerId(buyerId);
            return savedOrders.stream().map(this::toResponse).toList();
        } catch (RuntimeException exception) {
            if (exception instanceof DuplicateKeyException) {
                List<Order> duplicateOrders = orderRepository.findByBuyerIdAndCheckoutKey(buyerId, request.checkoutKey());
                if (!duplicateOrders.isEmpty()) {
                    return duplicateOrders.stream().map(this::toResponse).toList();
                }
            }
            rollbackOrders(savedOrders, exception);
            releaseStock(reservedItems, exception);
            if (exception instanceof DuplicateKeyException) {
                throw new ApiException(HttpStatus.CONFLICT, "Checkout could not be completed");
            }
            throw exception;
        }
    }

    public List<OrderResponse> buyerOrders(
            String buyerId,
            String search,
            String status,
            String from,
            String to) {
        return findOrders("buyerId", buyerId, search, status, from, to);
    }

    public List<OrderResponse> sellerOrders(
            String sellerId,
            String search,
            String status,
            String from,
            String to) {
        return findOrders("sellerId", sellerId, search, status, from, to);
    }

    public OrderResponse getOrder(String orderId, String buyerId) {
        Order order = findOrder(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this order");
        }
        return toResponse(order);
    }

    public OrderResponse cancelOrder(String orderId, String buyerId) {
        Order order = findOrder(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot cancel this order");
        }
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only placed orders can be cancelled");
        }

        for (Order.OrderItem item : order.getItems()) {
            productClient.release(item.getProductId(), item.getReservationId());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        return toResponse(orderRepository.save(order));
    }

    public void archiveOrder(String orderId, String buyerId) {
        Order order = findOrder(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot remove this order");
        }
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.DELIVERED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only cancelled or delivered orders can be removed");
        }
        order.setArchived(true);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }

    public OrderResponse updateSellerStatus(String orderId, String sellerId, OrderStatus nextStatus) {
        Order order = findOrder(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot update this order");
        }

        OrderStatus expectedNext = switch (order.getStatus()) {
            case PLACED -> OrderStatus.PROCESSING;
            case PROCESSING -> OrderStatus.SHIPPED;
            case SHIPPED -> OrderStatus.DELIVERED;
            default -> null;
        };

        if (expectedNext == null || expectedNext != nextStatus) {
            throw new ApiException(HttpStatus.CONFLICT, "Invalid order status transition");
        }

        order.setStatus(nextStatus);
        order.setUpdatedAt(Instant.now());
        return toResponse(orderRepository.save(order));
    }

    private ProductSummary reserve(Cart.CartLine line, String reservationId) {
        try {
            return productClient.reserve(line.getProductId(), line.getQuantity(), reservationId);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new ApiException(HttpStatus.CONFLICT, "Insufficient stock for product " + line.getProductId());
            }
            if (exception.getStatusCode().value() == 404) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Product not found: " + line.getProductId());
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Product service could not reserve stock");
        }
    }

    private List<OrderResponse> findOrders(
            String ownerField,
            String ownerId,
            String search,
            String status,
            String from,
            String to) {
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where(ownerField).is(ownerId).and("archived").is(false));
        if (status != null && !status.isBlank()) {
            try {
                filters.add(Criteria.where("status").is(OrderStatus.valueOf(status.trim().toUpperCase())));
            } catch (IllegalArgumentException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid order status filter");
            }
        }

        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Start date must not be after end date");
        }
        if (fromDate != null || toDate != null) {
            Criteria dateCriteria = Criteria.where("createdAt");
            if (fromDate != null) {
                dateCriteria.gte(fromDate.atStartOfDay(ZoneOffset.UTC).toInstant());
            }
            if (toDate != null) {
                dateCriteria.lt(toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
            }
            filters.add(dateCriteria);
        }

        if (search != null && !search.isBlank()) {
            if (search.length() > 100) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Search text must not exceed 100 characters");
            }
            String safeSearch = java.util.regex.Pattern.quote(search.trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("_id").regex(safeSearch, "i"),
                    Criteria.where("items.productName").regex(safeSearch, "i")));
        }

        Query query = Query.query(new Criteria().andOperator(filters.toArray(Criteria[]::new)))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Order.class).stream().map(this::toResponse).toList();
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Dates must use YYYY-MM-DD format");
        }
    }

    private Order createOrder(String buyerId, String sellerId, CheckoutRequest request) {
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setItems(new ArrayList<>());
        DeliveryAddressRequest address = request.deliveryAddress();
        order.setDeliveryAddress(new Order.DeliveryAddress(
                address.recipientName(), address.phone(), address.street(), address.city(), address.postalCode()));
        order.setTotal(BigDecimal.ZERO);
        order.setCheckoutKey(request.checkoutKey());
        return order;
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(), order.getBuyerId(), order.getSellerId(),
                order.getItems().stream().map(item -> new OrderResponse.OrderItemResponse(
                        item.getProductId(), item.getProductName(), item.getCategory(), item.getSellerId(),
                        item.getUnitPrice(), item.getQuantity(), item.getLineTotal())).toList(),
                order.getDeliveryAddress(), order.getTotal(), order.getPaymentMethod(),
                order.getStatus(), order.getCreatedAt());
    }

    private void rollbackOrders(List<Order> orders, RuntimeException originalException) {
        for (Order order : orders) {
            if (order.getId() != null) {
                try {
                    orderRepository.deleteById(order.getId());
                } catch (RuntimeException rollbackException) {
                    originalException.addSuppressed(rollbackException);
                }
            }
        }
    }

    private void releaseStock(List<ReservedItem> items, RuntimeException originalException) {
        for (ReservedItem item : items) {
            try {
                productClient.release(item.product().id(), item.reservationId());
            } catch (RuntimeException releaseException) {
                originalException.addSuppressed(releaseException);
            }
        }
    }

    private String reservationId(String buyerId, String checkoutKey, String productId) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((buyerId + ":" + checkoutKey + ":" + productId)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record ReservedItem(ProductSummary product, int quantity, String reservationId) {
    }
}
