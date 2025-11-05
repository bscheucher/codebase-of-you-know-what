package com.ibosng.gatewayservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibosng.dbservice.dtos.mitarbeiter.StammdatenDto;
import com.ibosng.gatewayservice.dtos.response.PayloadResponse;
import com.ibosng.gatewayservice.exceptions.MissingSozialversicherungsnummerException;
import com.ibosng.gatewayservice.services.BenutzerDetailsService;
import com.ibosng.gatewayservice.services.MAOnboardingService;
import com.ibosng.gatewayservice.services.MasterdataService;
import com.ibosng.gatewayservice.services.MitarbeiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MitarbeiterController.class)
@DisplayName("MitarbeiterController - Stammdaten Tests for Teilnehmer without Sozialversicherungsnummer")
class MitarbeiterControllerStammdatenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "maOnboardingServiceImpl")
    private MAOnboardingService maOnboardingService;

    @MockBean(name = "mitarbeiterServiceImpl")
    private MitarbeiterService mitarbeiterService;

    @MockBean
    private MasterdataService masterdataService;

    @MockBean
    private BenutzerDetailsService benutzerDetailsService;

    @MockBean
    private com.ibosng.gatewayservice.services.TokenValidatorService tokenValidatorService;

    @MockBean
    private com.ibosng.dbservice.services.BenutzerService benutzerService;

    private static final String AUTH_TOKEN = "Bearer valid-token-12345";
    private static final String TOKEN = "valid-token-12345";
    private static final String PERSONALNUMMER = "T12345";


    private StammdatenDto stammdatenDto;

    @BeforeEach
    void setUp() {
        //Create mock StammdatenDto without SVNR
        stammdatenDto = new StammdatenDto();
        stammdatenDto.setId(1);
        stammdatenDto.setPersonalnummer("T12345");
        stammdatenDto.setFirma("IBOS GmbH");
        stammdatenDto.setAnrede("Herr");
        stammdatenDto.setTitel("Mag.");
        stammdatenDto.setTitel2("MSc");
        stammdatenDto.setNachname("Mustermann");
        stammdatenDto.setVorname("Max");
        stammdatenDto.setGeburtsname("Muster");
        stammdatenDto.setSvnr(null); // intentionally null for this test
        stammdatenDto.setEcard("E123456789");
        stammdatenDto.setGeschlecht("M");
        stammdatenDto.setFamilienstand("Ledig");
        stammdatenDto.setGeburtsDatum("1991-03-12");
        stammdatenDto.setAlter("34");
        stammdatenDto.setStaatsbuergerschaft("Österreich");
        stammdatenDto.setMuttersprache("Deutsch");
        stammdatenDto.setStrasse("Hauptstraße 15");
        stammdatenDto.setLand("Österreich");
        stammdatenDto.setPlz("1010");
        stammdatenDto.setOrt("Wien");
        stammdatenDto.setAStrasse("Nebenstraße 5");
        stammdatenDto.setALand("Österreich");
        stammdatenDto.setAPlz("1020");
        stammdatenDto.setAOrt("Wien");
        stammdatenDto.setEmail("max.mustermann@example.com");
        stammdatenDto.setMobilnummer("+43 660 1234567");
        stammdatenDto.setHandySignatur(true);
        stammdatenDto.setBank("Erste Bank");
        stammdatenDto.setIban("AT611904300234573201");
        stammdatenDto.setBic("GIBAATWWXXX");
        stammdatenDto.setBankcard("BC123456789");
        stammdatenDto.setBurgenland(false);
        stammdatenDto.setKaernten(false);
        stammdatenDto.setNiederoesterreich(false);
        stammdatenDto.setOberoesterreich(false);
        stammdatenDto.setSalzburg(false);
        stammdatenDto.setSteiermark(false);
        stammdatenDto.setTirol(false);
        stammdatenDto.setVorarlberg(false);
        stammdatenDto.setWien(true);
        stammdatenDto.setArbeitsgenehmigungDok("genehmigung.pdf");
        stammdatenDto.setGueltigBis("2030-12-31");
        stammdatenDto.setArbeitsgenehmigung("Ja");
        stammdatenDto.setFoto("https://example.com/fotos/max_mustermann.jpg");


        // Mock token validation and user retrieval
        when(tokenValidatorService.isTokenValid(anyString())).thenReturn(true);
    }

    // ==================== POST /mitarbeiter/stammdaten/edit/{personalnummer} ====================

    @Test
    @DisplayName("Should throw exception when saving Teilnehmer Stammdaten without Sozialversicherungsnummer")
    void saveStammdaten_TeilnehmerWithoutSVNR_ShouldThrowException() throws Exception {
        // Arrange
        when(mitarbeiterService.saveStammdaten(
                any(StammdatenDto.class),
                anyInt(),
                anyBoolean(),
                anyString()
        )).thenThrow(new MissingSozialversicherungsnummerException(
                "Sozialversicherungsnummer is required for Teilnehmer"
        ));

        // Act & Assert
        mockMvc.perform(post("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("workflowId", "1")
                        .param("isOnboarding", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stammdatenDto)))
                .andExpect(status().isBadRequest());

        // Verify service was called
        verify(mitarbeiterService, times(1)).saveStammdaten(
                any(StammdatenDto.class),
                eq(1),
                eq(true),
                eq(AUTH_TOKEN)
        );
    }

    @Test
    @DisplayName("Should throw exception when saving Teilnehmer Stammdaten without SVNR during onboarding")
    void saveStammdaten_TeilnehmerOnboardingWithoutSVNR_ShouldThrowException() throws Exception {
        // Arrange
        when(mitarbeiterService.saveStammdaten(
                any(StammdatenDto.class),
                isNull(),
                eq(true),
                anyString()
        )).thenThrow(new MissingSozialversicherungsnummerException(
                "Sozialversicherungsnummer is mandatory during Teilnehmer onboarding"
        ));

        // Act & Assert
        mockMvc.perform(post("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("isOnboarding", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stammdatenDto)))
                .andExpect(status().isBadRequest());

        verify(mitarbeiterService, times(1)).saveStammdaten(
                any(StammdatenDto.class),
                isNull(),
                eq(true),
                eq(AUTH_TOKEN)
        );
    }

    @Test
    @DisplayName("Should throw exception when saving Teilnehmer Stammdaten without SVNR (non-onboarding)")
    void saveStammdaten_TeilnehmerNonOnboardingWithoutSVNR_ShouldThrowException() throws Exception {
        // Arrange
        when(mitarbeiterService.saveStammdaten(
                any(StammdatenDto.class),
                anyInt(),
                eq(false),
                anyString()
        )).thenThrow(new MissingSozialversicherungsnummerException(
                "Sozialversicherungsnummer cannot be null for Teilnehmer"
        ));

        // Act & Assert
        mockMvc.perform(post("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("workflowId", "2")
                        .param("isOnboarding", "false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stammdatenDto)))
                .andExpect(status().isBadRequest());

        verify(mitarbeiterService, times(1)).saveStammdaten(
                any(StammdatenDto.class),
                eq(2),
                eq(false),
                eq(AUTH_TOKEN)
        );
    }

    // ==================== POST /mitarbeiter/stammdaten/edit (without path variable) ====================

    @Test
    @DisplayName("Should throw exception when creating new Teilnehmer without Sozialversicherungsnummer")
    void saveStammdaten_NewTeilnehmerWithoutSVNR_ShouldThrowException() throws Exception {
        // Arrange
        stammdatenDto.setPersonalnummer(null); // New Teilnehmer, no personalnummer yet

        when(mitarbeiterService.saveStammdaten(
                any(StammdatenDto.class),
                isNull(),
                eq(true),
                anyString()
        )).thenThrow(new MissingSozialversicherungsnummerException(
                "Sozialversicherungsnummer is required when creating a new Teilnehmer"
        ));

        // Act & Assert
        mockMvc.perform(post("/mitarbeiter/stammdaten/edit")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("isOnboarding", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stammdatenDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sozialversicherungsnummer is required when creating a new Teilnehmer"));

        verify(mitarbeiterService, times(1)).saveStammdaten(
                any(StammdatenDto.class),
                isNull(),
                eq(true),
                eq(AUTH_TOKEN)
        );
    }

    @Test
    @DisplayName("Should throw exception when empty Sozialversicherungsnummer is provided")
    void saveStammdaten_TeilnehmerWithEmptySVNR_ShouldThrowException() throws Exception {
        // Arrange
        stammdatenDto.setSvnr(""); // Empty string instead of null

        when(mitarbeiterService.saveStammdaten(
                any(StammdatenDto.class),
                anyInt(),
                anyBoolean(),
                anyString()
        )).thenThrow(new MissingSozialversicherungsnummerException(
                "Sozialversicherungsnummer cannot be empty for Teilnehmer"
        ));

        // Act & Assert
        mockMvc.perform(post("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("workflowId", "1")
                        .param("isOnboarding", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stammdatenDto)))
                .andExpect(status().isBadRequest());

        verify(mitarbeiterService, times(1)).saveStammdaten(
                any(StammdatenDto.class),
                eq(1),
                eq(true),
                eq(AUTH_TOKEN)
        );
    }

    // ==================== GET /mitarbeiter/stammdaten/edit/{personalnummer} ====================

    @Test
    @DisplayName("Should throw exception when retrieving Teilnehmer Stammdaten without Sozialversicherungsnummer")
    void getStammdaten_TeilnehmerWithoutSVNR_ShouldThrowException() throws Exception {
        // Arrange
        when(mitarbeiterService.getStammdaten(
                eq(PERSONALNUMMER),
                eq(false),
                anyString()
        )).thenThrow(new MissingSozialversicherungsnummerException(
                "Retrieved Teilnehmer data is missing Sozialversicherungsnummer"
        ));

        // Act & Assert
        mockMvc.perform(get("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("isOnboarding", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Retrieved Teilnehmer data is missing Sozialversicherungsnummer"));

        verify(mitarbeiterService, times(1)).getStammdaten(
                eq(PERSONALNUMMER),
                eq(false),
                eq(AUTH_TOKEN)
        );
    }

    @Test
    @DisplayName("Should throw exception when retrieving Teilnehmer Stammdaten during onboarding without SVNR")
    void getStammdaten_TeilnehmerOnboardingWithoutSVNR_ShouldThrowException() throws Exception {
        // Arrange
        when(mitarbeiterService.getStammdaten(
                eq(PERSONALNUMMER),
                eq(true),
                anyString()
        )).thenThrow(new MissingSozialversicherungsnummerException(
                "Teilnehmer onboarding data must contain Sozialversicherungsnummer"
        ));

        // Act & Assert
        mockMvc.perform(get("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("isOnboarding", "true"))
                .andExpect(status().isBadRequest());

        verify(mitarbeiterService, times(1)).getStammdaten(
                eq(PERSONALNUMMER),
                eq(true),
                eq(AUTH_TOKEN)
        );
    }

    // ==================== Positive Test Cases ====================

    @Test
    @DisplayName("Should successfully save Teilnehmer Stammdaten with valid Sozialversicherungsnummer")
    void saveStammdaten_TeilnehmerWithValidSVNR_ShouldSucceed() throws Exception {
        // Arrange
        stammdatenDto.setSvnr("1234010175"); // Valid SVNR

        PayloadResponse successResponse = new PayloadResponse();
        successResponse.setSuccess(true);
        successResponse.setMessage("Stammdaten saved successfully");

        when(mitarbeiterService.saveStammdaten(
                any(StammdatenDto.class),
                anyInt(),
                anyBoolean(),
                anyString()
        )).thenReturn(ResponseEntity.ok(successResponse));

        // Act & Assert
        mockMvc.perform(post("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("workflowId", "1")
                        .param("isOnboarding", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stammdatenDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Stammdaten saved successfully"));

        verify(mitarbeiterService, times(1)).saveStammdaten(
                any(StammdatenDto.class),
                eq(1),
                eq(true),
                eq(AUTH_TOKEN)
        );
    }

   }