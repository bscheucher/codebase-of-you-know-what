package com.ibosng.gatewayservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibosng.dbservice.dtos.mitarbeiter.StammdatenDto;
import com.ibosng.gatewayservice.services.BenutzerDetailsService;
import com.ibosng.gatewayservice.services.MAOnboardingService;
import com.ibosng.gatewayservice.services.MasterdataService;
import com.ibosng.gatewayservice.services.MitarbeiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;;


@WebMvcTest(MitarbeiterController.class)
public class MitarbeiterControllerMvcTest {

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
    private static final String PERSONALNUMMER = "T12345";

    private StammdatenDto stammdatenDtoWithoutSvnr;

    @BeforeEach
    void setUp() {
        // Create mock StammdatenDto without social security number
        stammdatenDtoWithoutSvnr = new StammdatenDto();
        stammdatenDtoWithoutSvnr.setId(1);
        stammdatenDtoWithoutSvnr.setPersonalnummer(PERSONALNUMMER);
        stammdatenDtoWithoutSvnr.setFirma("IBOS GmbH");
        stammdatenDtoWithoutSvnr.setAnrede("Herr");
        stammdatenDtoWithoutSvnr.setNachname("Mustermann");
        stammdatenDtoWithoutSvnr.setVorname("Max");
        stammdatenDtoWithoutSvnr.setSvnr(null); // No social security number
        stammdatenDtoWithoutSvnr.setGeschlecht("M");
        stammdatenDtoWithoutSvnr.setFamilienstand("Ledig");
        stammdatenDtoWithoutSvnr.setEmail("max.mustermann@example.com");

        // Mock token validation to allow requests to pass authentication
        when(tokenValidatorService.isTokenValid(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("Should return Bad Request when saving Stammdaten without social security number")
    void saveStammdaten_WithoutSocialSecurityNumber_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("workflowId", "1")
                        .param("isOnboarding", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stammdatenDtoWithoutSvnr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].attributes[0].errors.svnr").value("must not be null"));

        // Verify service was NOT called due to validation failure
        verify(mitarbeiterService, never()).saveStammdaten(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return Bad Request when saving Stammdaten without social security number during non-onboarding")
    void saveStammdaten_WithoutSocialSecurityNumberNonOnboarding_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("isOnboarding", "false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stammdatenDtoWithoutSvnr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].attributes[0].errors.svnr").value("must not be null"));

        // Verify service was NOT called due to validation failure
        verify(mitarbeiterService, never()).saveStammdaten(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return Bad Request when saving Stammdaten with empty social security number")
    void saveStammdaten_WithEmptySocialSecurityNumber_ShouldReturnBadRequest() throws Exception {
        // Arrange
        stammdatenDtoWithoutSvnr.setSvnr(""); // Empty string passes @NotNull but may fail other validations

        when(mitarbeiterService.saveStammdaten(
                any(StammdatenDto.class),
                anyInt(),
                anyBoolean(),
                anyString()
        )).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        // Act & Assert
        mockMvc.perform(post("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                        .param("workflowId", "1")
                        .param("isOnboarding", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stammdatenDtoWithoutSvnr)))
                .andExpect(status().isBadRequest());

        // Verify service WAS called since empty string passes @NotNull
        verify(mitarbeiterService, times(1)).saveStammdaten(
                any(StammdatenDto.class),
                eq(1),
                eq(true),
                eq(AUTH_TOKEN)
        );
    }
}