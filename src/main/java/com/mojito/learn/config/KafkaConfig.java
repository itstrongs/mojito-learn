package com.mojito.learn.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liufq
 * @since 2023-07-06 16:08:16
 */
@Configuration
@EnableKafka
public class KafkaConfig {

//    /**
//     * 定义一个KafkaAdmin的bean，可以自动检测集群中是否存在topic，不存在则创建
//     */
//    @Bean
//    public KafkaAdmin kafkaAdmin() {
//        Map<String, Object> configs = new HashMap<>();
//        // 指定多个kafka集群多个地址，例如：192.168.2.11,9092,192.168.2.12:9092,192.168.2.13:9092
//        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "10.20.140.86:9091,10.20.140.86:9092,10.20.140.86:9093");
//        return new KafkaAdmin(configs);
//    }
//
//    /**
//     * 创建 Topic
//     */
//    @Bean
//    public NewTopic topicinfo() {
//        // 创建topic，需要指定创建的topic的"名称"、"分区数"、"副本数量(副本数数目设置要小于Broker数量)"
//        return new NewTopic("test", 3, (short) 1);
//    }

    /**
     * Producer Template 配置
     */
    @Bean(name = "kafkaTemplate")
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Producer 工厂配置
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * Producer 参数配置
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        // 指定多个kafka集群多个地址
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "10.20.140.86:19091,10.20.140.86:19092,10.20.140.86:19093");

        // 重试次数，0为不启用重试机制
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        //同步到副本, 默认为1
        // acks=0 把消息发送到kafka就认为发送成功
        // acks=1 把消息发送到kafka leader分区，并且写入磁盘就认为发送成功
        // acks=all 把消息发送到kafka leader分区，并且leader分区的副本follower对消息进行了同步就任务发送成功
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        // 生产者空间不足时，send()被阻塞的时间，默认60s
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 6000);
        // 控制批处理大小，单位为字节
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 4096);
        // 批量发送，延迟为1毫秒，启用该功能能有效减少生产者发送消息次数，从而提高并发量
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        // 生产者可以使用的总内存字节来缓冲等待发送到服务器的记录
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 40960);
        // 消息的最大大小限制,也就是说send的消息大小不能超过这个限制, 默认1048576(1MB)
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1048576);
        // 键的序列化方式
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 值的序列化方式
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 压缩消息，支持四种类型，分别为：none、lz4、gzip、snappy，默认为none。
        // 消费者默认支持解压，所以压缩设置在生产者，消费者无需设置。
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");

//        props.put("security.protocol", "SSL");
//        props.put("ssl.truststore.location", "/tmp/ssl/client2/kafka.client.truststore.jks");
//        props.put("ssl.truststore.password", "123456");
//
//        props.put("ssl.keystore.location", "/tmp/ssl/client2/kafka.client.keystore.jks");
//        props.put("ssl.keystore.password", "123456");
//        props.put("ssl.key.password", "123456");
        return props;
    }
}
