package com.product.productService.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class InventoryTopic {
    public static final String NAME = "inventory-events";

    @Bean
    public NewTopic inventoryTopicBean() {
        return TopicBuilder.name(NAME)
                .partitions(3)
                .replicas(1)
                .build();
    }
}