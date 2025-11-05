package com.ibosng.gatewayservice.services;

import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface AsyncService {
    @Async
    <T> CompletableFuture<T> asyncExecutor(Supplier<T> supplier);
}
