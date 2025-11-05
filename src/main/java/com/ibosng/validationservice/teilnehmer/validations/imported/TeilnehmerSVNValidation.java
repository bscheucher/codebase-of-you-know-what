package com.ibosng.validationservice.teilnehmer.validations.imported;

import com.ibosng.dbservice.entities.teilnehmer.Teilnehmer;
import com.ibosng.dbservice.entities.teilnehmer.TeilnehmerStaging;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.teilnehmer.validations.AbstractValidation;
import com.ibosng.validationservice.validations.SVNValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Required for VHS, eAMS, MDLC
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class TeilnehmerSVNValidation extends AbstractValidation<TeilnehmerStaging, Teilnehmer> {

    private final ValidationUserHolder validationUserHolder;

    @Override
    public boolean executeValidation(TeilnehmerStaging teilnehmerStaging, Teilnehmer teilnehmer) {
        String svn = SVNValidation.validateSVN(teilnehmerStaging.getSvNummer(), teilnehmer.getGeburtsdatum() != null ? teilnehmer.getGeburtsdatum() : null);
        if (svn != null) {
            teilnehmer.setSvNummer(svn);
            return true;
        }
        teilnehmer.addError("svNummer", "Ungültige SV-Nummer angegeben", validationUserHolder.getUsername());
        return false;
    }
}
