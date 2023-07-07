package com.mojito.learn.service;

import com.mojito.learn.util.BusinessException;
import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author liufq
 * @since 2023-07-07 09:59:26
 */
@Service
@Slf4j
public class EtcdService {

    @Value("${ueagent.etcd.endpoints:https://etcdserver1.com:42379}")
    private List<String> endpoints;

    @Value("${ueagent.etcd.user:root}")
    private String user;

    @Value("${ueagent.etcd.password:b191204641c3}")
    private String password;

    private Client client;

    public Client getClient() {
        if (client != null) {
            return client;
        }

        try {
            log.info("etcd client not exist, recreate...");
            ClassPathResource classPathResource = new ClassPathResource("ca/ca.pem");
            InputStream inputStream = classPathResource.getInputStream();

            SslContext sslContext = GrpcSslContexts
                    .forClient()
                    .sslProvider(SslProvider.OPENSSL)
                    .trustManager(inputStream)
                    .build();

            client = Client.builder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .endpoints(endpoints.stream().map(URI::create).collect(Collectors.toList()))
                    .user(ByteSequence.from(user.getBytes(StandardCharsets.UTF_8)))
                    .password(ByteSequence.from(password.getBytes(StandardCharsets.UTF_8)))
                    .sslContext(sslContext)
                    .retryMaxDuration(Duration.ofDays(1))
                    .build();
            return client;
        } catch (Exception e) {
            throw new BusinessException("etcd connect exception", e);
        }
    }

    public String getValue(String key) {
        return getValue(key, 0);
    }

    public String getValue(String key, int retry) {
        try {
            GetResponse response = getClient().getKVClient().get(getByteSequenceKey(key),
                    GetOption.newBuilder().isPrefix(true).build()).get(10, TimeUnit.SECONDS);
            List<KeyValue> keyValues = response.getKvs();
            String value = getFirstOrDefault(keyValues);
            log.info("query etcd key={}, value={}", key, value);
            return value;
        } catch (Exception e) {
            if (retry > 10) {
                throw new BusinessException("etcd connect timeout", e);
            }

            log.error("etcd query exception, retry again {} times", ++retry);
            // 重试10次，每次隔10秒
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            client = null;
            return getValue(key, retry);
        }
    }

    private String getFirstOrDefault(List<KeyValue> keyValues) {
        return keyValues.stream().map(value -> value.getValue().toString(StandardCharsets.UTF_8)).findFirst().orElse(null);
    }

    private ByteSequence getByteSequenceKey(String key) {
        return ByteSequence.from(key, StandardCharsets.UTF_8);
    }

    /**
     * etcd增加初始化watch
     */
    public void initWatch() {
        log.info("add etcd init watch");
        String key = "/test/0707";
        getClient().getWatchClient().watch(getByteSequenceKey(key), response -> {
            log.info("watch result: {}", response);
        }, throwable -> {
            log.error("watch exception", throwable);
        });
    }

    public void putValue(String key, String value) {
        putValue(getClient().getLeaseClient(), getClient().getKVClient(), key, value, 10L);
        log.info("etcd put success");
    }

    private void putValue(Lease leaseClient, KV kvClient, String key, String value, Long ttl) {
        try {
            if (ttl != 0L) {
                long leaseId = leaseClient.grant(ttl).get().getID();
                kvClient.put(getByteSequenceKey(key), getByteSequenceValue(value), PutOption.newBuilder().withLeaseId(leaseId).build()).get();
            } else {
                kvClient.put(getByteSequenceKey(key), getByteSequenceValue(value)).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("写值失败", e);
        }
    }

    private ByteSequence getByteSequenceValue(String value) {
        return ByteSequence.from(value, StandardCharsets.UTF_8);
    }
}
