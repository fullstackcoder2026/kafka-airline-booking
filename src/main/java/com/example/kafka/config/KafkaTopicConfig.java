package com.example.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topic.booking.problem}")
    private String problemTopic;

    @Value("${kafka.topic.booking.solved}")
    private String solvedTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic bookingProblemTopic() {
        // Single partition - will cause ordering issues
        return new NewTopic(problemTopic, 1, (short) 1);
    }

    @Bean
    public NewTopic bookingSolvedTopic() {
        // Multiple partitions - ordering maintained per booking ID
        return new NewTopic(solvedTopic, 3, (short) 1);
    }
}
