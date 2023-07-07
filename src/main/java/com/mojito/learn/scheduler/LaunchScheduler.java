package com.mojito.learn.scheduler;

import com.mojito.learn.service.EtcdService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author liufq
 * @since 2023-07-07 15:20:08
 */
@Component
public class LaunchScheduler implements CommandLineRunner {

    @Resource
    private EtcdService etcdService;

    @Override
    public void run(String... args) {
        etcdService.initWatch();
    }
}
