package com.ibosng.validationservice.services;

import com.ibosng.dbservice.dtos.ZeitbuchungenDto;
import com.ibosng.dbservice.entities.masterdata.Personalnummer;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface AsyncService {
    @Async
    CompletableFuture<Boolean> zeitbuchungOverlapCheck(Personalnummer personalnummer, LocalDate von, LocalDate bis);

    @Async
    <T> CompletableFuture<T> asyncExecutor(Supplier<T> supplier);

    @Async
    CompletableFuture<List<ZeitbuchungenDto>> processLeistungsdatum(Personalnummer personalnummer, String datum, List<ZeitbuchungenDto> zeitbuchungenDto);
}
