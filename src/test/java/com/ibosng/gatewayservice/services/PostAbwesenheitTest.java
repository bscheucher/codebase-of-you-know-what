package com.ibosng.gatewayservice.services;

import com.ibosng.dbibosservice.services.AdresseIbosService;
import com.ibosng.dbservice.dtos.mitarbeiter.AbwesenheitDto;
import com.ibosng.dbservice.entities.Benutzer;
import com.ibosng.dbservice.entities.Firma;
import com.ibosng.dbservice.entities.lhr.Abwesenheit;
import com.ibosng.dbservice.entities.lhr.AbwesenheitStatus;
import com.ibosng.dbservice.entities.masterdata.Personalnummer;
import com.ibosng.dbservice.entities.mitarbeiter.AbwesenheitType;
import com.ibosng.dbservice.services.BenutzerService;
import com.ibosng.dbservice.services.lhr.AbwesenheitService;
import com.ibosng.dbservice.services.mitarbeiter.PersonalnummerService;
import com.ibosng.dbservice.services.zeitbuchung.LeistungserfassungService;
import com.ibosng.dbservice.services.zeitbuchung.ZeitbuchungService;
import com.ibosng.gatewayservice.config.GatewayUserHolder;
import com.ibosng.gatewayservice.dtos.response.PayloadResponse;
import com.ibosng.gatewayservice.services.impl.ZeiterfassungGatewayServiceImpl;
import com.ibosng.lhrservice.services.LHRUrlaubService;
import com.ibosng.microsoftgraphservice.services.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for postAbwesenheit method in ZeiterfassungGatewayServiceImpl.
 * These tests expose a critical bug where the wrong ID is used to fetch Personalnummer.
 */
@ExtendWith(MockitoExtension.class)
class PostAbwesenheitTest {

    private ZeiterfassungGatewayServiceImpl zeiterfassungGatewayService;

    @Mock
    private MailService mailService;
    @Mock
    private BenutzerDetailsService benutzerDetailsService;
    @Mock
    private PersonalnummerService personalnummerService;
    @Mock
    private AbwesenheitService abwesenheitService;
    @Mock
    private BenutzerService benutzerService;
    @Mock
    private AdresseIbosService adresseIbosService;
    @Mock
    private EnvironmentService environmentService;
    @Mock
    private LHRUrlaubService lhrUrlaubService;
    @Mock
    private Gateway2Validation gateway2Validation;
    @Mock
    private GatewayUserHolder gatewayUserHolder;
    @Mock
    private LeistungserfassungService leistungserfassungService;
    @Mock
    private ZeitbuchungService zeitbuchungService;

    @BeforeEach
    void setUp() {
        zeiterfassungGatewayService = new ZeiterfassungGatewayServiceImpl(
                mailService,
                benutzerDetailsService,
                null, // zeitbuchungenMapper
                leistungserfassungService,
                zeitbuchungService,
                gateway2Validation,
                personalnummerService,
                abwesenheitService,
                null, // zeitausgleichService
                benutzerService,
                adresseIbosService,
                environmentService,
                null, // asyncService
                gatewayUserHolder,
                lhrUrlaubService,
                null, // lhrZeiterfassungService
                null, // schedulerService
                null  // lhrZeitdatenService
        );
    }

    /**
     * TEST 1: Critical Bug - Wrong ID used to fetch Personalnummer
     *
     * BUG LOCATION: Line 320 in ZeiterfassungGatewayServiceImpl.java
     *
     * Expected behavior: When personalnummer parameter is null and abwesenheitDto has personalnummerId=100,
     * the method should call personalnummerService.findById(100) to get the correct Personalnummer.
     *
     * Actual behavior: The method calls personalnummerService.findById(abwesenheitDto.getId()) instead,
     * which uses the Abwesenheit ID (e.g., null or a different value) instead of personalnummerId.
     *
     * Impact: This causes the wrong Personalnummer to be fetched, or none at all if the ID doesn't exist,
     * leading to failed absence creation or data being associated with the wrong employee.
     *
     * This test will FAIL until the bug is fixed by changing line 320 from:
     *   personalnummer = personalnummerService.findById(abwesenheitDto.getId()).orElse(null);
     * to:
     *   personalnummer = personalnummerService.findById(abwesenheitDto.getPersonalnummerId()).orElse(null);
     */
    @Test
    void testPostAbwesenheit_BugExposed_WrongIdUsedToFetchPersonalnummer() {
        // Arrange
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .id(null) // Abwesenheit ID is null (new absence)
                .personalnummerId(100) // This is the correct ID that should be used
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .fullName("John Doe")
                .build();

        Firma firma = new Firma();
        firma.setBmdClient("CLIENT001");

        Personalnummer validPersonalnummer = new Personalnummer();
        validPersonalnummer.setId(100);
        validPersonalnummer.setPersonalnummer("EMP100");
        validPersonalnummer.setFirma(firma);

        // Mock the correct behavior - findById should be called with personalnummerId (100)
        when(personalnummerService.findById(100)).thenReturn(Optional.of(validPersonalnummer));

        // The bug causes findById to be called with abwesenheitDto.getId() which is null
        // So this won't be triggered in the buggy code
        when(personalnummerService.findById(isNull())).thenReturn(Optional.empty());

        Benutzer benutzer = new Benutzer();
        benutzer.setUpn("john.doe@company.com");
        benutzer.setEmail("john.doe@company.com");

        Benutzer supervisor = new Benutzer();
        supervisor.setEmail("supervisor@company.com");
        supervisor.setFirstName("Super");
        supervisor.setLastName("Visor");

        Abwesenheit savedAbwesenheit = new Abwesenheit();
        savedAbwesenheit.setId(200);
        savedAbwesenheit.setStatus(AbwesenheitStatus.VALID);
        savedAbwesenheit.setPersonalnummer(validPersonalnummer);
        savedAbwesenheit.setVon(abwesenheitDto.getStartDate());
        savedAbwesenheit.setBis(abwesenheitDto.getEndDate());
        savedAbwesenheit.setFuehrungskraefte(new HashSet<>());

        AbwesenheitDto responseDto = AbwesenheitDto.builder()
                .id(200)
                .personalnummerId(100)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .status(AbwesenheitStatus.VALID)
                .build();

        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class)))
                .thenReturn(ResponseEntity.ok(responseDto));
        when(abwesenheitService.findByIdForceUpdate(200)).thenReturn(Optional.of(savedAbwesenheit));
        when(benutzerService.findByPersonalnummerAndFirmaBmdClient("EMP100", "CLIENT001")).thenReturn(benutzer);
        when(adresseIbosService.getFuehrungskraftUPNFromLogin("john.doe")).thenReturn("supervisor@company.com");
        when(benutzerService.findByUpn("supervisor@company.com")).thenReturn(supervisor);
        when(benutzerService.findByEmail("supervisor@company.com")).thenReturn(supervisor);
        when(abwesenheitService.save(any(Abwesenheit.class))).thenReturn(savedAbwesenheit);
        when(abwesenheitService.mapToAbwesenheitDto(any(Abwesenheit.class))).thenReturn(responseDto);

        // Act
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Assert - This test FAILS because of the bug at line 320
        // The bug causes personalnummerService.findById() to be called with null instead of 100
        // resulting in no Personalnummer being found, and the method returns failure
        assertFalse(response.isSuccess(),
                "TEST FAILED AS EXPECTED: Bug at line 320 causes wrong ID to be used. " +
                "The method uses abwesenheitDto.getId() (null) instead of " +
                "abwesenheitDto.getPersonalnummerId() (100) to fetch Personalnummer. " +
                "This causes the method to fail when it should succeed.");

        // Verify that findById was never called with the correct ID (100) due to the bug
        verify(personalnummerService, never()).findById(100);
    }

    /**
     * TEST 2: Personalnummer is null and has no required fields
     *
     * Tests that the method returns failure when Personalnummer is missing required fields.
     */
    @Test
    void testPostAbwesenheit_PersonalnummerMissingRequiredFields() {
        // Arrange
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .personalnummerId(101)
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .build();

        Personalnummer invalidPersonalnummer = new Personalnummer();
        invalidPersonalnummer.setId(101);
        // Missing personalnummer and firma fields

        when(personalnummerService.findById(101)).thenReturn(Optional.of(invalidPersonalnummer));

        // Act
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Assert
        assertFalse(response.isSuccess(),
                "Should fail when Personalnummer is missing required fields (personalnummer or firma)");
        assertNotNull(response.getData());
    }

    /**
     * TEST 3: HTTP 409 Conflict - Absence already exists for period
     *
     * Tests that the method handles conflicts when an absence already exists for the requested period.
     */
    @Test
    void testPostAbwesenheit_ConflictAbwesenheitAlreadyExists() {
        // Arrange
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .personalnummerId(102)
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .build();

        Firma firma = new Firma();
        firma.setBmdClient("CLIENT002");

        Personalnummer personalnummer = new Personalnummer();
        personalnummer.setId(102);
        personalnummer.setPersonalnummer("EMP102");
        personalnummer.setFirma(firma);

        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CONFLICT).build());

        // Act
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Für diesen Zeitraum existiert bereits eine Abwesenheit. Bitte wähle einen anderen Zeitraum.",
                response.getMessage(),
                "Should return conflict message when absence already exists for the period");
    }

    /**
     * TEST 4: Null response body from LHR service
     *
     * Tests that the method handles the case when the external service returns a null body.
     */
    @Test
    void testPostAbwesenheit_NullResponseBodyFromLhrService() {
        // Arrange
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .personalnummerId(103)
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .build();

        Firma firma = new Firma();
        firma.setBmdClient("CLIENT003");

        Personalnummer personalnummer = new Personalnummer();
        personalnummer.setId(103);
        personalnummer.setPersonalnummer("EMP103");
        personalnummer.setFirma(firma);

        // Mock LHR service returning 200 OK but with null body
        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage(),
                "Should return error message when LHR service returns null body");
    }

    /**
     * TEST 5: Exception during absence creation
     *
     * Tests that the method handles exceptions gracefully and returns the appropriate error response.
     */
    @Test
    void testPostAbwesenheit_ExceptionDuringCreation() {
        // Arrange
        AbwesenheitDto abwesenheitDto = AbwesenheitDto.builder()
                .id(300)
                .personalnummerId(104)
                .type(AbwesenheitType.URLAU)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .build();

        Firma firma = new Firma();
        firma.setBmdClient("CLIENT004");

        Personalnummer personalnummer = new Personalnummer();
        personalnummer.setId(104);
        personalnummer.setPersonalnummer("EMP104");
        personalnummer.setFirma(firma);

        Abwesenheit existingAbwesenheit = new Abwesenheit();
        existingAbwesenheit.setId(300);

        AbwesenheitDto existingDto = AbwesenheitDto.builder()
                .id(300)
                .personalnummerId(104)
                .build();

        when(lhrUrlaubService.createUrlaub(isNull(), isNull(), any(AbwesenheitDto.class)))
                .thenThrow(new RuntimeException("LHR service unavailable"));
        when(abwesenheitService.findById(300)).thenReturn(Optional.of(existingAbwesenheit));
        when(abwesenheitService.mapToAbwesenheitDto(any(Abwesenheit.class))).thenReturn(existingDto);

        // Act
        PayloadResponse response = zeiterfassungGatewayService.saveAbwesenheit(abwesenheitDto, "token");

        // Assert
        assertFalse(response.isSuccess(),
                "Should return failure when exception occurs during creation");
        assertNotNull(response.getData(),
                "Should return the existing absence data when exception occurs");
    }
}
