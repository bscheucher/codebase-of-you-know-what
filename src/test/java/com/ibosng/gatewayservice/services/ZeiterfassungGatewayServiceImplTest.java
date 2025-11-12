package com.ibosng.gatewayservice.services;

import com.ibosng.dbmapperservice.services.ZeitbuchungenMapperService;
import com.ibosng.dbservice.dtos.mitarbeiter.AbwesenheitDto;
import com.ibosng.dbservice.entities.Benutzer;
import com.ibosng.dbservice.entities.Zeitausgleich;
import com.ibosng.dbservice.entities.lhr.Abwesenheit;
import com.ibosng.dbservice.entities.lhr.AbwesenheitStatus;
import com.ibosng.dbservice.entities.masterdata.Personalnummer;
import com.ibosng.dbservice.services.ZeitausgleichService;
import com.ibosng.dbservice.services.lhr.AbwesenheitService;
import com.ibosng.dbservice.services.mitarbeiter.PersonalnummerService;
import com.ibosng.dbservice.services.zeitbuchung.LeistungserfassungService;
import com.ibosng.dbservice.services.zeitbuchung.ZeitbuchungService;
import com.ibosng.dbservice.services.zeiterfassung.AuszahlungsantragService;
import com.ibosng.dbservice.services.zeiterfassung.ZeitspeicherService;
import com.ibosng.gatewayservice.dtos.response.PayloadResponse;
import com.ibosng.gatewayservice.enums.PayloadTypes;
import com.ibosng.gatewayservice.services.impl.ZeiterfassungGatewayServiceImpl;
import com.ibosng.gatewayservice.utils.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private ZeitbuchungenMapperService zeitbuchungenMapper;

    @Mock
    private PersonalnummerService personalnummerService;

    @Mock
    private Helpers helpers;

    @Mock
    private AuszahlungsantragService auszahlungsantragService;

    @Mock
    private ZeitausgleichService zeitausgleichService;


    @Test
    void testGetAbwesenheit_Found() {
        // Given
        Integer abwesenheitId = 1;
        Abwesenheit abwesenheit = new Abwesenheit();
        abwesenheit.setId(abwesenheitId);

        AbwesenheitDto expectedDto = AbwesenheitDto.builder().id(abwesenheitId).build();

        when(abwesenheitService.findById(abwesenheitId)).thenReturn(Optional.of(abwesenheit));
        when(abwesenheitService.mapToAbwesenheitDto(abwesenheit)).thenReturn(expectedDto);

        // When
        PayloadResponse response = zeiterfassungGatewayService.getAbwesenheit(abwesenheitId);

        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals(PayloadTypes.ABWESENHEIT.getValue(), response.getData().get(0).getType());
        assertEquals(1, response.getData().get(0).getAttributes().size());
        assertEquals(expectedDto, response.getData().get(0).getAttributes().get(0));

        verify(abwesenheitService).findById(abwesenheitId);
        verify(abwesenheitService).mapToAbwesenheitDto(abwesenheit);
    }

    @Test
    void testGetAbwesenheit_NotFound() {
        // Given
        Integer abwesenheitId = 999;

        when(abwesenheitService.findById(abwesenheitId)).thenReturn(Optional.empty());
        when(zeitausgleichService.findAllZeitausgleichInPeriod(abwesenheitId)).thenReturn(Collections.emptyList());

        // When
        PayloadResponse response = zeiterfassungGatewayService.getAbwesenheit(abwesenheitId);

        // Then
        assertFalse(response.isSuccess());

        verify(abwesenheitService).findById(abwesenheitId);
        verify(zeitausgleichService).findAllZeitausgleichInPeriod(abwesenheitId);
    }

    @Test
    void testGetAbwesenheit_FallbackToZeitausgleich() {
        // Given
        Integer abwesenheitId = 1;
        Zeitausgleich zeitausgleich = new Zeitausgleich();
        zeitausgleich.setId(abwesenheitId);

        AbwesenheitDto expectedDto = AbwesenheitDto.builder().id(abwesenheitId).build();

        when(abwesenheitService.findById(abwesenheitId)).thenReturn(Optional.empty());
        when(zeitausgleichService.findAllZeitausgleichInPeriod(abwesenheitId))
            .thenReturn(List.of(zeitausgleich));
        when(zeitausgleichService.mapListZeitausgleichToListAbwesenheitDto(List.of(zeitausgleich)))
            .thenReturn(List.of(expectedDto));

        // When
        PayloadResponse response = zeiterfassungGatewayService.getAbwesenheit(abwesenheitId);

        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(expectedDto, response.getData().get(0).getAttributes().get(0));

        verify(abwesenheitService).findById(abwesenheitId);
        verify(zeitausgleichService).findAllZeitausgleichInPeriod(abwesenheitId);
    }

    @Test
    void testDeleteAbwesenheit_AcceptedStatus() {
        // Given
        Integer abwesenheitId = 1;
        Abwesenheit abwesenheit = new Abwesenheit();
        abwesenheit.setId(abwesenheitId);
        abwesenheit.setStatus(AbwesenheitStatus.ACCEPTED);

        Abwesenheit savedAbwesenheit = new Abwesenheit();
        savedAbwesenheit.setId(abwesenheitId);
        savedAbwesenheit.setStatus(AbwesenheitStatus.REQUEST_CANCELLATION);

        AbwesenheitDto expectedDto = AbwesenheitDto.builder()
            .id(abwesenheitId)
            .status(AbwesenheitStatus.REQUEST_CANCELLATION)
            .build();

        when(abwesenheitService.findById(abwesenheitId)).thenReturn(Optional.of(abwesenheit));
        when(abwesenheitService.save(any(Abwesenheit.class))).thenReturn(savedAbwesenheit);
        when(abwesenheitService.mapToAbwesenheitDto(savedAbwesenheit)).thenReturn(expectedDto);

        // When
        PayloadResponse response = zeiterfassungGatewayService.deleteAbwesenheit(abwesenheitId);

        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(PayloadTypes.ABWESENHEIT.getValue(), response.getData().get(0).getType());

        verify(abwesenheitService).findById(abwesenheitId);
        verify(abwesenheitService).save(any(Abwesenheit.class));
    }

    @Test
    void testDeleteAbwesenheit_WrongStatus() {
        // Given
        Integer abwesenheitId = 1;
        Abwesenheit abwesenheit = new Abwesenheit();
        abwesenheit.setId(abwesenheitId);
        abwesenheit.setStatus(AbwesenheitStatus.VALID); // Wrong status

        when(abwesenheitService.findById(abwesenheitId)).thenReturn(Optional.of(abwesenheit));

        // When
        PayloadResponse response = zeiterfassungGatewayService.deleteAbwesenheit(abwesenheitId);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Wrong urlaube status", response.getMessage());

        verify(abwesenheitService).findById(abwesenheitId);
        verify(abwesenheitService, never()).save(any());
    }

    @Test
    void testDeleteAbwesenheit_NotFound() {
        // Given
        Integer abwesenheitId = 999;

        when(abwesenheitService.findById(abwesenheitId)).thenReturn(Optional.empty());
        when(zeitausgleichService.findAllZeitausgleichInPeriod(abwesenheitId))
            .thenReturn(Collections.emptyList());

        // When
        PayloadResponse response = zeiterfassungGatewayService.deleteAbwesenheit(abwesenheitId);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("No abwesenheit found found", response.getMessage());

        verify(abwesenheitService).findById(abwesenheitId);
        verify(zeitausgleichService).findAllZeitausgleichInPeriod(abwesenheitId);
    }

}