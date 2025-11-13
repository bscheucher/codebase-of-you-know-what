package com.ibosng.gatewayservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibosng.dbservice.dtos.mitarbeiter.StammdatenDto;
import com.ibosng.dbservice.services.BenutzerService;
import com.ibosng.gatewayservice.services.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(MitarbeiterController.class)
public class MitarbeiterControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unused")
    @MockBean(name = "maOnboardingServiceImpl")
    private MAOnboardingService maOnboardingService;

    @MockBean(name = "mitarbeiterServiceImpl")
    private MitarbeiterService mitarbeiterService;

    @SuppressWarnings("unused")
    @MockBean
    private MasterdataService masterdataService;

    @SuppressWarnings("unused")
    @MockBean
    private BenutzerDetailsService benutzerDetailsService;

    @MockBean
    private TokenValidatorService tokenValidatorService;

    @SuppressWarnings("unused")
    @MockBean
    private BenutzerService benutzerService;

    private static final String AUTH_TOKEN = "Bearer valid-token-12345";
    private static final String PERSONALNUMMER = "T12345";

    @BeforeEach
    void setUp() {
        // Mock token validation to allow requests to pass authentication
        when(tokenValidatorService.isTokenValid(anyString())).thenReturn(true);
    }

    @SneakyThrows
    @Test
    void saveStammdaten_WithoutSocialSecurityNumber_ReturnBadRequest() {
        // Arrange
        StammdatenDto dto = createStammdatenDtoWithoutSvnr();

        // Act & Assert
        performPost(dto)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].attributes[0].errors.svnr").value("must not be blank"));

        verify(mitarbeiterService, never()).saveStammdaten(any(), any(), any(), any());
    }


    @SneakyThrows
    @Test
    void saveStammdaten_WithEmptySocialSecurityNumber_ShouldReturnBadRequest() {

        // Arrange
        StammdatenDto dto = createStammdatenDtoWithoutSvnr();
        dto.setSvnr("");

        // Act & Assert
        performPost(dto)
                .andExpect(jsonPath("$.data[0].attributes[0].errors.svnr").value("must not be blank"));

        verify(mitarbeiterService, never()).saveStammdaten(any(), any(), any(), any());
    }

    @SneakyThrows
    @Test
    void saveStammdaten_WithWhitespaceSocialSecurityNumber_ShouldReturnBadRequest() {


        // Arrange
        StammdatenDto dto = createStammdatenDtoWithoutSvnr();
        dto.setSvnr("   ");

        // Act & Assert
        performPost(dto)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].attributes[0].errors.svnr").value("must not be blank"));

        verify(mitarbeiterService, never()).saveStammdaten(any(), any(), any(), any());
    }

    @SneakyThrows
    @Test
    void saveStammdaten_WithNonEmptySocialSecurityNumber_ReturnOk() {

        // Arrange
        StammdatenDto dto = createStammdatenDtoWithoutSvnr();
        dto.setSvnr("1234010175");

        // Mock successful service response
        when(mitarbeiterService.saveStammdaten(
                dto,
                1,
                true,
                AUTH_TOKEN
        )).thenReturn(ResponseEntity.ok().build());

        // Act & Assert
        performPost(dto)
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @NotNull
    private ResultActions performPost(StammdatenDto dto) {
        return mockMvc.perform(post("/mitarbeiter/stammdaten/edit/{personalnummer}", PERSONALNUMMER)
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .param("workflowId", "1")
                .param("isOnboarding", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));
    }

    private StammdatenDto createStammdatenDtoWithoutSvnr() {
        var dto = new StammdatenDto();
        dto.setId(1);
        dto.setPersonalnummer(PERSONALNUMMER);
        dto.setFirma("ibis acam Bildungs GmbH");
        dto.setAnrede("Herr");
        dto.setNachname("Rehgeiss");
        dto.setVorname("Johann");
        dto.setSvnr(null);
        dto.setGeschlecht("M");
        dto.setFamilienstand("Ledig");
        dto.setEmail("johann.rehgeiss@ibisacam.at");
        return dto;
    }

}