package com.ibosng.validationservice.teilnehmer.validations;

import com.ibosng.dbservice.entities.Land;
import com.ibosng.dbservice.entities.teilnehmer.Teilnehmer;
import com.ibosng.dbservice.entities.teilnehmer.TeilnehmerSource;
import com.ibosng.dbservice.entities.teilnehmer.TeilnehmerStaging;
import com.ibosng.dbservice.services.LandService;
import com.ibosng.validationservice.Validation;
import com.ibosng.validationservice.config.DataSourceConfigTest;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.teilnehmer.validations.imported.NationValidation;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@SpringBootTest(classes = DataSourceConfigTest.class)
public class NationValidationTest {

    @Mock
    private LandService landService;

    @Mock
    private ValidationUserHolder validationUserHolder;


    @Test
    void testExecuteValidation_validNation() {

        TeilnehmerStaging teilnehmerStaging = new TeilnehmerStaging();
        teilnehmerStaging.setSource(TeilnehmerSource.VHS);
        Teilnehmer teilnehmer = new Teilnehmer();
        teilnehmerStaging.setNation("AT"); // Set with the valid nation
        teilnehmer.setErrors(new ArrayList<>());
        Land land = new Land();
        when(landService.findByEldaCode(any())).thenReturn(land);
        when(landService.getLandFromCountryCode(any())).thenReturn(land);
        Validation<TeilnehmerStaging, Teilnehmer> validation = new NationValidation(landService, validationUserHolder);

        boolean result = validation.executeValidation(teilnehmerStaging, teilnehmer);

        assertTrue(result);
        assertTrue(teilnehmer.getErrors().isEmpty());
    }

    @Test
    void testExecuteValidation_emptyNation() {

        TeilnehmerStaging teilnehmerStaging = new TeilnehmerStaging();
        teilnehmerStaging.setSource(TeilnehmerSource.VHS);
        Teilnehmer teilnehmer = new Teilnehmer();
        teilnehmer.setErrors(new ArrayList<>());
        when(landService.findByEldaCode(any())).thenReturn(null);
        when(landService.getLandFromCountryCode(any())).thenReturn(null);
        Validation<TeilnehmerStaging, Teilnehmer> validation = new NationValidation(landService, validationUserHolder);

        boolean result = validation.executeValidation(teilnehmerStaging, teilnehmer);

        assertTrue(result);
        assertTrue(teilnehmer.getErrors().isEmpty());
    }

}
