package com.mojito.learn.controller;

import com.mojito.learn.service.EtcdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

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

    private static final String ZK_ADDR = "10.20.140.86:2181";

    private static final int ZK_SESSION_TIMEOUT = 10000;

    @GetMapping("/etcd/get")
    public String etcd() {
        return etcdService.getValue("/");
    }

    @GetMapping("/etcd/put")
    public void etcd(@RequestParam String key, @RequestParam String value) {
        etcdService.putValue(key,value);
    }

    @GetMapping("/kafka")
    public String kafka() throws InterruptedException, IOException {
        kafkaTemplate.send("topic_test_0706", UUID.randomUUID().toString());

        System.setProperty("java.security.auth.login.config", "/Users/mojito/Repositories/mojito-parent/mojito-learn/src/main/resources/zk_jaas.conf");
        log.info("********************** start zk ..................");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ZooKeeper zooKeeper = new ZooKeeper(ZK_ADDR, ZK_SESSION_TIMEOUT, event -> {
            log.info("%%%%%%%%%%%%%%%%%%%%%触发了事件：[{}]", event);
            countDownLatch.countDown();
        });
        countDownLatch.await();
        return "发送成功";
    }
}
