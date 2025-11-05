package com.ibosng.validationservice.teilnehmer.validations.imported;

import com.ibosng.dbservice.entities.Plz;
import com.ibosng.dbservice.entities.teilnehmer.Teilnehmer;
import com.ibosng.dbservice.entities.teilnehmer.TeilnehmerSource;
import com.ibosng.dbservice.entities.teilnehmer.TeilnehmerStaging;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.teilnehmer.validations.AbstractValidation;
import com.ibosng.validationservice.validations.PLZValidation;
import lombok.RequiredArgsConstructor;

/**
 * Required for VHS, eAMS, MDLC
 */
@RequiredArgsConstructor
public class TeilnehmerPlzValidation extends AbstractValidation<TeilnehmerStaging, Teilnehmer> {
    private final PLZValidation plzValidation;
    private final ValidationUserHolder validationUserHolder;

    @Override
    public boolean shouldValidationRun() {
        return !getSources().contains(TeilnehmerSource.OEIF);
    }

    @Override
    public boolean executeValidation(TeilnehmerStaging teilnehmerStaging, Teilnehmer teilnehmer) {
        Plz plz = plzValidation.validatePlz(teilnehmerStaging.getPlz());
        if (plz != null) {
            return true;
        }
        teilnehmer.addError("plz", "Ungültige PLZ angegeben", validationUserHolder.getUsername());
        return false;
    }
}
