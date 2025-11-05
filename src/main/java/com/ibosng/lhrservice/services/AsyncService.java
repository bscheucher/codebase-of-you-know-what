package com.ibosng.lhrservice.services;

import com.ibosng.lhrservice.dtos.zeitdaten.AnfrageSuccessDto;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface AsyncService {
    @Async("generalExecutor")
    <T> CompletableFuture<T> asyncExecutor(Supplier<T> supplier);

    @Async("generalExecutor")
    <T> void asyncExecutorVoid(Runnable runnable);

    @Async("generalExecutor")
    CompletableFuture<ResponseEntity<AnfrageSuccessDto>> pollAuszahlungsanfrage(Supplier<ResponseEntity<AnfrageSuccessDto>> supplier, int maxAttempts);
}
