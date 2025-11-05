package com.ibosng.validationservice.teilnehmer.validations;

import com.ibosng.dbservice.entities.teilnehmer.Teilnehmer;
import com.ibosng.dbservice.entities.teilnehmer.TeilnehmerSource;
import com.ibosng.dbservice.entities.teilnehmer.TeilnehmerStaging;
import com.ibosng.validationservice.Validation;
import com.ibosng.validationservice.config.DataSourceConfigTest;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.teilnehmer.validations.imported.TeilnehmerNachnameValidation;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DataSourceConfigTest.class)
public class TeilnehmerNachnameValidationTest {

    @Mock
    private ValidationUserHolder validationUserHolder;

    @Test
    void testExecuteValidation_validNachname() {

        TeilnehmerStaging teilnehmerStaging = new TeilnehmerStaging();
        Teilnehmer teilnehmer = new Teilnehmer();
        teilnehmerStaging.setSource(TeilnehmerSource.VHS);
        teilnehmerStaging.setNachname("Müller"); // Set with the valid surname
        teilnehmer.setErrors(new ArrayList<>());
        Validation<TeilnehmerStaging, Teilnehmer> validation = new TeilnehmerNachnameValidation(validationUserHolder);

        boolean result = validation.executeValidation(teilnehmerStaging, teilnehmer);

        assertTrue(result);
        assertTrue(teilnehmer.getErrors().isEmpty());
    }

    @Test
    void testExecuteValidation_invalidNachname() {

        TeilnehmerStaging teilnehmerStaging = new TeilnehmerStaging();
        teilnehmerStaging.setSource(TeilnehmerSource.VHS);
        Teilnehmer teilnehmer = new Teilnehmer();
        teilnehmerStaging.setNachname("Müller123"); // Invalid surname with numbers
        teilnehmer.setErrors(new ArrayList<>());
        Validation<TeilnehmerStaging, Teilnehmer> validation = new TeilnehmerNachnameValidation(validationUserHolder);

        boolean result = validation.executeValidation(teilnehmerStaging, teilnehmer);

        assertFalse(result);
        assertEquals(1, teilnehmer.getErrors().size());
        assertEquals("nachname", teilnehmer.getErrors().get(0).getError());
    }

}
