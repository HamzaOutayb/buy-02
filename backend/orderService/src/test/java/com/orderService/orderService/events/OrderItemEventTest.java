package com.orderService.orderService.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class OrderItemEventTest {

    @Test
    void testTwoArgConstructor() {
        OrderItemEvent event = new OrderItemEvent("prod-123", 5);
        assertNull(event.orderId());
        assertEquals("prod-123", event.productId());
        assertEquals(5, event.quantity());
        assertEquals("DECREASE", event.operation());
    }

    @Test
    void testFourArgConstructor() {
        OrderItemEvent event = new OrderItemEvent("ord-1", "prod-123", 5, "RESTORE");
        assertEquals("ord-1", event.orderId());
        assertEquals("prod-123", event.productId());
        assertEquals(5, event.quantity());
        assertEquals("RESTORE", event.operation());
    }
}
