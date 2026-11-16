package com.orderService.orderService.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class OrderTopic {
    public static final String NAME = "order-events";

    @Bean
    public NewTopic orderTopicBean() {
        return TopicBuilder.name(NAME)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
