package com.ibosng.validationservice.teilnehmer.validations.manual;

import com.ibosng.dbservice.dtos.teilnehmer.TeilnehmerDto;
import com.ibosng.dbservice.entities.masterdata.Muttersprache;
import com.ibosng.dbservice.entities.teilnehmer.Teilnehmer;
import com.ibosng.dbservice.services.masterdata.MutterspracheService;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.teilnehmer.validations.AbstractValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;

@Component
@RequiredArgsConstructor
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TeilnehmerDtoMutterspracheValidation extends AbstractValidation<TeilnehmerDto, Teilnehmer> {

    private final MutterspracheService mutterspracheService;
    private final ValidationUserHolder validationUserHolder;

    @Override
    public boolean executeValidation(TeilnehmerDto teilnehmerDto, Teilnehmer teilnehmer) {
        if (!isNullOrBlank(teilnehmerDto.getMuttersprache())) {
            Muttersprache muttersprache = mutterspracheService.findByName(teilnehmerDto.getMuttersprache());
            if (muttersprache != null) {
                teilnehmer.setMuttersprache(muttersprache);
                return true;
            }
            teilnehmer.addError("muttersprache", "Ungültige Muttersprache angegeben", validationUserHolder.getUsername());
            return false;
        }
        // If a value already exists keep it and return it to the FE
        if(teilnehmer.getMuttersprache() != null){
            teilnehmerDto.setMuttersprache(teilnehmer.getMuttersprache().getName());
        }
        return true;
    }
}
