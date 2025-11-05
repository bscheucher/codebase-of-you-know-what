package com.ibosng.validationservice.teilnehmer.validations.manual;

import com.ibosng.dbservice.dtos.teilnehmer.TeilnehmerDto;
import com.ibosng.dbservice.entities.teilnehmer.Teilnehmer;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.teilnehmer.validations.AbstractValidation;
import com.ibosng.validationservice.validations.NachnameValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class TeilnehmerDtoNachnameValidation extends AbstractValidation<TeilnehmerDto, Teilnehmer> {

    private final ValidationUserHolder validationUserHolder;

    @Override
    public boolean executeValidation(TeilnehmerDto teilnehmerDto, Teilnehmer teilnehmer) {
        if (!isNullOrBlank(teilnehmerDto.getNachname())) {
            boolean result = NachnameValidation.isNachnameValid(teilnehmerDto.getNachname());
            if (!result) {
                teilnehmer.addError("nachname", "Ungültiger Nachname angegeben", validationUserHolder.getUsername());
                return false;
            } else {
                teilnehmer.setNachname(teilnehmerDto.getNachname());
                return true;
            }
        }
        teilnehmer.addError("nachname", "Ungültiger Nachname angegeben", validationUserHolder.getUsername());
        return false;
    }
}
