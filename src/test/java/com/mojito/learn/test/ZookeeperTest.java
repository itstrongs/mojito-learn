package com.mojito.learn.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author liufq
 * @since 2023-07-11 15:59:51
 */
@Slf4j
public class ZookeeperTest {

    private static final String ZK_ADDR = "10.20.140.86:2181";

    private static final int ZK_SESSION_TIMEOUT = 10000;

    @Test
    public void test() throws IOException, InterruptedException {
        System.setProperty("java.security.auth.login.config", "/Users/mojito/Repositories/mojito-parent/mojito-learn/src/main/resources/zk_jaas.conf");
        log.info("********************** start zk ..................");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ZooKeeper zooKeeper = new ZooKeeper(ZK_ADDR, ZK_SESSION_TIMEOUT, event -> {
            log.info("%%%%%%%%%%%%%%%%%%%%%触发了事件：[{}]", event);
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }
}
