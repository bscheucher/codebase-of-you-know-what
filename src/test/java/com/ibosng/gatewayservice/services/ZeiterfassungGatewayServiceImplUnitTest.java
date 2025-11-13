package com.ibosng.gatewayservice.services;

import com.ibosng.dbibosservice.services.AdresseIbosService;
import com.ibosng.dbmapperservice.services.ZeitbuchungenMapperService;
import com.ibosng.dbservice.dtos.mitarbeiter.AbwesenheitDto;
import com.ibosng.dbservice.entities.Benutzer;
import com.ibosng.dbservice.entities.lhr.Abwesenheit;
import com.ibosng.dbservice.entities.lhr.AbwesenheitStatus;
import com.ibosng.dbservice.entities.masterdata.IbisFirma;
import com.ibosng.dbservice.entities.masterdata.Personalnummer;
import com.ibosng.dbservice.entities.mitarbeiter.AbwesenheitType;
import com.ibosng.dbservice.services.BenutzerService;
import com.ibosng.dbservice.services.ZeitausgleichService;
import com.ibosng.dbservice.services.lhr.AbwesenheitService;
import com.ibosng.dbservice.services.mitarbeiter.PersonalnummerService;
import com.ibosng.dbservice.services.zeitbuchung.LeistungserfassungService;
import com.ibosng.dbservice.services.zeitbuchung.ZeitbuchungService;
import com.ibosng.gatewayservice.config.GatewayUserHolder;
import com.ibosng.gatewayservice.dtos.response.PayloadResponse;
import com.ibosng.gatewayservice.services.impl.ZeiterfassungGatewayServiceImpl;
import com.ibosng.lhrservice.services.LHRUrlaubService;
import com.ibosng.lhrservice.services.LHRZeitdatenService;
import com.ibosng.lhrservice.services.LHRZeiterfassungService;
import com.ibosng.lhrservice.services.SchedulerService;
import com.ibosng.microsoftgraphservice.services.MailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ZeiterfassungGatewayServiceImplUnitTest {

    @Mock
    private MailService mailService;

    @Mock
    private BenutzerService benutzerService;

    @Mock
    private AbwesenheitService abwesenheitService;

    @Mock
    private LHRUrlaubService lhrUrlaubService;

    @Mock
    private PersonalnummerService personalnummerService;

    @Mock
    private BenutzerDetailsService benutzerDetailsService;

    @Mock
    private ZeitbuchungenMapperService zeitbuchungenMapper;

    @Mock
    private LeistungserfassungService leistungserfassungService;

    @Mock
    private ZeitbuchungService zeitbuchungService;

    @Mock
    private Gateway2Validation gateway2Validation;

    @Mock
    private ZeitausgleichService zeitausgleichService;

    @Mock
    private AdresseIbosService adresseIbosService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private AsyncService asyncService;

    @Mock
    private GatewayUserHolder gatewayUserHolder;

    @Mock
    private LHRZeiterfassungService lhrZeiterfassungService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private LHRZeitdatenService lhrZeitdatenService;


    @InjectMocks
    private ZeiterfassungGatewayServiceImpl zeiterfassungGatewayService;

    private AbwesenheitDto abwesenheitDto;
    private Personalnummer personalnummer;
    private Benutzer employee;
    private Benutzer supervisor;
    private Abwesenheit abwesenheit;
    private IbisFirma firma;

    @BeforeEach
    void setUp() {
        // Set up test data
        firma = new IbisFirma();
        firma.setBmdClient(12345);

        personalnummer = new Personalnummer();
        personalnummer.setId(1);
        personalnummer.setPersonalnummer("12345");
        personalnummer.setFirma(firma);

        employee = new Benutzer();
        employee.setId(1);
        employee.setUpn("employee@test.com");
        employee.setEmail("employee@test.com");
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setPersonalnummer(personalnummer);

        supervisor = new Benutzer();
        supervisor.setId(2);
        supervisor.setUpn("supervisor@test.com");
        supervisor.setEmail("supervisor@test.com");
        supervisor.setFirstName("Jane");
        supervisor.setLastName("Manager");

        abwesenheitDto = new AbwesenheitDto();
        abwesenheitDto.setId(100);
        abwesenheitDto.setPersonalnummerId(1);
        abwesenheitDto.setType(AbwesenheitType.URLAU);
        abwesenheitDto.setStartDate(LocalDate.now().plusDays(1));
        abwesenheitDto.setEndDate(LocalDate.now().plusDays(5));
        abwesenheitDto.setFullName("John Doe");

        abwesenheit = new Abwesenheit();
        abwesenheit.setId(100);
        abwesenheit.setPersonalnummer(personalnummer);
        abwesenheit.setStatus(AbwesenheitStatus.VALID);
        abwesenheit.setVon(abwesenheitDto.getStartDate());
        abwesenheit.setBis(abwesenheitDto.getEndDate());

        // Initialize Führungskräfte set
        Set<Benutzer> fuehrungskraefte = new HashSet<>();
        fuehrungskraefte.add(supervisor);
        abwesenheit.setFuehrungskraefte(fuehrungskraefte);
    }

    @Test
    void testPostAbwesenheit_EmailsArrayIsNotEmpty_WhenFuehrungskraefteExists() {
        // Arrange - Mock validation dependencies first
        when(gatewayUserHolder.getUserId()).thenReturn(1);

        when(benutzerService.findById(1)).thenReturn(Optional.of(employee));

        // Mock the leistungserfassung month closed checks
        when(leistungserfassungService.isLeistungserfassungMonthClosed(
                eq(personalnummer.getId()),
                eq(firma.getBmdClient()),
                any(LocalDate.class)))
                .thenReturn(false);

        // Mock zeitbuchung overlaps check - return empty list (no overlaps)
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(
                eq(personalnummer.getId()),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(Boolean.TRUE)))
                .thenReturn(java.util.Collections.emptyList());

        // Mock zeitausgleich check - return empty list (no existing zeitausgleich)
        when(zeitausgleichService.findByPersonalnummerInPeriod(
                eq(personalnummer.getId()),
                any(LocalDate.class),
                any(LocalDate.class),
                anyList()))
                .thenReturn(java.util.Collections.emptyList());

        // Mock abwesenheit check - return empty list (no existing abwesenheit)
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(
                eq(personalnummer.getId()),
                any(LocalDate.class),
                any(LocalDate.class),
                anyList()))
                .thenReturn(java.util.Collections.emptyList());

        // Mock environmentService - this is called in saveAbwesenheit to get personalnummer
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // Mock for getFuehrungskraefte method
        when(environmentService.isProduction()).thenReturn(true);
        when(adresseIbosService.getFuehrungskraftUPNFromLogin(anyString()))
                .thenReturn("supervisor@test.com");
        when(benutzerService.findByUpn("supervisor@test.com"))
                .thenReturn(supervisor);

        // Now mock the actual postAbwesenheit flow
        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), eq(abwesenheitDto)))
                .thenAnswer(invocation -> ResponseEntity.ok(abwesenheitDto));

        when(abwesenheitService.findByIdForceUpdate(abwesenheitDto.getId()))
                .thenReturn(Optional.of(abwesenheit));

        when(benutzerService.findByPersonalnummerAndFirmaBmdClient(
                personalnummer.getPersonalnummer(),
                firma.getBmdClient()))
                .thenReturn(employee);

        when(abwesenheitService.save(any(Abwesenheit.class)))
                .thenReturn(abwesenheit);

        when(abwesenheitService.mapToAbwesenheitDto(any(Abwesenheit.class)))
                .thenReturn(abwesenheitDto);

        // This is needed for sendMailToFuehrungskraft method
        when(benutzerService.findByEmail("supervisor@test.com"))
                .thenReturn(supervisor);

        doNothing().when(mailService).sendEmail(
                anyString(),
                anyString(),
                isNull(),
                any(String[].class),
                any(),
                any());

        // Act
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, null);

        // Assert
        assertTrue(response.isSuccess(), "Response should be successful");

        ArgumentCaptor<String[]> emailCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mailService, times(1)).sendEmail(
                eq("gateway-service.ma-abwesenheit-info"),
                eq("german"),
                isNull(),
                emailCaptor.capture(),
                any(),
                any()
        );

        String[] capturedEmails = emailCaptor.getValue();

        log.info(">>>>>>>>> Captured emails: {} >>>>>>>>>>>>>>>>>>", Arrays.toString(capturedEmails));
        assertNotNull(capturedEmails, "Emails array should not be null");
        assertTrue(capturedEmails.length > 0, "Emails array should not be empty");
        assertEquals("supervisor@test.com", capturedEmails[0], "Email should match supervisor's email");
    }
}