package com.ibosng.lhrservice.services.impl;

import com.ibosng.lhrservice.dtos.zeitdaten.AnfrageSuccessDto;
import com.ibosng.lhrservice.services.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.ibosng.dbibosservice.utils.Helpers.isNullOrBlank;

@Slf4j
@Component("lhrAsyncService")
@RequiredArgsConstructor
public class AsyncServiceImpl implements AsyncService {

    @Async("generalExecutor")
    @Override
    public <T> CompletableFuture<T> asyncExecutor(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }

    @Async("generalExecutor")
    @Override
    public <T> void asyncExecutorVoid(Runnable runnable) {
        runnable.run();
    }

    @Async("generalExecutor")
    @Override
    public CompletableFuture<ResponseEntity<AnfrageSuccessDto>> pollAuszahlungsanfrage(Supplier<ResponseEntity<AnfrageSuccessDto>> supplier, int maxAttempts) {
        int attempts = 0;
        while (attempts++ < maxAttempts) {
            ResponseEntity<AnfrageSuccessDto> response = supplier.get();
            if (response != null && response.getBody() != null && !isNullOrBlank(response.getBody().getStatus()) &&
                    "done".equalsIgnoreCase(response.getBody().getStatus())) {
                log.info("Poll finished");
                return CompletableFuture.completedFuture(response);
            }
            try {
                Thread.sleep(1000); // wait 1 sec between polls
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted");
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        }
        log.error("Timed out");
        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build());
    }
}
