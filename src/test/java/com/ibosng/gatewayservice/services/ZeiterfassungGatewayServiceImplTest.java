package com.ibosng.gatewayservice.services;

import com.ibosng.dbibosservice.services.AdresseIbosService;
import com.ibosng.dbmapperservice.services.ZeitbuchungenMapperService;
import com.ibosng.dbservice.dtos.mitarbeiter.AbwesenheitDto;
import com.ibosng.dbservice.entities.Benutzer;
import com.ibosng.dbservice.entities.lhr.Abwesenheit;
import com.ibosng.dbservice.entities.lhr.AbwesenheitStatus;
import com.ibosng.dbservice.entities.masterdata.Firma;
import com.ibosng.dbservice.entities.masterdata.Personalnummer;
import com.ibosng.dbservice.entities.mitarbeiter.AbwesenheitType;
import com.ibosng.dbservice.entities.mitarbeiter.Stammdaten;
import com.ibosng.dbservice.entities.zeitbuchung.Leistungserfassung;
import com.ibosng.dbservice.entities.zeiterfassung.Zeitspeicher;
import com.ibosng.dbservice.services.BenutzerService;
import com.ibosng.dbservice.services.ZeitausgleichService;
import com.ibosng.dbservice.services.lhr.AbwesenheitService;
import com.ibosng.dbservice.services.mitarbeiter.PersonalnummerService;
import com.ibosng.dbservice.services.mitarbeiter.StammdatenService;
import com.ibosng.dbservice.services.zeitbuchung.LeistungserfassungService;
import com.ibosng.dbservice.services.zeitbuchung.ZeitbuchungService;
import com.ibosng.dbservice.services.zeiterfassung.AuszahlungsantragService;
import com.ibosng.dbservice.services.zeiterfassung.ZeitspeicherService;
import com.ibosng.gatewayservice.config.DataSourceConfigTest;
import com.ibosng.gatewayservice.config.GatewayUserHolder;
import com.ibosng.gatewayservice.dtos.response.PayloadResponse;
import com.ibosng.gatewayservice.enums.PayloadTypes;
import com.ibosng.gatewayservice.services.impl.ZeiterfassungGatewayServiceImpl;
import com.ibosng.gatewayservice.utils.Helpers;
import com.ibosng.lhrservice.services.LHRUrlaubService;
import com.ibosng.microsoftgraphservice.services.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = DataSourceConfigTest.class)
class ZeiterfassungGatewayServiceImplTest {

    @InjectMocks
    private ZeiterfassungGatewayServiceImpl zeiterfassungGatewayService;

    @Mock
    private AbwesenheitService abwesenheitService;

    @Mock
    private BenutzerDetailsService benutzerDetailsService;

    @Mock
    private LeistungserfassungService leistungserfassungService;

    @Mock
    private ZeitbuchungService zeitbuchungService;

    @Mock
    private ZeitspeicherService zeitspeicherService;

    @Mock
    private Gateway2Validation gateway2Validation;

    @Mock
    private StammdatenService stammdatenService;

    @Mock
    private ZeitbuchungenMapperService zeitbuchungenMapper;

    @Mock
    private PersonalnummerService personalnummerService;

    @Mock
    private Helpers helpers;

    @Mock
    private AuszahlungsantragService auszahlungsantragService;

    @Mock
    private ZeitausgleichService zeitausgleichService;

    @Mock
    private LHRUrlaubService lhrUrlaubService;

    @Mock
    private BenutzerService benutzerService;

    @Mock
    private AdresseIbosService adresseIbosService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private MailService mailService;

    @Mock
    private GatewayUserHolder gatewayUserHolder;

    @BeforeEach
    void setUp() {
        // Set nextAuthUrl via reflection since it's injected via @Value
        ReflectionTestUtils.setField(zeiterfassungGatewayService, "nextAuthUrl", "http://localhost:3000");
    }


    @Test
    void testGetAbwesenheit() {
        Integer abwesenheitId = 1;

        Abwesenheit mockedAbwesenheit = new Abwesenheit();
        mockedAbwesenheit.setId(abwesenheitId);
        when(abwesenheitService.findById(anyInt())).thenReturn(Optional.of(mockedAbwesenheit));
        when(abwesenheitService.mapToAbwesenheitDto(any())).thenReturn(AbwesenheitDto.builder().id(1).build());
        PayloadResponse response = zeiterfassungGatewayService.getAbwesenheit(abwesenheitId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(PayloadTypes.ABWESENHEIT.getValue(), response.getData().get(0).getType());
    }

    @Test
    @Disabled
    void testDeleteAbwesenheit() {
        Integer abwesenheitId = 123;

        PayloadResponse response = zeiterfassungGatewayService.deleteAbwesenheit(abwesenheitId);

        assertNotNull(response, "Response should not be null");
        assertTrue(response.isSuccess(), "Response should indicate success");
    }

    @Disabled
    @Test
    void testGetAbwesenheitenList() {
        String authorizationToken = "Bearer Authorization";
        Abwesenheit mockedResult = new Abwesenheit();
        mockedResult.setId(1);
        mockedResult.setPersonalnummer(createPersonalNummer());
        List<Abwesenheit> mockedAbwesenenheiten = new ArrayList<>();
        mockedAbwesenenheiten.add(mockedResult);

        Benutzer benutzer = new Benutzer();
        benutzer.setFirstName("John");
        benutzer.setLastName("Doe");
        benutzer.setCreatedBy("Max");
        benutzer.setEmail("email@test.com");
        benutzer.setId(1);
        String sortProperty = "startDate";
        String sortDirection = "ASC";
        int page = 0;
        int size = 100;

        when(abwesenheitService.findAllByFuehrungskraefteId(anyInt())).thenReturn(mockedAbwesenenheiten);
        when(benutzerDetailsService.getUserFromToken(authorizationToken)).thenReturn(benutzer);
        PayloadResponse response = zeiterfassungGatewayService.getAbwesenheitenList(authorizationToken, true, "", null, sortProperty, sortDirection, page, size);

        assertNotNull(response);
        assertTrue(response.isSuccess(), "Response should indicate success");
    }


    private Personalnummer createPersonalNummer() {
        Personalnummer personalnummer = new Personalnummer();
        personalnummer.setId(1);
        personalnummer.setPersonalnummer("123456");

        return personalnummer;
    }

    // ========================================
    // Tests for postAbwesenheit method
    // ========================================

    @Test
    void testPostAbwesenheit_SuccessWithValidStatus_SendsEmail() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 5);

        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .type(AbwesenheitType.URLAU)
                .startDate(startDate)
                .endDate(endDate)
                .fullName("John Doe")
                .build();

        Personalnummer personalnummer = createPersonalnummerWithFirma();
        Firma firma = personalnummer.getFirma();

        Abwesenheit savedAbwesenheit = createAbwesenheit(1, startDate, endDate, AbwesenheitStatus.VALID, personalnummer);

        AbwesenheitDto responseDto = AbwesenheitDto.builder()
                .id(1)
                .type(AbwesenheitType.URLAU)
                .startDate(startDate)
                .endDate(endDate)
                .status(AbwesenheitStatus.VALID)
                .fullName("John Doe")
                .build();

        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");
        Benutzer fuehrungskraft = createBenutzer(2, "manager@test.com", "Max", "Manager", "manager@test.com");

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));
        when(leistungserfassungService.isLeistungserfassungMonthClosed(anyInt(), anyString(), any())).thenReturn(false);
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(zeitausgleichService.findByPersonalnummerInPeriod(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // Mock LHR service response
        ResponseEntity<AbwesenheitDto> lhrResponse = ResponseEntity.ok(responseDto);
        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class))).thenReturn(lhrResponse);

        // Mock abwesenheit lookup
        when(abwesenheitService.findByIdForceUpdate(1)).thenReturn(Optional.of(savedAbwesenheit));
        when(benutzerService.findByPersonalnummerAndFirmaBmdClient(anyString(), anyString())).thenReturn(mitarbeiter);

        // Mock Führungskraft lookup
        when(environmentService.isProduction()).thenReturn(true);
        when(adresseIbosService.getFuehrungskraftUPNFromLogin(anyString())).thenReturn("manager@test.com");
        when(benutzerService.findByUpn("manager@test.com")).thenReturn(fuehrungskraft);
        when(abwesenheitService.save(any(Abwesenheit.class))).thenReturn(savedAbwesenheit);
        when(abwesenheitService.mapToAbwesenheitDto(any(Abwesenheit.class))).thenReturn(responseDto);

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals(PayloadTypes.ABWESENHEIT.getValue(), response.getData().get(0).getType());

        // Verify email was sent
        verify(mailService, times(1)).sendEmail(
                eq("gateway-service.ma-abwesenheit-info"),
                eq("german"),
                isNull(),
                eq(new String[]{"manager@test.com"}),
                any(),
                any()
        );
    }

    @Test
    void testPostAbwesenheit_SuccessWithNonValidStatus_NoEmail() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 5);

        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .type(AbwesenheitType.SURL)
                .startDate(startDate)
                .endDate(endDate)
                .fullName("John Doe")
                .build();

        Personalnummer personalnummer = createPersonalnummerWithFirma();
        Abwesenheit savedAbwesenheit = createAbwesenheit(1, startDate, endDate, AbwesenheitStatus.ACCEPTED, personalnummer);

        AbwesenheitDto responseDto = AbwesenheitDto.builder()
                .id(1)
                .type(AbwesenheitType.SURL)
                .startDate(startDate)
                .endDate(endDate)
                .status(AbwesenheitStatus.ACCEPTED)
                .fullName("John Doe")
                .build();

        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));
        when(leistungserfassungService.isLeistungserfassungMonthClosed(anyInt(), anyString(), any())).thenReturn(false);
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(zeitausgleichService.findByPersonalnummerInPeriod(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // Mock LHR service response
        ResponseEntity<AbwesenheitDto> lhrResponse = ResponseEntity.ok(responseDto);
        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class))).thenReturn(lhrResponse);

        // Mock abwesenheit lookup
        when(abwesenheitService.findByIdForceUpdate(1)).thenReturn(Optional.of(savedAbwesenheit));
        when(benutzerService.findByPersonalnummerAndFirmaBmdClient(anyString(), anyString())).thenReturn(mitarbeiter);
        when(abwesenheitService.save(any(Abwesenheit.class))).thenReturn(savedAbwesenheit);
        when(abwesenheitService.mapToAbwesenheitDto(any(Abwesenheit.class))).thenReturn(responseDto);

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify no email was sent
        verify(mailService, never()).sendEmail(anyString(), anyString(), any(), any(), any(), any());
    }

    @Test
    void testPostAbwesenheit_PersonalnummerIsNull_ReturnsFalse() {
        // Given
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .id(1)
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 5))
                .build();

        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");
        mitarbeiter.setPersonalnummer(null);

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));

        // Mock personalnummer lookup returning null
        when(personalnummerService.findById(1)).thenReturn(Optional.empty());
        when(environmentService.getPersonalnummer()).thenReturn(null);

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        verify(lhrUrlaubService, never()).createUrlaub(any(), any(), any());
    }

    @Test
    void testPostAbwesenheit_PersonalnummerStringIsBlank_ReturnsFalse() {
        // Given
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 5))
                .build();

        Personalnummer personalnummer = new Personalnummer();
        personalnummer.setId(1);
        personalnummer.setPersonalnummer(""); // blank
        personalnummer.setFirma(createFirma());

        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));
        when(leistungserfassungService.isLeistungserfassungMonthClosed(anyInt(), anyString(), any())).thenReturn(false);
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(zeitausgleichService.findByPersonalnummerInPeriod(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        verify(lhrUrlaubService, never()).createUrlaub(any(), any(), any());
    }

    @Test
    void testPostAbwesenheit_FirmaIsNull_ReturnsFalse() {
        // Given
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 5))
                .build();

        Personalnummer personalnummer = new Personalnummer();
        personalnummer.setId(1);
        personalnummer.setPersonalnummer("123456");
        personalnummer.setFirma(null); // null firma

        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));
        when(leistungserfassungService.isLeistungserfassungMonthClosed(anyInt(), anyString(), any())).thenReturn(false);
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(zeitausgleichService.findByPersonalnummerInPeriod(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        verify(lhrUrlaubService, never()).createUrlaub(any(), any(), any());
    }

    @Test
    void testPostAbwesenheit_LhrReturns409Conflict_ReturnsErrorMessage() {
        // Given
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 5))
                .build();

        Personalnummer personalnummer = createPersonalnummerWithFirma();
        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));
        when(leistungserfassungService.isLeistungserfassungMonthClosed(anyInt(), anyString(), any())).thenReturn(false);
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(zeitausgleichService.findByPersonalnummerInPeriod(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // Mock LHR service returning 409 Conflict
        ResponseEntity<AbwesenheitDto> conflictResponse = ResponseEntity.status(HttpStatusCode.valueOf(409)).build();
        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class))).thenReturn(conflictResponse);

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Für diesen Zeitraum existiert bereits eine Abwesenheit. Bitte wähle einen anderen Zeitraum.", response.getMessage());
    }

    @Test
    void testPostAbwesenheit_LhrReturnsNullBody_ReturnsErrorMessage() {
        // Given
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .type(AbwesenheitType.UNURL)
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 5))
                .build();

        Personalnummer personalnummer = createPersonalnummerWithFirma();
        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));
        when(leistungserfassungService.isLeistungserfassungMonthClosed(anyInt(), anyString(), any())).thenReturn(false);
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(zeitausgleichService.findByPersonalnummerInPeriod(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // Mock LHR service returning null body
        ResponseEntity<AbwesenheitDto> nullBodyResponse = ResponseEntity.ok().body(null);
        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class))).thenReturn(nullBodyResponse);

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage());
    }

    @Test
    void testPostAbwesenheit_AbwesenheitNotFoundAfterCreation_ReturnsErrorMessage() {
        // Given
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 5))
                .build();

        Personalnummer personalnummer = createPersonalnummerWithFirma();
        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");

        AbwesenheitDto responseDto = AbwesenheitDto.builder()
                .id(1)
                .type(AbwesenheitType.URLAU)
                .build();

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));
        when(leistungserfassungService.isLeistungserfassungMonthClosed(anyInt(), anyString(), any())).thenReturn(false);
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(zeitausgleichService.findByPersonalnummerInPeriod(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // Mock LHR service response
        ResponseEntity<AbwesenheitDto> lhrResponse = ResponseEntity.ok(responseDto);
        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class))).thenReturn(lhrResponse);

        // Mock abwesenheit not found
        when(abwesenheitService.findByIdForceUpdate(1)).thenReturn(Optional.empty());

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage());
    }

    @Test
    void testPostAbwesenheit_ExceptionFromLhr_ReturnsFailure() {
        // Given
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 5))
                .build();

        Personalnummer personalnummer = createPersonalnummerWithFirma();
        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));
        when(leistungserfassungService.isLeistungserfassungMonthClosed(anyInt(), anyString(), any())).thenReturn(false);
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(zeitausgleichService.findByPersonalnummerInPeriod(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // Mock LHR service throwing exception
        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class)))
                .thenThrow(new RuntimeException("LHR service error"));

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
    }

    @Test
    void testPostAbwesenheit_FuehrungskraftNotFound_StillSucceeds() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 5);

        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .type(AbwesenheitType.URLAU)
                .startDate(startDate)
                .endDate(endDate)
                .fullName("John Doe")
                .build();

        Personalnummer personalnummer = createPersonalnummerWithFirma();
        Abwesenheit savedAbwesenheit = createAbwesenheit(1, startDate, endDate, AbwesenheitStatus.VALID, personalnummer);

        AbwesenheitDto responseDto = AbwesenheitDto.builder()
                .id(1)
                .type(AbwesenheitType.URLAU)
                .startDate(startDate)
                .endDate(endDate)
                .status(AbwesenheitStatus.VALID)
                .fullName("John Doe")
                .build();

        Benutzer mitarbeiter = createBenutzer(1, "john.doe@test.com", "John", "Doe", "john.doe@test.com");

        // Mock validation
        when(gatewayUserHolder.getUserId()).thenReturn(1);
        when(benutzerService.findById(1)).thenReturn(Optional.of(mitarbeiter));
        when(leistungserfassungService.isLeistungserfassungMonthClosed(anyInt(), anyString(), any())).thenReturn(false);
        when(zeitbuchungService.findZeitbuchungenInPeriodAndAnAbwesenheit(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(zeitausgleichService.findByPersonalnummerInPeriod(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(abwesenheitService.findAbwesenheitBetweenDatesAndStatuses(anyInt(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(environmentService.getPersonalnummer()).thenReturn(personalnummer);

        // Mock LHR service response
        ResponseEntity<AbwesenheitDto> lhrResponse = ResponseEntity.ok(responseDto);
        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class))).thenReturn(lhrResponse);

        // Mock abwesenheit lookup
        when(abwesenheitService.findByIdForceUpdate(1)).thenReturn(Optional.of(savedAbwesenheit));
        when(benutzerService.findByPersonalnummerAndFirmaBmdClient(anyString(), anyString())).thenReturn(mitarbeiter);

        // Mock Führungskraft not found
        when(environmentService.isProduction()).thenReturn(true);
        when(adresseIbosService.getFuehrungskraftUPNFromLogin(anyString())).thenReturn(null);
        when(abwesenheitService.save(any(Abwesenheit.class))).thenReturn(savedAbwesenheit);
        when(abwesenheitService.mapToAbwesenheitDto(any(Abwesenheit.class))).thenReturn(responseDto);

        // When
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify no email was attempted (no Führungskraft, but still VALID status)
        verify(mailService, never()).sendEmail(anyString(), anyString(), any(), any(), any(), any());
    }

    // ========================================
    // Helper methods for creating test objects
    // ========================================

    private Personalnummer createPersonalnummerWithFirma() {
        Personalnummer personalnummer = new Personalnummer();
        personalnummer.setId(1);
        personalnummer.setPersonalnummer("123456");
        personalnummer.setFirma(createFirma());
        return personalnummer;
    }

    private Firma createFirma() {
        Firma firma = new Firma();
        firma.setId(1);
        firma.setBmdClient("TEST_CLIENT");
        return firma;
    }

    private Abwesenheit createAbwesenheit(Integer id, LocalDate von, LocalDate bis, AbwesenheitStatus status, Personalnummer personalnummer) {
        Abwesenheit abwesenheit = new Abwesenheit();
        abwesenheit.setId(id);
        abwesenheit.setVon(von);
        abwesenheit.setBis(bis);
        abwesenheit.setStatus(status);
        abwesenheit.setPersonalnummer(personalnummer);
        abwesenheit.setFuehrungskraefte(new HashSet<>());
        return abwesenheit;
    }

    private Benutzer createBenutzer(Integer id, String upn, String firstName, String lastName, String email) {
        Benutzer benutzer = new Benutzer();
        benutzer.setId(id);
        benutzer.setUpn(upn);
        benutzer.setFirstName(firstName);
        benutzer.setLastName(lastName);
        benutzer.setEmail(email);

        Personalnummer personalnummer = createPersonalnummerWithFirma();
        benutzer.setPersonalnummer(personalnummer);

        return benutzer;
    }

}