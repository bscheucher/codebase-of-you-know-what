package com.ibosng.gatewayservice.services.impl;

import com.ibosng.gatewayservice.services.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Slf4j
@Component("gatewayAsyncService")
@RequiredArgsConstructor
public class AsyncServiceImpl implements AsyncService {
    @Async
    @Override
    public <T> CompletableFuture<T> asyncExecutor(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }

}
