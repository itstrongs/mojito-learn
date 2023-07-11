package com.mojito.learn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author liufq
 * @since 2023-07-10 10:54:43
 */
@Component
@Slf4j
public class KafkaConsumer {

    @KafkaListener(topics = "topic_test_0706",groupId = "mojito-learn-kafka-consumer2")
    public void consumer(String content) {
        log.info("收到Kafka消息：{}", content);
    }
}
