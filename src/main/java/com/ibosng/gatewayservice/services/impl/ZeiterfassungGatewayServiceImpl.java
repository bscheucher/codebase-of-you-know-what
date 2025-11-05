package com.ibosng.gatewayservice.services.impl;

import com.ibosng.dbibosservice.services.AdresseIbosService;
import com.ibosng.dbmapperservice.services.ZeitbuchungenMapperService;
import com.ibosng.dbservice.dtos.ZeitausgleichDto;
import com.ibosng.dbservice.dtos.ZeitbuchungMetadataDto;
import com.ibosng.dbservice.dtos.ZeitbuchungSyncRequestDto;
import com.ibosng.dbservice.dtos.ZeitbuchungenDto;
import com.ibosng.dbservice.dtos.mitarbeiter.AbwesenheitDto;
import com.ibosng.dbservice.dtos.urlaubsdaten.AbwesenheitOverviewDto;
import com.ibosng.dbservice.dtos.zeiterfassung.umbuchung.UmbuchungDto;
import com.ibosng.dbservice.entities.Benutzer;
import com.ibosng.dbservice.entities.Zeitausgleich;
import com.ibosng.dbservice.entities.lhr.Abwesenheit;
import com.ibosng.dbservice.entities.lhr.AbwesenheitStatus;
import com.ibosng.dbservice.entities.masterdata.Personalnummer;
import com.ibosng.dbservice.entities.zeitbuchung.Leistungserfassung;
import com.ibosng.dbservice.entities.zeitbuchung.MoxisStatus;
import com.ibosng.dbservice.entities.zeitbuchung.Zeitbuchung;
import com.ibosng.dbservice.services.BenutzerService;
import com.ibosng.dbservice.services.ZeitausgleichService;
import com.ibosng.dbservice.services.lhr.AbwesenheitService;
import com.ibosng.dbservice.services.mitarbeiter.PersonalnummerService;
import com.ibosng.dbservice.services.zeitbuchung.LeistungserfassungService;
import com.ibosng.dbservice.services.zeitbuchung.ZeitbuchungService;
import com.ibosng.gatewayservice.config.GatewayUserHolder;
import com.ibosng.gatewayservice.dtos.GenehmigungDto;
import com.ibosng.gatewayservice.dtos.response.Pagination;
import com.ibosng.gatewayservice.dtos.response.PayloadResponse;
import com.ibosng.gatewayservice.dtos.response.PayloadTypeList;
import com.ibosng.gatewayservice.enums.PayloadTypes;
import com.ibosng.gatewayservice.exceptions.BusinessLogicException;
import com.ibosng.gatewayservice.services.*;
import com.ibosng.lhrservice.services.LHRUrlaubService;
import com.ibosng.lhrservice.services.LHRZeitdatenService;
import com.ibosng.lhrservice.services.LHRZeiterfassungService;
import com.ibosng.lhrservice.services.SchedulerService;
import com.ibosng.microsoftgraphservice.services.MailService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ibosng.dbservice.utils.Constants.*;
import static com.ibosng.dbservice.utils.Helpers.localDateTimeToString;
import static com.ibosng.dbservice.utils.Parsers.*;
import static com.ibosng.gatewayservice.utils.Constants.*;
import static com.ibosng.gatewayservice.utils.Helpers.*;
import static com.ibosng.gatewayservice.utils.Parsers.isValidDate;
import static com.ibosng.gatewayservice.utils.Parsers.parseDate;
import static com.ibosng.microsoftgraphservice.utils.Helpers.toObjectArray;


@Service
@Slf4j
public class ZeiterfassungGatewayServiceImpl implements ZeiterfassungGatewayService {

    private final MailService mailService;
    private final BenutzerDetailsService benutzerDetailsService;
    private final ZeitbuchungenMapperService zeitbuchungenMapper;
    private final LeistungserfassungService leistungserfassungService;
    private final ZeitbuchungService zeitbuchungService;
    private final Gateway2Validation gateway2Validation;
    private final PersonalnummerService personalnummerService;
    private final AbwesenheitService abwesenheitService;
    private final ZeitausgleichService zeitausgleichService;
    private final BenutzerService benutzerService;
    private final AdresseIbosService adresseIbosService;
    private final EnvironmentService environmentService;
    private final AsyncService asyncService;
    private final GatewayUserHolder gatewayUserHolder;
    private final LHRUrlaubService lhrUrlaubService;
    private final LHRZeiterfassungService lhrZeiterfassungService;
    private final SchedulerService schedulerService;
    private final LHRZeitdatenService lhrZeitdatenService;


    @Getter
    @Value("${nextAuthUrl:#{null}}")
    private String nextAuthUrl;

    public ZeiterfassungGatewayServiceImpl(MailService mailService,
                                           BenutzerDetailsService benutzerDetailsService,
                                           ZeitbuchungenMapperService zeitbuchungenMapper,
                                           LeistungserfassungService leistungserfassungService,
                                           ZeitbuchungService zeitbuchungService,
                                           Gateway2Validation gateway2Validation,
                                           PersonalnummerService personalnummerService,
                                           AbwesenheitService abwesenheitService,
                                           ZeitausgleichService zeitausgleichService,
                                           BenutzerService benutzerService,
                                           AdresseIbosService adresseIbosService,
                                           EnvironmentService environmentService,
                                           @Qualifier("gatewayAsyncService") AsyncService asyncService,
                                           GatewayUserHolder gatewayUserHolder,
                                           LHRUrlaubService lhrUrlaubService,
                                           LHRZeiterfassungService lhrZeiterfassungService,
                                           SchedulerService schedulerService,
                                           LHRZeitdatenService lhrZeitdatenService) {
        this.mailService = mailService;
        this.benutzerDetailsService = benutzerDetailsService;
        this.zeitbuchungenMapper = zeitbuchungenMapper;
        this.leistungserfassungService = leistungserfassungService;
        this.zeitbuchungService = zeitbuchungService;
        this.gateway2Validation = gateway2Validation;
        this.personalnummerService = personalnummerService;
        this.abwesenheitService = abwesenheitService;
        this.zeitausgleichService = zeitausgleichService;
        this.benutzerService = benutzerService;
        this.adresseIbosService = adresseIbosService;
        this.environmentService = environmentService;
        this.asyncService = asyncService;
        this.gatewayUserHolder = gatewayUserHolder;
        this.lhrUrlaubService = lhrUrlaubService;
        this.lhrZeiterfassungService = lhrZeiterfassungService;
        this.schedulerService = schedulerService;
        this.lhrZeitdatenService = lhrZeitdatenService;
    }

    /**
     * Remove Abwesenheit (URLAU, SURL, UNRUL, ZEITAUSGLEICH) and send mail to Führungskraft that
     * an already accepted Abwesenheit was rejected by their employee.
     *
     * @param abwesenheitId
     * @return PayloadResponse of Abwesenheitdto
     */
    @Override
    public PayloadResponse deleteAbwesenheit(Integer abwesenheitId) {
        Optional<Abwesenheit> abwesenheitOptional = abwesenheitService.findById(abwesenheitId);
        if (abwesenheitOptional.isPresent()) {
            return abwesenheitOptional.filter(abw -> AbwesenheitStatus.ACCEPTED.equals(abw.getStatus()))
                    .map(abw -> {
                        abw.setStatus(AbwesenheitStatus.REQUEST_CANCELLATION);
                        return abwesenheitService.save(abw);
                    })
                    .map(abwesenheitService::mapToAbwesenheitDto)
                    .map(abw ->
                    {
                        List<AbwesenheitDto> listOfAbw = List.of(abw);
                        mailZeitausgleichCancellation(listOfAbw);
                        return PayloadResponse.builder()
                                .success(true)
                                .data(List.of(new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(), listOfAbw)))
                                .build();
                    })
                    .orElse(PayloadResponse.builder().success(false).message("Wrong urlaube status").build());
        } else {
            List<Zeitausgleich> zeitausgleichList = zeitausgleichService.findAllZeitausgleichInPeriod(abwesenheitId);
            if (zeitausgleichList.isEmpty()) {
                return PayloadResponse.builder().success(false).message("No abwesenheit found found").build();
            }

            for (Zeitausgleich zeitausgleich : zeitausgleichList) {
                if (!AbwesenheitStatus.ACCEPTED.equals(zeitausgleich.getStatus())) {
                    return PayloadResponse.builder().success(false).message("Wrong zeitausgleich status").build();
                }
            }

            for (Zeitausgleich zeitausgleich : zeitausgleichList) {
                zeitausgleich.setStatus(AbwesenheitStatus.REQUEST_CANCELLATION);
                zeitausgleichService.save(zeitausgleich);
            }

            List<AbwesenheitDto> abwesenheitDto = zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(zeitausgleichList);

            mailZeitausgleichCancellation(abwesenheitDto);

            return PayloadResponse.builder()
                    .success(true)
                    .data(List.of(new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(),
                            abwesenheitDto)))
                    .build();
        }
    }

    /**
     * Delete Zeitausgleich and send email to employee that storno  was successful.
     *
     * @param zeitausgleicheToSend
     * @param comment
     * @return PayloadResponse AbwesenheitDto
     */
    private PayloadResponse deleteZeitausgleich(List<Zeitausgleich> zeitausgleicheToSend, String comment) {
        boolean result = zeitausgleicheToSend.stream().map(zeitausgleich -> deleteZeitausgleich(
                        zeitausgleich.getPersonalnummer().getId(),
                        zeitausgleich.getDatum().format(DateTimeFormatter.ISO_DATE)).isSuccess())
                .reduce(true, (subtotal, element) -> subtotal && element);

        List<Zeitausgleich> updated = zeitausgleicheToSend.stream().map(Zeitausgleich::getId)
                .flatMap(id -> zeitausgleichService.findByIdForceUpdate(id).stream())
                .toList();

        if (!isNullOrBlank(comment)) {
            updated.forEach(zeitausgleich -> zeitausgleich.setCommentFuehrungskraft(comment));
            zeitausgleichService.saveAll(updated);
        }

        List<AbwesenheitDto> abwesenheitDtoList = zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(updated);

        AbwesenheitDto abwesenheitDto = abwesenheitDtoList.stream().findFirst().orElse(null);

        if (abwesenheitDto != null) {
            Benutzer benutzer = benutzerService.findByPersonalnummerId(abwesenheitDto.getPersonalnummerId());

            String email = benutzer.getEmail();

            mailService.sendEmail("gateway-service.ma-abwesenheit-storno-erfolgreich",
                    "german",
                    null,
                    new String[]{email},
                    toObjectArray(),
                    toObjectArray(abwesenheitDto.getFullName(),
                            abwesenheitDto.getStartDate(),
                            abwesenheitDto.getEndDate(),
                            MEINE_ABWESENHEITEN_LINK.formatted(nextAuthUrl)));

        }

        return PayloadResponse.builder()
                .success(result)
                .data(Collections.singletonList(new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(), List.of(abwesenheitDtoList))))
                .build();
    }

    /**
     * Send email to employee that their Abwesenheit (URLAU, SURL, UNURL) storno was accepted.
     *
     * @param abwesenheitDto
     * @param comment
     * @return PayloadType AbwesenheitDto
     */
    private PayloadResponse deleteAbwesenheit(AbwesenheitDto abwesenheitDto, String comment) {
        ResponseEntity result = lhrUrlaubService.deleteUrlaub(abwesenheitDto, null, null);

        AbwesenheitDto dbDto = abwesenheitService
                .findByIdForceUpdate(abwesenheitDto.getId())
                .map(abw -> {
                    abw.setCommentFuehrungskraft(comment);
                    return abwesenheitService.save(abw);
                })
                .map(abwesenheitService::mapToAbwesenheitDto)
                .orElse(null);

        PayloadTypeList<AbwesenheitDto> abwesenheitDtoPayloadType = new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue());

        if (dbDto != null && AbwesenheitStatus.ACCEPTED.equals(dbDto.getStatus())) {
            String name = abwesenheitDto.getFullName();

            Benutzer benutzer = benutzerService.findByPersonalnummerId(abwesenheitDto.getPersonalnummerId());

            String email = benutzer.getEmail();

            mailService.sendEmail("gateway-service.ma-abwesenheit-storno-erfolgreich",
                    "german",
                    null,
                    new String[]{email},
                    toObjectArray(),
                    toObjectArray(name, abwesenheitDto.getStartDate(), abwesenheitDto.getEndDate(),
                            MEINE_ABWESENHEITEN_LINK.formatted(nextAuthUrl)));


        }

        abwesenheitDtoPayloadType.setAttributes(Collections.singletonList(dbDto));

        return PayloadResponse.builder()
                .success(result.getStatusCode().is2xxSuccessful())
                .data(List.of(abwesenheitDtoPayloadType))
                .build();
    }

    @Override
    public PayloadResponse getAbwesenheit(Integer abwesenheitId) {
        Optional<AbwesenheitDto> abwesenheit = abwesenheitService.findById(abwesenheitId)
                .map(abwesenheitService::mapToAbwesenheitDto)
                .or(() ->
                        zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(
                                        zeitausgleichService.findAllZeitausgleichInPeriod(abwesenheitId))
                                .stream()
                                .findFirst()
                );

        if (abwesenheit.isPresent()) {
            AbwesenheitDto dto = abwesenheit.get();
            PayloadTypeList<AbwesenheitDto> abwesenheitDtoPayloadType = new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue());
            abwesenheitDtoPayloadType.setAttributes(Collections.singletonList(dto));
            return PayloadResponse.builder().success(true).data(Collections.singletonList(abwesenheitDtoPayloadType)).build();
        } else {
            log.error("Abwesenheit {} not found", abwesenheitId);
            return PayloadResponse.builder().success(false).build();
        }
    }

    /**
     * Employee creates their Abwesenheit (URLAU, SURL, UNURL).
     * Mail service then emails their Führungskraft saying a Abwesenheit has been created and awaits approval/denial.
     *
     * @param abwesenheitDto
     * @param personalnummer
     * @return PayloadResponse AbwesenheitDto
     */
    private PayloadResponse postAbwesenheit(AbwesenheitDto abwesenheitDto, Personalnummer personalnummer) {
        if (personalnummer == null) {
            personalnummer = personalnummerService.findById(abwesenheitDto.getId()).orElse(null);
        }
        if (personalnummer == null || isNullOrBlank(personalnummer.getPersonalnummer()) || personalnummer.getFirma() == null) {
            return PayloadResponse.builder().success(false).data(Collections.singletonList(
                    new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(), List.of(abwesenheitDto)))).build();
        }
        abwesenheitDto.setPersonalnummerId(personalnummer.getId());
        PayloadTypeList<AbwesenheitDto> abwesenheitDtoPayloadType = new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue());

        try {
            ResponseEntity<?> responseEntity = lhrUrlaubService.createUrlaub(null, null, abwesenheitDto);
            ResponseEntity<AbwesenheitDto> abwesenheitRequest = (ResponseEntity<AbwesenheitDto>) responseEntity;

            if (abwesenheitRequest.getStatusCode().equals(HttpStatusCode.valueOf(409))) {
                return PayloadResponse.builder().success(false).message("Für diesen Zeitraum existiert bereits eine Abwesenheit. Bitte wähle einen anderen Zeitraum.").build();
            }

            if (abwesenheitRequest.getBody() == null) {
                return PayloadResponse.builder().success(false).message("Something went wrong").build();
            }

            Optional<Abwesenheit> optional = abwesenheitService.findByIdForceUpdate(abwesenheitRequest.getBody().getId());
            if (optional.isEmpty()) {
                return PayloadResponse.builder().success(false).message("Something went wrong").build();
            }

            Abwesenheit abwesenheit = optional.get();
            Benutzer benutzer = benutzerService.findByPersonalnummerAndFirmaBmdClient(personalnummer.getPersonalnummer(), personalnummer.getFirma().getBmdClient());
            Benutzer fuehrungskraeft = getFuehrungskraefte(benutzer.getUpn());
            if (fuehrungskraeft != null) {
                abwesenheit.getFuehrungskraefte().add(fuehrungskraeft);
            }
            abwesenheit = abwesenheitService.save(abwesenheit);
            abwesenheitDtoPayloadType.setAttributes(Collections.singletonList(optional.map(abwesenheitService::mapToAbwesenheitDto).orElse(null)));
            AbwesenheitDto savedAbwesenheitDto = abwesenheitRequest.getBody();

            if (abwesenheit.getStatus().equals(AbwesenheitStatus.VALID)) {
                abwesenheit = optional.get();
                String[] emails = abwesenheit.getFuehrungskraefte().stream().map(Benutzer::getEmail).toArray(String[]::new);

                LocalDate startDate = savedAbwesenheitDto.getStartDate();
                LocalDate endDate = savedAbwesenheitDto.getEndDate();
                String nameMitarbeiter = savedAbwesenheitDto.getFullName();

                sendMailToFuehrungskraft(emails, startDate, endDate, nameMitarbeiter);
            }
            return PayloadResponse.builder().success(true).data(Collections.singletonList(abwesenheitDtoPayloadType)).build();
        } catch (Exception e) {
            log.error("Error sending Urlaub request to LHR: {}; Dto - {}", e.getMessage(), abwesenheitDto);
            if (abwesenheitDto.getId() != null) {
                abwesenheitDtoPayloadType.setAttributes(Collections.singletonList(abwesenheitService.findById(abwesenheitDto.getId()).map(abwesenheitService::mapToAbwesenheitDto).orElse(null)));
            } else {
                abwesenheitDtoPayloadType.setAttributes(Collections.singletonList(abwesenheitDto));
            }
            return PayloadResponse.builder().success(false).data(Collections.singletonList(abwesenheitDtoPayloadType)).build();
        }
    }

    private Benutzer getFuehrungskraefte(String email) {
        if (!environmentService.isProduction() && email.contains(TEST_TENANT_UPN_PREFIX)) {
            email = email.replace(TEST_TENANT_UPN_PREFIX, "");
        }

        String fuhrungskraefteUpn = adresseIbosService.getFuehrungskraftUPNFromLogin(email.split("@")[0]);
        Benutzer fuehrungskraft = null;

        if (!isNullOrBlank(fuhrungskraefteUpn)) {
            fuhrungskraefteUpn = fuhrungskraefteUpn.toLowerCase();
            fuehrungskraft = benutzerService.findByUpn(fuhrungskraefteUpn);

            if (fuehrungskraft == null) {
                // Sync the supervisor if not found
                gateway2Validation.validateSyncMitarbeiterWithUPN(fuhrungskraefteUpn);
                fuehrungskraft = benutzerService.findByUpn(fuhrungskraefteUpn); // Retry after sync
            }

        }
        return fuehrungskraft;
    }

    /**
     * Employee creates their Zeitausgleich. Mail service then emails their Führungskraft
     * saying a Zeitausgleich has been created and awaits approval/denial.
     *
     * @param abwesenheitDto
     * @param personalnummer
     * @return PayloadResponse of AbwesenheitDto
     */
    private PayloadResponse postZeitausgleich(AbwesenheitDto abwesenheitDto, Personalnummer personalnummer) {
        if ((personalnummer != null) && !isNullOrBlank(personalnummer.getPersonalnummer())) {
            abwesenheitDto.setPersonalnummerId(personalnummer.getId());
        }

        ResponseEntity<List<ZeitausgleichDto>> request = lhrZeiterfassungService.submitZeitausgleichForPeriod(abwesenheitDto);

        if (request.getStatusCode().equals(HttpStatus.CONFLICT)) {
            return PayloadResponse.builder().success(false).message("Für diesen Zeitraum existiert bereits eine Abwesenheit. Bitte wähle einen anderen Zeitraum.").build();
        }

        List<ZeitausgleichDto> postZeitausgleich = request.getBody();

        if (postZeitausgleich == null || postZeitausgleich.isEmpty()) {
            return PayloadResponse.builder()
                    .success(false)
                    .build();
        }

        List<Zeitausgleich> zeitausgleiche = zeitausgleichService.findAllByIdIn(postZeitausgleich.stream().map(ZeitausgleichDto::getId).collect(Collectors.toList()));
        zeitausgleiche.forEach(zeitausgleich -> {
            if ((personalnummer != null) && !isNullOrBlank(personalnummer.getPersonalnummer())) {
                Benutzer benutzer = benutzerService.findByPersonalnummerAndFirmaBmdClient(personalnummer.getPersonalnummer(), personalnummer.getFirma().getBmdClient());
                Benutzer fuehrungskraeft = getFuehrungskraefte(benutzer.getUpn());
                if (fuehrungskraeft != null) {
                    Set<Benutzer> fuehrungskraefteSet = new HashSet<>();
                    fuehrungskraefteSet.add(fuehrungskraeft);
                    zeitausgleich.setFuehrungskraefte(fuehrungskraefteSet);
                    zeitausgleichService.save(zeitausgleich);
                }
            }
        });

        List<AbwesenheitDto> abwesenheitDtos = zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(zeitausgleiche);

        if (AbwesenheitStatus.VALID.equals(abwesenheitDtos.stream().findFirst().map(AbwesenheitDto::getStatus).orElse(null))) {

            String[] emails = abwesenheitDtos.stream()
                    .flatMap(dto -> dto.getFuehrungskraefte().stream())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toArray(String[]::new);

            LocalDate startDate = abwesenheitDtos.stream().findFirst().map(AbwesenheitDto::getStartDate).orElse(null);
            LocalDate endDate = abwesenheitDtos.stream().findFirst().map(AbwesenheitDto::getEndDate).orElse(null);
            String nameMitarbeiter = abwesenheitDtos.stream().findFirst().map(AbwesenheitDto::getFullName).orElse(null);

            sendMailToFuehrungskraft(emails, startDate, endDate, nameMitarbeiter);
        }

        PayloadTypeList<AbwesenheitDto> abwesenheitDtoPayloadType = new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(),
                zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(zeitausgleiche));

        return PayloadResponse.builder().success(true).data(Collections.singletonList(abwesenheitDtoPayloadType)).build();
    }

    @Override
    public PayloadResponse resendAbwesenheit(Integer id, String token) {
        Personalnummer personalnummer = environmentService.getPersonalnummer();

        Optional<Abwesenheit> optional = abwesenheitService.findByIdForceUpdate(id);
        if (optional.isEmpty()) {
            return PayloadResponse.builder()
                    .success(false)
                    .message("Cannot find abwesenheit with %s".formatted(id))
                    .build();
        }

        Abwesenheit abwesenheit = optional.get();
        if ((abwesenheit.getPersonalnummer() == null) ||
                !AbwesenheitStatus.INVALID.equals(abwesenheit.getStatus()) ||
                !Objects.equals(abwesenheit.getPersonalnummer().getId(), personalnummer.getId())) {
            return PayloadResponse.builder()
                    .success(false)
                    .message("User not eligible to create abwesenheit")
                    .build();
        }

        AbwesenheitDto abwesenheitDto = abwesenheitService.mapToAbwesenheitDto(abwesenheit);
        abwesenheitDto.setDurationInDays(null);
        return saveAbwesenheit(abwesenheitDto, token);
    }

    @Override
    public PayloadResponse saveAbwesenheit(AbwesenheitDto abwesenheitDto, String token) {
        Personalnummer personalnummer = environmentService.getPersonalnummer();

        validateAbwesenheiten(abwesenheitDto);

        return switch (abwesenheitDto.getType()) {
            case ZEITAUSGLEICH -> postZeitausgleich(abwesenheitDto, personalnummer);
            case URLAU, SURL, UNURL -> postAbwesenheit(abwesenheitDto, personalnummer);
            default -> PayloadResponse.builder().success(false).data(Collections.singletonList(
                            new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(), List.of(abwesenheitDto))))
                    .build();
        };
    }

    private void validateAbwesenheiten(AbwesenheitDto abwesenheitDto) throws BusinessLogicException {
        //benutzer check
        if (gatewayUserHolder.getUserId() == null) {
            throw new BusinessLogicException("User not exists");
        }
        Benutzer benutzer = benutzerService.findById(gatewayUserHolder.getUserId()).orElse(null);
        if (benutzer == null) {
            throw new BusinessLogicException("User not exists");
        }
        Personalnummer personalnummer = benutzer.getPersonalnummer();
        if (personalnummer == null) {
            throw new BusinessLogicException("Personalnummer not found");
        }

        //abwesenheit check
        if (abwesenheitDto.getStartDate() == null || abwesenheitDto.getEndDate() == null
                || leistungserfassungService.isLeistungserfassungMonthClosed(personalnummer.getId(), personalnummer.getFirma().getBmdClient(), abwesenheitDto.getStartDate())
                || leistungserfassungService.isLeistungserfassungMonthClosed(personalnummer.getId(), personalnummer.getFirma().getBmdClient(), abwesenheitDto.getEndDate())) {
            throw new BusinessLogicException("Keine Buchung oder Änderung in abgeschlossenen Monat möglich");
        }

        List<Zeitbuchung> zeitbuchungsOverlaps = zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(personalnummer.getId(), abwesenheitDto.getStartDate(), abwesenheitDto.getEndDate(), Boolean.TRUE);
        if (!zeitbuchungsOverlaps.isEmpty()) {
            throw new BusinessLogicException("Für den angefragten Zeitraum gibt es Leistungsbuchungen, deshalb kann die Abwesenheitsanfrage nicht bearbeitet werden. Bitte wähle einen anderen Zeitraum aus und versuch es erneut.");
        }

        final List<AbwesenheitStatus> abwStatuses = List.of(AbwesenheitStatus.VALID, AbwesenheitStatus.ACCEPTED, AbwesenheitStatus.ACCEPTED_FINAL, AbwesenheitStatus.USED);
        final List<Zeitausgleich> zeitausgleichList = zeitausgleichService.findByPersonalnummerInPeriod(personalnummer.getId(), abwesenheitDto.getStartDate(), abwesenheitDto.getEndDate(), abwStatuses);
        if (!zeitausgleichList.isEmpty()) {
            throw new BusinessLogicException("Für diesen Zeitraum existiert bereits eine Abwesenheit. Bitte wähle einen anderen Zeitraum.");
        }

        final List<Abwesenheit> abwesenheitList = abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(personalnummer.getId(), abwesenheitDto.getStartDate(), abwesenheitDto.getEndDate(), abwStatuses);
        if (!abwesenheitList.isEmpty()) {
            throw new BusinessLogicException("Für diesen Zeitraum existiert bereits eine Abwesenheit. Bitte wähle einen anderen Zeitraum.");
        }

    }

    @Override
    public PayloadResponse getMitarbeiterZeitbuchungen(String token, String startDate, String endDate, boolean shouldSync) {
        Personalnummer personalnummer = environmentService.getPersonalnummer();
        if (shouldSync) {
            gateway2Validation.validateZeitbuchungen(ZeitbuchungSyncRequestDto.builder().personalnummerId(personalnummer.getId()).startDate(startDate).endDate(endDate).build());
        }

        List<Leistungserfassung> leistungserfassungList = leistungserfassungService
                .findByPersonalnummerInPeriod(personalnummer, startDate, endDate);

        ZeitbuchungMetadataDto zeitbuchungMetadataDto = new ZeitbuchungMetadataDto();

        LocalDateTime changedBy = leistungserfassungList.stream()
                .findFirst()
                .flatMap(l -> zeitbuchungService.getZeitbuchungenByListungserfassen(l).stream().findFirst())
                .map(Zeitbuchung::getChangedOn)
                .orElse(null);

        boolean umbuchungAvaliable;

        if (changedBy != null) {
            zeitbuchungMetadataDto.setLastSyncedAt(localDateTimeToString(changedBy));
            umbuchungAvaliable = leistungserfassungService.findByPersonalnummerAndMonthClosedDistinct(personalnummer.getId(), changedBy.toLocalDate()).stream().findFirst().map(l -> !MoxisStatus.SUCCESS.equals(l.getMoxisStatus())).orElse(Boolean.TRUE);
            zeitbuchungMetadataDto.setUmbuchungAvailable(umbuchungAvaliable);
        }

        List<ZeitbuchungenDto> zeitbuchungenDtos = new ArrayList<>(leistungserfassungList.stream()
                .flatMap(leistungserfassung -> zeitbuchungService.getZeitbuchungenByListungserfassen(leistungserfassung).stream())
                .map(zeitbuchungenMapper::mapToDto)
                .toList());
        List<AbwesenheitStatus> excludedStatuses = List.of(AbwesenheitStatus.REJECTED, AbwesenheitStatus.NEW, AbwesenheitStatus.INVALID, AbwesenheitStatus.ERROR, AbwesenheitStatus.CANCELED);
        List<Abwesenheit> abwesenheitList = abwesenheitService.findFilteredAbwesenheitenByStatusInPeriod(personalnummer.getId(), excludedStatuses, startDate, endDate);
        List<Zeitausgleich> zeitausgleichList = zeitausgleichService.findFilteredZeitausgleichByStatusInPeriod(personalnummer.getId(), excludedStatuses, startDate, endDate);
        for (Abwesenheit abwesenheit : abwesenheitList) {
            LocalDate start = abwesenheit.getVon();
            LocalDate end = abwesenheit.getBis();
            List<LocalDate> allDatesAvailiable = zeitbuchungenDtos.stream().map(zeitbuchungenDto -> parseDate(zeitbuchungenDto.getLeistungsdatum())).toList();
            while (!start.isAfter(end) && !allDatesAvailiable.contains(start)) {
                ZeitbuchungenDto dto = zeitbuchungService.mapToZeitbuchungenDto(abwesenheit, start);
                zeitbuchungenDtos.add(dto);
                start = start.plusDays(1);
            }
        }
        zeitbuchungenDtos.addAll(zeitausgleichService.mapListZeitausgleichToListZeitbuchungenDto(zeitausgleichList));
        checkForOverlaps(zeitbuchungenDtos);
        PayloadTypeList<ZeitbuchungenDto> zeitbuchungenDtoPayloadTypeList = new PayloadTypeList<>(PayloadTypes.ZEITBUCHUNGEN.getValue(), zeitbuchungenDtos);
        PayloadTypeList<ZeitbuchungMetadataDto> zeitbuchungMetadataDtoPayloadTypeList =
                new PayloadTypeList<>(PayloadTypes.ZEITBUCHUNGMETADATA.getValue(), List.of(zeitbuchungMetadataDto));

        PayloadResponse response = new PayloadResponse();

        response.setSuccess(true);
        response.setData(List.of(zeitbuchungenDtoPayloadTypeList, zeitbuchungMetadataDtoPayloadTypeList));
        List<LocalDate> localDateList = leistungserfassungService.findDatesWithOverlapping(personalnummer.getId(), startDate, endDate);
        if (!localDateList.isEmpty()) {
            response.setMessage("Overlapping dates detected: %s".formatted(localDateList.toString()));
        }
        return response;
    }

    private static void checkForOverlaps(List<ZeitbuchungenDto> buchungen) {
        buchungen.forEach(currentBooking ->
                buchungen.stream()
                        .filter(otherBooking ->
                                !currentBooking.equals(otherBooking) && hasOverlap(currentBooking, otherBooking)
                        )
                        .forEach(otherBooking -> {
                            currentBooking.setHasError(true);
                            otherBooking.setHasError(true);
                        })
        );
    }

    private static boolean hasOverlap(ZeitbuchungenDto currentBooking, ZeitbuchungenDto otherBooking) {
        if (!currentBooking.getLeistungsdatum().equals(otherBooking.getLeistungsdatum())) {
            return false;
        }

        LocalTime currentVon = isValidTime(currentBooking.getVon()) ? parseTime(currentBooking.getVon()) : null;
        LocalTime currentBis = isValidTime(currentBooking.getBis()) ? parseTime(currentBooking.getBis()) : null;
        LocalTime otherVon = isValidTime(otherBooking.getVon()) ? parseTime(otherBooking.getVon()) : null;
        LocalTime otherBis = isValidTime(otherBooking.getBis()) ? parseTime(otherBooking.getBis()) : null;

        if (currentVon == null || currentBis == null || otherVon == null || otherBis == null) {
            return false;
        }

        return currentVon.isBefore(otherBis) && currentBis.isAfter(otherVon);
    }

    @Override
    public PayloadResponse getAbwesenheitenList(String token, Boolean isPersonal, String status, Integer year, String sortProperty, String sortDirection, int page, int size) {
        List<Abwesenheit> abwesenheitList;
        List<Zeitausgleich> zeitausgleichList;
        Set<String> yearList = new HashSet<>();
        if (isPersonal) {
            Personalnummer personalnummer = environmentService.getPersonalnummer();
            asyncService.asyncExecutor(() -> lhrUrlaubService.syncUrlaubDetails(personalnummer.getId(), null, null));
            abwesenheitList = abwesenheitService.findFilteredAbwesenheitenByStatusAndYear(personalnummer.getId(), status, year);
            zeitausgleichList = zeitausgleichService.findFilteredZeitausgleichByStatusAndYear(personalnummer.getId(), status, year);
            yearList.addAll(abwesenheitService.findDistinctYearsByPersonalnummer(personalnummer.getId()));
            yearList.addAll(zeitausgleichService.findDistinctYearsByPersonalnummer(personalnummer.getId()));
        } else {
            Benutzer loggedFuehrungskraft = benutzerDetailsService.getUserFromToken(token);
            List<AbwesenheitStatus> abwStatuses = isNullOrBlank(status) ? Arrays.asList(AbwesenheitStatus.values()) : List.of(AbwesenheitStatus.valueOf(status));
            abwesenheitList = abwesenheitService.findAllByFuehrungskraefteIdAndStatusIn(loggedFuehrungskraft.getId(), abwStatuses);
            asyncService.asyncExecutor(() -> {
                abwesenheitList.stream().map(abw -> abw.getPersonalnummer().getId()).distinct().forEach(pn -> lhrUrlaubService.syncUrlaubDetails(pn, null, null));
                return true;
            });
            zeitausgleichList = zeitausgleichService.findAllByFuehrungskraefteIdAndStatusIn(loggedFuehrungskraft.getId(), abwStatuses);
        }

        List<AbwesenheitDto> abwesenheitDtoList = new ArrayList<>();
        for (Abwesenheit abwesenheit : abwesenheitList) {
            abwesenheitDtoList.add(abwesenheitService.mapToAbwesenheitDto(abwesenheit));
        }
        abwesenheitDtoList.addAll(zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(zeitausgleichList));

        Page<AbwesenheitDto> resultPage = getAbwesenheitenListPagionation(abwesenheitDtoList, sortProperty, sortDirection, page, size);
        PayloadTypeList<AbwesenheitDto> abwesenheitDtoPayloadType = new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(), resultPage.getContent());
        PayloadTypeList<String> yearListPayloadType = new PayloadTypeList<>(PayloadTypes.ABWESENHEITEN_YEAR_LIST.getValue(), yearList.stream().toList());

        return PayloadResponse.builder()
                .success(true)
                .data(List.of(abwesenheitDtoPayloadType, yearListPayloadType))
                .pagination(createPagination(resultPage))
                .build();
    }

    public Page<AbwesenheitDto> getAbwesenheitenListPagionation(List<AbwesenheitDto> abwesenheitDtoList, String sortProperty, String sortDirection, int page, int size) {
        // Apply sorting
        Comparator<AbwesenheitDto> comparator;
        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = Comparator.comparing(
                    dto -> (Comparable<Object>) getSortPropertyForAbwesenheitDto(dto, sortProperty),
                    Comparator.nullsFirst(Comparator.reverseOrder())
            );
        } else {
            comparator = Comparator.comparing(
                    dto -> (Comparable<Object>) getSortPropertyForAbwesenheitDto(dto, sortProperty),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        }

        if (!isNullOrBlank(sortProperty)) {
            abwesenheitDtoList.sort(comparator);
        }

        // Apply pagination
        int start = Math.min(page * size, abwesenheitDtoList.size());
        int end = Math.min(start + size, abwesenheitDtoList.size());
        List<AbwesenheitDto> paginatedList = abwesenheitDtoList.subList(start, end);

        // Return as Page
        return new PageImpl<>(paginatedList, PageRequest.of(page, size), abwesenheitDtoList.size());
    }

    @Override
    public PayloadResponse deleteZeitausgleich(Integer personalnummerId, String date) {
        ResponseEntity<?> responseEntity = lhrUrlaubService.deleteZeitausgelich(personalnummerId, date);
        return PayloadResponse.builder()
                .success(responseEntity.getStatusCode().is2xxSuccessful())
                .build();
    }

    @Override
    public PayloadResponse getAbwesenheitOverview(String token, String startDate, String endDate, String sortProperty, Sort.Direction sortDirection, int page, int size) {
        Personalnummer personalnummerObject = environmentService.getPersonalnummer();

        LocalDate dateVon;
        LocalDate dateVonActuall;
        if (!isNullOrBlank(startDate) && isValidDate(startDate)) {
            dateVon = parseDate(startDate).withDayOfMonth(1);
            dateVonActuall = parseDate(startDate);
        } else {
            dateVon = LocalDate.now().withDayOfMonth(1).withMonth(1);
            dateVonActuall = LocalDate.now().withDayOfMonth(1).withMonth(1);
        }

        LocalDate dateBis;
        LocalDate dateBisActuall;
        if (!isNullOrBlank(endDate) && isValidDate(endDate)) {
            dateBisActuall = parseDate(endDate);
            dateBis = dateBisActuall.withDayOfMonth(dateBisActuall.lengthOfMonth());
        } else {
            dateBisActuall = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            dateBis = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        }
        asyncService.asyncExecutor(() -> {
            ResponseEntity response = lhrUrlaubService.syncUrlaubDetails(personalnummerObject.getId(), dateVon.format(DateTimeFormatter.ISO_DATE), dateBis.format(DateTimeFormatter.ISO_DATE));
            log.info("Synced result: {}", response.getStatusCode());
            return response;
        });

        final PageRequest request = PageRequest.of(page, size, sortDirection, getSortPropertyForAbwesenheitOverwiev(sortProperty));
        log.info("Form urlaub overview for personalnummer: {} for period: [{}] - [{}]", personalnummerObject.getPersonalnummer(), dateVon, dateBis);
        AbwesenheitOverviewDto overviewDto = abwesenheitService.formUrlaubOverview(personalnummerObject.getId(), dateVonActuall, dateBisActuall, request);

        return PayloadResponse.builder()
                .success(true)
                .data(List.of(new PayloadTypeList<>(PayloadTypes.URLAUB_OVERVIEW.getValue(), List.of(overviewDto))))
                .pagination(Pagination.builder()
                        .totalCount(abwesenheitService.countByPersonalnummerBetweenVonAndBisStatusAndGrunde(personalnummerObject.getId(), dateVonActuall, dateBisActuall, List.of(AbwesenheitStatus.VALID, AbwesenheitStatus.ACCEPTED, AbwesenheitStatus.USED), List.of(URLAUB, ARZT, KRANK)))
                        .page(page)
                        .pageSize(size)
                        .build())
                .build();
    }

    @Override
    public PayloadResponse genehmigungAbwesenheit(String token, Integer abwesenheitId, GenehmigungDto genehmigung) {
        Personalnummer personalnummerObject = environmentService.getPersonalnummer();

        List<Zeitausgleich> zeitausgleichList = new ArrayList<>();
        Optional<AbwesenheitDto> abwesenheitOptional = abwesenheitService.findByIdForceUpdate(abwesenheitId)
                .map(abwesenheitService::mapToAbwesenheitDto)
                .or(() -> {
                    zeitausgleichList.addAll(zeitausgleichService.findAllZeitausgleichInPeriod(abwesenheitId));
                    return zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(zeitausgleichList)
                            .stream().findFirst();
                });
        if (abwesenheitOptional.isEmpty()) {
            log.error("Abwesenheit {} not found", abwesenheitId);
            return PayloadResponse.builder().success(false).message("Abwesenheit not found").build();
        }

        AbwesenheitDto abwesenheitDto = abwesenheitOptional.get();

        if (AbwesenheitStatus.VALID.equals(abwesenheitDto.getStatus())) {
            Benutzer benutzer = benutzerDetailsService.getUserFromToken(token);
            return switch (abwesenheitDto.getType()) {
                case ZEITAUSGLEICH -> genehmigung.getIsAccepted() ?
                        acceptZeitausgleich(zeitausgleichList, benutzer, genehmigung.getComment()) :
                        rejectZeitausgleich(zeitausgleichList, benutzer, genehmigung.getComment());
                case URLAU, UNURL, SURL -> genehmigung.getIsAccepted() ?
                        acceptAbwesenheit(abwesenheitDto, benutzer, genehmigung.getComment()) :
                        rejectAbwesenheit(abwesenheitDto, benutzer, genehmigung.getComment());
                default -> PayloadResponse.builder().success(false).message("Unproccesable abwesenheit type").build();
            };
        }
        if (AbwesenheitStatus.REQUEST_CANCELLATION.equals(abwesenheitDto.getStatus())) {
            return switch (abwesenheitDto.getType()) {
                case ZEITAUSGLEICH -> genehmigung.getIsAccepted() ?
                        deleteZeitausgleich(zeitausgleichList, genehmigung.getComment()) :
                        rejectRequestCancelation(zeitausgleichList, genehmigung.getComment());
                case URLAU, UNURL, SURL -> genehmigung.getIsAccepted() ?
                        deleteAbwesenheit(abwesenheitDto, genehmigung.getComment()) :
                        rejectRequestCancelation(abwesenheitDto, genehmigung.getComment());
                default -> PayloadResponse.builder().success(false).message("Unproccesable abwesenheit type").build();
            };
        }
        return PayloadResponse.builder().success(false).message("Wrong abwesenheit status to genehmigung").build();
    }

    @Override
    public void syncAbwesenheitData() {
        log.info("syncAbwesenheitData");
        schedulerService.syncMAAbwesenheitenData();
    }

    @Override
    public void syncDocumentData() {
        log.info("syncDocumentData");
        schedulerService.syncLhrDocuments();
    }

    @Override
    public void closeMonaten() {
        log.info("closeMonaten");
        schedulerService.closeMonaten();
    }

    @Override
    public PayloadResponse getUmbuchung(String token, String date) {
        Personalnummer personalnummer = environmentService.getPersonalnummer();
        ResponseEntity<UmbuchungDto> responseEntity = lhrZeitdatenService.getPeriodensummen(personalnummer.getId(), date);
        PayloadTypeList<UmbuchungDto> umbuchungPayloadTypeList = new PayloadTypeList<>(PayloadTypes.UMBUCHUNG.getValue(), List.of(responseEntity.getBody()));

        return PayloadResponse.builder().success(true).data(List.of(umbuchungPayloadTypeList)).build();
    }

    @Override
    public PayloadResponse postUmbuchung(String token, String date, UmbuchungDto umbuchungDto) {
        Personalnummer personalnummer = environmentService.getPersonalnummer();

        if (umbuchungDto.getZeitspeicher().stream().noneMatch(z2v -> z2v.getValue() > 0)) {
            PayloadResponse.builder().success(false).message("At least one zeitspeicher should contain value > 0");
        }
        ResponseEntity<Void> responseEntity = lhrZeitdatenService.postAuszahlungsanfrage(personalnummer.getId(), date, umbuchungDto);
        ResponseEntity<UmbuchungDto> umbuchungResponse = lhrZeitdatenService.getPeriodensummen(personalnummer.getId(), date);
        PayloadTypeList<UmbuchungDto> umbuchungPayloadTypeList = new PayloadTypeList<>(PayloadTypes.UMBUCHUNG.getValue(), List.of(umbuchungResponse.getBody()));
        if (responseEntity.getStatusCode().is4xxClientError()) {
            log.warn("LHR check failed for umbuchung! Pn-{}, date-{}, umbuchungDto-{}", personalnummer.getPersonalnummer(), date, umbuchungDto);
            return PayloadResponse.builder()
                    .success(false)
                    .data(List.of(umbuchungPayloadTypeList))
                    .message(umbuchungResponse.getBody().getMetadata().getReason())
                    .build();
        }

        return PayloadResponse.builder().success(true).data(List.of(umbuchungPayloadTypeList)).build();
    }

    /**
     * Reject cancellation of Abwesenheit of a Mitarbeiter and email to Mitarbeiter
     * after it is cancelled.
     *
     * @param zeitausgleichList
     * @param comment
     * @return PayloadResponse with a List of abwesenheitDto
     */
    private PayloadResponse rejectRequestCancelation(List<Zeitausgleich> zeitausgleichList, String comment) {
        zeitausgleichList.forEach(zeitausgleich -> {
            zeitausgleich.setStatus(AbwesenheitStatus.ACCEPTED_FINAL);
            zeitausgleich.setCommentFuehrungskraft(comment);
            zeitausgleichService.save(zeitausgleich);
        });

        List<AbwesenheitDto> listAbwesenheitDto = zeitausgleichService
                .mapListZeitausgleichToListAbwesenheitDto(zeitausgleichList);

        PayloadTypeList<AbwesenheitDto> abwesenheitDtoPayloadTypeList = new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(),
                listAbwesenheitDto);

        AbwesenheitDto abwesenheitDto = listAbwesenheitDto.stream().findFirst().orElse(null);

        if (abwesenheitDto != null && abwesenheitDto.getStatus().equals(AbwesenheitStatus.ACCEPTED_FINAL)) {
            Benutzer benutzer = benutzerService.findByPersonalnummerId(abwesenheitDto.getPersonalnummerId());
            String email = benutzer.getEmail();
            String name = benutzer.getFirstName() + " " + benutzer.getLastName();

            mailService.sendEmail(
                    "gateway-service.ma-abwesenheit-storno-fehlgeschlagen",
                    "german",
                    null,
                    new String[]{email},
                    toObjectArray(),
                    toObjectArray(name, abwesenheitDto.getStartDate(), abwesenheitDto.getEndDate(),
                            ABWESENHEIT_GENEHMIGEN_LINK.formatted(nextAuthUrl))
            );
        }

        return PayloadResponse.builder().success(true).data(List.of(abwesenheitDtoPayloadTypeList)).build();
    }

    /**
     * Reject the cancellation request of Abwesenheit of mitarbeiter.
     * Send email to employee that cancellation was unscucessful.
     *
     * @param abwesenheitDto
     * @param comment
     * @return PayloadResponse of AbwesenheitDto
     */
    private PayloadResponse rejectRequestCancelation(AbwesenheitDto abwesenheitDto, String comment) {
        Abwesenheit abwesenheit = abwesenheitService.findByIdForceUpdate(abwesenheitDto.getId()).orElse(null);

        if (abwesenheit == null) {
            return PayloadResponse.builder().success(false).message("Cannot find abwesenheit").build();
        }

        abwesenheit.setStatus(AbwesenheitStatus.ACCEPTED_FINAL);
        abwesenheit.setCommentFuehrungskraft(comment);
        abwesenheitService.save(abwesenheit);

        AbwesenheitDto mappedAbwesenheitDto = abwesenheitService.mapToAbwesenheitDto(abwesenheit);

        PayloadTypeList<AbwesenheitDto> abwesenheitDtoPayloadTypeList = new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(),
                List.of(mappedAbwesenheitDto));

        if (mappedAbwesenheitDto.getStatus().equals(AbwesenheitStatus.ACCEPTED_FINAL)) {
            Benutzer benutzer = benutzerService.findByPersonalnummerId(abwesenheitDto.getPersonalnummerId());
            String[] email = new String[]{benutzer.getEmail()};
            String name = abwesenheitDto.getFullName();

            mailService.sendEmail("gateway-service.ma-abwesenheit-storno-fehlgeschlagen",
                    "german",
                    null,
                    email,
                    toObjectArray(),
                    toObjectArray(name, abwesenheitDto.getStartDate(), abwesenheitDto.getEndDate(),
                            MEINE_ABWESENHEITEN_LINK.formatted(nextAuthUrl)));
        }

        return PayloadResponse.builder().success(true).data(List.of(abwesenheitDtoPayloadTypeList)).build();
    }


    /**
     * Accept Abwesenheit (URLAU,SURL,UNURL) of MA and send email.
     *
     * @param abwesenheitDto
     * @param benutzer
     * @param comment
     * @return PayloadResponse of AbwesenheitDto
     */
    private PayloadResponse acceptAbwesenheit(AbwesenheitDto abwesenheitDto, Benutzer benutzer, String comment) {
        Abwesenheit abwesenheit = abwesenheitService.findById(abwesenheitDto.getId()).orElse(null);
        if (abwesenheit.getFuehrungskraefte() != null && !abwesenheit.getFuehrungskraefte().contains(benutzer)) {
            return PayloadResponse.builder().success(false).message("Wrong fuehrungskraefte for abwesenheit").build();
        }

        abwesenheitDto = abwesenheitService.findByIdForceUpdate(abwesenheitDto.getId()).map(abw -> {
            abw.setStatus(AbwesenheitStatus.ACCEPTED);
            if (!isNullOrBlank(comment)) {
                abw.setCommentFuehrungskraft(comment);
            }
            AbwesenheitDto mappedToAbwesenheitDto = abwesenheitService.mapToAbwesenheitDto(abwesenheitService.save(abw));
            Personalnummer pn = abw.getPersonalnummer();
            String[] email = new String[]{
                    benutzerService.findByPersonalnummerAndFirmaBmdClient(pn.getPersonalnummer(),
                            pn.getFirma().getBmdClient()).getEmail()
            };

            String name = mappedToAbwesenheitDto.getFullName();
            LocalDate startDate = mappedToAbwesenheitDto.getStartDate();
            LocalDate endDate = mappedToAbwesenheitDto.getEndDate();

            if (startDate != null && endDate != null) {
                mailService.sendEmail("gateway-service.ma-abwesenheit-genehmigt",
                        "german",
                        null,
                        email,
                        toObjectArray(),
                        toObjectArray(name, startDate, endDate,
                                MEINE_ABWESENHEITEN_LINK.formatted(nextAuthUrl)));
            }

            return mappedToAbwesenheitDto;
        }).orElse(null);

        PayloadTypeList<AbwesenheitDto> abwesenheitDtoPayloadType = new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue());
        abwesenheitDtoPayloadType.setAttributes(Collections.singletonList(abwesenheitDto));


        return PayloadResponse.builder()
                .success(true)
                .data(List.of(abwesenheitDtoPayloadType))
                .build();
    }


    /**
     * Accept ZEITAUSGLEICH of MA and send email
     *
     * @param zeitausgleichList
     * @param benutzer
     * @param comment
     * @return PayloadResponse of AbwesenheitDto
     */
    private PayloadResponse acceptZeitausgleich(List<Zeitausgleich> zeitausgleichList, Benutzer benutzer, String comment) {

        for (Zeitausgleich zeitausgleich : zeitausgleichList) {
            if (zeitausgleich.getFuehrungskraefte() != null && !zeitausgleich.getFuehrungskraefte().contains(benutzer)) {
                return PayloadResponse.builder().success(false).message("Wrong fuehrungskraefte for zeitausgleich").build();
            }
        }

        zeitausgleichList = zeitausgleichList.stream().map(Zeitausgleich::getId)
                .flatMap(id -> zeitausgleichService.findByIdForceUpdate(id).stream())
                .map(zeitausgleich -> {
                    zeitausgleich.setStatus(AbwesenheitStatus.ACCEPTED);
                    if (!isNullOrBlank(comment)) {
                        zeitausgleich.setCommentFuehrungskraft(comment);
                    }
                    return zeitausgleichService.save(zeitausgleich);
                })
                .toList();

        List<AbwesenheitDto> abwesenheitDtoList = zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(zeitausgleichList);

        if (abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getStatus)
                .filter(status -> status == AbwesenheitStatus.ACCEPTED).isPresent()) {

            Integer personalnummerMA = abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getPersonalnummerId).orElse(null);

            Benutzer benutzerMA = benutzerService.findByPersonalnummerId(personalnummerMA);
            String name = abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getFullName).orElse(null);
            String[] email = new String[]{benutzerMA.getEmail()};


            LocalDate startDate = abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getStartDate).orElse(null);
            LocalDate endDate = abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getEndDate).orElse(null);

            if (startDate != null && endDate != null && name != null) {
                mailService.sendEmail("gateway-service.ma-abwesenheit-genehmigt",
                        "german",
                        null,
                        email,
                        toObjectArray(),
                        toObjectArray(name, startDate, endDate,
                                MEINE_ABWESENHEITEN_LINK.formatted(nextAuthUrl)));
            }
        }

        return PayloadResponse.builder()
                .success(true)
                .data(Collections.singletonList(new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(), List.of(abwesenheitDtoList))))
                .build();
    }

    /**
     * Reject Abwesenheit (URLAU,SURL,UNURL) of MA and send email them.
     *
     * @param abwesenheitDto
     * @param benutzer
     * @param comment
     * @return PayloadResponse of AbwesenheitDto
     */
    private PayloadResponse rejectAbwesenheit(AbwesenheitDto abwesenheitDto, Benutzer benutzer, String comment) {
        Abwesenheit abwesenheit = abwesenheitService.findById(abwesenheitDto.getId()).orElse(null);
        if (abwesenheit.getFuehrungskraefte() != null && !abwesenheit.getFuehrungskraefte().contains(benutzer)) {
            return PayloadResponse.builder().success(false).message("Wrong fuehrungskraefte for abwesenheit").build();
        }

        ResponseEntity result = lhrUrlaubService.deleteUrlaub(abwesenheitDto, null, null);

        if (result.getStatusCode().is2xxSuccessful()) {
            abwesenheitService.findById(abwesenheitDto.getId()).ifPresent(abw -> {
                abw.setStatus(AbwesenheitStatus.REJECTED);
                if (!isNullOrBlank(comment)) {
                    abw.setCommentFuehrungskraft(comment);
                }
                abwesenheitService.save(abw);
            });
        }

        AbwesenheitDto dbDto = abwesenheitService
                .findByIdForceUpdate(abwesenheitDto.getId())
                .map(abwesenheitService::mapToAbwesenheitDto)
                .orElse(null);

        if (Objects.requireNonNull(dbDto).getStatus().equals(AbwesenheitStatus.REJECTED)) {

            String[] email = new String[]{benutzer.getEmail()};


            String name = dbDto.getFullName();
            LocalDate startDate = dbDto.getStartDate();
            LocalDate endDate = dbDto.getEndDate();

            mailService.sendEmail("gateway-service.ma-abwesenheit-abgelehnt",
                    "german",
                    null,
                    email,
                    toObjectArray(),
                    toObjectArray(name, startDate, endDate,
                            MEINE_ABWESENHEITEN_LINK.formatted(nextAuthUrl)));
        }

        PayloadTypeList<AbwesenheitDto> abwesenheitDtoPayloadType = new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue());
        abwesenheitDtoPayloadType.setAttributes(Collections.singletonList(dbDto));

        return PayloadResponse.builder()
                .success(result.getStatusCode().is2xxSuccessful())
                .data(List.of(abwesenheitDtoPayloadType))
                .build();
    }

    /**
     * Reject ZEITAUSGLEICH of MA and send email to them.
     *
     * @param zeitausgleichList
     * @param benutzer
     * @param comment
     * @return PayloadResponse AbwesenheitDto
     */
    private PayloadResponse rejectZeitausgleich(List<Zeitausgleich> zeitausgleichList, Benutzer benutzer, String comment) {
        for (Zeitausgleich zeitausgleich : zeitausgleichList) {
            if (zeitausgleich.getFuehrungskraefte() != null && !zeitausgleich.getFuehrungskraefte().contains(benutzer)) {
                return PayloadResponse.builder().success(false).message("Wrong fuehrungskraefte for zeitausgleich").build();
            }
        }

        boolean result = zeitausgleichList.stream().map(zeitausgleich -> deleteZeitausgleich(
                        zeitausgleich.getPersonalnummer().getId(),
                        zeitausgleich.getDatum().format(DateTimeFormatter.ISO_DATE)).isSuccess())
                .reduce(true, (subtotal, element) -> subtotal && element);

        String message = null;

        if (result) {
            zeitausgleichList = zeitausgleichList.stream().map(Zeitausgleich::getId)
                    .flatMap(id -> zeitausgleichService.findByIdForceUpdate(id).stream())
                    .map(zeitausgleich -> {
                        zeitausgleich.setStatus(AbwesenheitStatus.REJECTED);
                        if (!isNullOrBlank(comment)) {
                            zeitausgleich.setCommentFuehrungskraft(comment);
                        }
                        return zeitausgleichService.save(zeitausgleich);
                    })
                    .toList();
        } else {
            message = "Error from lhr-side";
        }
        List<AbwesenheitDto> abwesenheitDtoList = zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(zeitausgleichList);

        if (abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getStatus)
                .filter(status -> status == AbwesenheitStatus.VALID).isPresent()) {

            String[] email = new String[]{benutzer.getEmail()};

            LocalDate startDate = abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getStartDate).orElse(null);
            LocalDate endDate = abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getEndDate).orElse(null);
            String name = abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getFullName).orElse(null);
            AbwesenheitStatus status = abwesenheitDtoList.stream().findFirst().map(AbwesenheitDto::getStatus).orElse(null);

            if (Objects.requireNonNull(status).equals(AbwesenheitStatus.REJECTED) &&
                    startDate != null && endDate != null && name != null) {

                mailService.sendEmail("gateway-service.ma-abwesenheit-abgelehnt",
                        "german",
                        null,
                        email,
                        toObjectArray(),
                        toObjectArray(name, startDate, endDate,
                                MEINE_ABWESENHEITEN_LINK.formatted(nextAuthUrl)));
            }

        }
        return PayloadResponse.builder()
                .success(result)
                .data(Collections.singletonList(new PayloadTypeList<>(PayloadTypes.ABWESENHEIT.getValue(), List.of(abwesenheitDtoList))))
                .message(message)
                .build();
    }

    private void sendMailToFuehrungskraft(String[] emails, LocalDate startDate, LocalDate endDate, String nameMitarbeiter) {
        if (emails.length == 0) {
            log.warn("No email were found, therefore mails won`t be sent");
            return;
        }

        Benutzer benutzer = benutzerService.findByEmail(emails[0]);
        if (benutzer == null) {
            log.warn("Benutzer not found by email: {}", emails[0]);
            return;
        }

        String nameFuehrungskraft = Stream.of(benutzer.getFirstName(), benutzer.getLastName()).filter(Objects::nonNull).collect(Collectors.joining(" "));
        mailService.sendEmail("gateway-service.ma-abwesenheit-info",
                "german",
                null,
                emails,
                toObjectArray(nameMitarbeiter),
                toObjectArray(nameFuehrungskraft, nameMitarbeiter, startDate, endDate, ABWESENHEIT_GENEHMIGEN_LINK.formatted(nextAuthUrl)));
    }

    private void mailZeitausgleichCancellation(List<AbwesenheitDto> abwesenheitDtos) {
        if (Objects.requireNonNull(abwesenheitDtos.stream().findFirst().map(AbwesenheitDto::getStatus).orElse(null))
                .equals(AbwesenheitStatus.REQUEST_CANCELLATION)) {

            String[] emails = abwesenheitDtos.stream()
                    .flatMap(dto -> dto.getFuehrungskraefte().stream())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toArray(String[]::new);


            LocalDate startDate = abwesenheitDtos.stream().findFirst().map(AbwesenheitDto::getStartDate).orElse(null);
            LocalDate endDate = abwesenheitDtos.stream().findFirst().map(AbwesenheitDto::getEndDate).orElse(null);
            String nameMitarbeiter = abwesenheitDtos.stream().findFirst().map(AbwesenheitDto::getFullName).orElse(null);
            String email = Arrays.stream(emails).findFirst().orElse(null);
            if (isNullOrBlank(email)) {
                log.warn("Cannot find an email");
                return;
            }

            String nameFuehrungskraft = Optional.ofNullable(benutzerService.findByEmail(email))
                    .map(b -> Stream.of(b.getFirstName(), b.getLastName())
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(" ")))
                    .orElse(null);

            if (startDate != null && endDate != null && nameMitarbeiter != null && nameFuehrungskraft != null) {

                mailService.sendEmail("gateway-service.ma-abwesenheit-storno-info",
                        "german",
                        null,
                        emails,
                        toObjectArray(nameMitarbeiter),
                        toObjectArray(nameFuehrungskraft, nameMitarbeiter, startDate, endDate,
                                ABWESENHEIT_GENEHMIGEN_LINK.formatted(nextAuthUrl)));

            }
        }
    }
}