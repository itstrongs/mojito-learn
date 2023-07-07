package com.mojito.learn.config;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liufq
 * @since 2022-08-09 19:43:56
 */
@Configuration
public class EtcdConfig {

    @Value("${ueagent.etcd.endpoints:https://etcdserver1.com:42379}")
    private List<String> endpoints;

    @Value("${ueagent.etcd.user:root}")
    private String user;

    @Value("${ueagent.etcd.password:b191204641c3}")
    private String password;

    @Bean
    public Client client() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("ca/ca.pem");
        InputStream inputStream = classPathResource.getInputStream();

        SslContext sslContext = GrpcSslContexts
                .forClient()
                .sslProvider(SslProvider.OPENSSL)
                .trustManager(inputStream)
                .build();

        return Client.builder()
                .connectTimeout(Duration.ofSeconds(30))
                .endpoints(endpoints.stream().map(URI::create).collect(Collectors.toList()))
                .user(ByteSequence.from(user.getBytes(StandardCharsets.UTF_8)))
                .password(ByteSequence.from(password.getBytes(StandardCharsets.UTF_8)))
                .sslContext(sslContext)
                .retryMaxDuration(Duration.ofDays(1))
                .build();
    }
}
