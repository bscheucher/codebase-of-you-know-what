package com.ibosng.validationservice.services.impl;

import com.ibosng.dbservice.dtos.ZeitbuchungenDto;
import com.ibosng.dbservice.entities.Zeitausgleich;
import com.ibosng.dbservice.entities.lhr.Abwesenheit;
import com.ibosng.dbservice.entities.masterdata.Personalnummer;
import com.ibosng.dbservice.entities.zeitbuchung.Leistungserfassung;
import com.ibosng.dbservice.entities.zeitbuchung.Zeitbuchung;
import com.ibosng.dbservice.services.ZeitausgleichService;
import com.ibosng.dbservice.services.lhr.AbwesenheitService;
import com.ibosng.dbservice.services.zeitbuchung.LeistungserfassungService;
import com.ibosng.dbservice.services.zeitbuchung.ZeitbuchungService;
import com.ibosng.validationservice.services.AsyncService;
import com.ibosng.validationservice.services.Validation2LHRService;
import com.ibosng.validationservice.services.ValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.ibosng.validationservice.utils.Constants.VALIDATION_SERVICE;

@Slf4j
@Component("validationAsyncService")
@RequiredArgsConstructor
public class AsyncServiceImpl implements AsyncService {

    private final Validation2LHRService validation2LHRService;
    private final AbwesenheitService abwesenheitService;
    private final ZeitbuchungService zeitbuchungService;
    private final LeistungserfassungService leistungserfassungService;
    private final ZeitausgleichService zeitausgleichService;
    private final ValidatorService validatorService;

    @Autowired
    @Lazy   //avoid circular dependency exception
    @org.springframework.beans.factory.annotation.Qualifier("validationAsyncService")
    private AsyncService self;    //for avoiding proxy-self-invocation problem

    @Async
    @Override
    public CompletableFuture<Boolean> zeitbuchungOverlapCheck(Personalnummer personalnummer, LocalDate von, LocalDate bis) {
        log.info("Overlapping check for [{}]-[{}] pn-{}", von, bis, personalnummer.getPersonalnummer());
        List<Leistungserfassung> leistungserfassungen = leistungserfassungService.findByPersonalnummerInPeriod(personalnummer, von.format(DateTimeFormatter.ISO_DATE), bis.format(DateTimeFormatter.ISO_DATE));

        List<Zeitbuchung> zeitbuchungen = leistungserfassungen.stream()
                .flatMap(leistungserfassung -> zeitbuchungService.getZeitbuchungenByListungserfassen(leistungserfassung).stream())
                .toList();

        for (Zeitbuchung zeitbuchung : zeitbuchungen) {
            if (Boolean.FALSE.equals(zeitbuchung.getAnAbwesenheit())) {
                continue;
            }
            List<Abwesenheit> abwesenheiten = abwesenheitService.findAbwesenheitBetweenDates(personalnummer.getId(), von, bis);
            List<Zeitausgleich> zeitausgleichList = zeitausgleichService.findByPersonalnummerInPeriod(personalnummer.getId(), von, bis);
            abwesenheiten
                    .stream()
                    .filter(abw ->
                            !zeitbuchung.getLeistungserfassung().getLeistungsdatum().isBefore(abw.getVon()) &&
                                    !zeitbuchung.getLeistungserfassung().getLeistungsdatum().isAfter(abw.getBis()))
                    .forEach(abw -> validation2LHRService.deleteAbwesenheit(abwesenheitService.mapToAbwesenheitDto(abw)));

            zeitausgleichList.forEach(z -> validation2LHRService.deleteZeitausgleich(z.getPersonalnummer().getId(), z.getDatum().format(DateTimeFormatter.ISO_DATE), true) );
        }
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    @Async
    @Override
    public <T> CompletableFuture<T> asyncExecutor(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }

    @Async
    @Override
    public CompletableFuture<List<ZeitbuchungenDto>> processLeistungsdatum(Personalnummer personalnummer, String datum, List<ZeitbuchungenDto> zeitbuchungenDto) {
        List<Leistungserfassung> leistungserfassungen = leistungserfassungService.findByPersonalnummerAndDate(personalnummer, datum);
        List<ZeitbuchungenDto> zeitbuchungenDtos = new ArrayList<>();

        for (Leistungserfassung leistungserfassung : leistungserfassungen) {
            if (Boolean.TRUE.equals(leistungserfassung.getIsSyncedWithLhr())) {
                validation2LHRService.deleteLeistungserfassung(personalnummer.getId(), leistungserfassung.getLeistungsdatum().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            List<Zeitbuchung> zeitbuchungsToDelete = zeitbuchungService.getZeitbuchungenByListungserfassen(leistungserfassung);
            zeitbuchungsToDelete.forEach(zeitbuchung -> zeitbuchungService.deleteById(zeitbuchung.getId()));
        }

        for (ZeitbuchungenDto zeitbuchungDto : zeitbuchungenDto) {
            zeitbuchungenDtos.add(validatorService.validateZeitbuchung(zeitbuchungDto, VALIDATION_SERVICE).getBody());
            self.asyncExecutor(() -> {
                HttpStatusCode result = validation2LHRService.syncLeistungerfassung(personalnummer.getId(), zeitbuchungDto.getLeistungsdatum()).getStatusCode();
                if (result.is2xxSuccessful()) {
                    log.info("Successfully synced with lhr pn-{}, {}", personalnummer, zeitbuchungDto.getLeistungsdatum());
                } else {
                    log.error("Failed to sync with lhr pn-{}, {}, status: {}", personalnummer, zeitbuchungDto.getLeistungsdatum(), result);
                }
                return result.is2xxSuccessful();
            });
        }

        return CompletableFuture.completedFuture(zeitbuchungenDtos);
    }
}
