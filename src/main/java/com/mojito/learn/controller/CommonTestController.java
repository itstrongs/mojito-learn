package com.mojito.learn.controller;

import com.mojito.learn.util.EtcdUtils;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

/**
 * @author liufq
 * @since 2023-07-04 14:38:32
 */
@RestController
@RequestMapping("/test")
@Slf4j
public class CommonTestController {

    public static final String UEAGENT_INFO_KEY = "/";

    @Resource
    private Client etcdClient;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/etcd")
    public String etcd() {
        List<KeyValue> kvs = EtcdUtils.getResponse(etcdClient.getKVClient(), UEAGENT_INFO_KEY, true).getKvs();
        String value = EtcdUtils.getFirstOrDefault(kvs, null);
        log.info("查询etcd key={}, value={}", UEAGENT_INFO_KEY, value);
        return value;
    }

    @GetMapping("/kafka")
    public String kafka() {
        kafkaTemplate.send("topic_test_0706", UUID.randomUUID().toString());
        return "发送成功";
    }
}
