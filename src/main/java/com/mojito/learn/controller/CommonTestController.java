package com.mojito.learn.controller;

import com.mojito.learn.service.EtcdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * @author liufq
 * @since 2023-07-04 14:38:32
 */
@RestController
@RequestMapping("/test")
@Slf4j
public class CommonTestController {

    @Resource
    private EtcdService etcdService;

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/etcd/get")
    public String etcd() {
        return etcdService.getValue("/");
    }

    @GetMapping("/etcd/put")
    public void etcd(@RequestParam String key, @RequestParam String value) {
        etcdService.putValue(key,value);
    }

    @GetMapping("/kafka")
    public String kafka() {
        kafkaTemplate.send("topic_test_0706", UUID.randomUUID().toString());
        return "发送成功";
    }
}
