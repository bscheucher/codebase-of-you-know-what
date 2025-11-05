package com.ibosng.validationservice.teilnehmer.validations;

import com.ibosng.dbservice.entities.teilnehmer.Teilnehmer;
import com.ibosng.dbservice.entities.teilnehmer.Teilnehmer2Seminar;
import com.ibosng.dbservice.entities.teilnehmer.TeilnehmerSource;
import com.ibosng.dbservice.entities.teilnehmer.TeilnehmerStaging;
import com.ibosng.validationservice.config.DataSourceConfigTest;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.teilnehmer.validations.imported.GeplantValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DataSourceConfigTest.class)
public class GeplantValidationTest {

    private GeplantValidation validation;
    private TeilnehmerStaging teilnehmerStaging;
    private Teilnehmer teilnehmer;
    private Teilnehmer2Seminar teilnehmer2Seminar;

    @Mock
    private ValidationUserHolder validationUserHolder;

    @BeforeEach
    void setUp() {
        validation = new GeplantValidation(validationUserHolder);
        teilnehmerStaging = new TeilnehmerStaging();
        teilnehmerStaging.setSource(TeilnehmerSource.VHS);
        teilnehmer = new Teilnehmer();
        teilnehmer.setErrors(new ArrayList<>());
        teilnehmer2Seminar = new Teilnehmer2Seminar();
        teilnehmer2Seminar.setTeilnehmer(teilnehmer);
    }


    @Test
    void whenZubuchungIsValid_thenValidationSucceeds() {
        teilnehmerStaging.setGeplant("02.01.2023");

        boolean result = validation.executeValidation(teilnehmerStaging, teilnehmer2Seminar);

        assertTrue(result);
        assertTrue(teilnehmer.getErrors().isEmpty());
    }

    @Test
    void whenZubuchungIsInvalid_thenValidationFails() {
        teilnehmerStaging.setGeplant("invalid-date");

        boolean result = validation.executeValidation(teilnehmerStaging, teilnehmer2Seminar);

        assertFalse(result);
        assertEquals(1, teilnehmer.getErrors().size());
        assertEquals("geplant", teilnehmer.getErrors().get(0).getError());
    }


}
