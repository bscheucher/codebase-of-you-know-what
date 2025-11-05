package com.ibosng.validationservice.mitarbeiter.validations.impl.vertragsdaten;

import com.ibosng.dbservice.dtos.mitarbeiter.VertragsdatenDto;
import com.ibosng.dbservice.entities.mitarbeiter.Vertragsdaten;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.teilnehmer.validations.AbstractValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;
import static com.ibosng.dbservice.utils.Parsers.isValidDate;
import static com.ibosng.dbservice.utils.Parsers.parseDate;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class MitarbeiterArbeitszeitmodellBisValidation extends AbstractValidation<VertragsdatenDto, Vertragsdaten> {

    private final ValidationUserHolder validationUserHolder;
    
    @Override
    public boolean executeValidation(VertragsdatenDto vertragsdatenDto, Vertragsdaten vertragsdaten) {
        if (vertragsdatenDto.getArbeitszeitmodell() != null && vertragsdatenDto.getArbeitszeitmodell().toLowerCase().startsWith("durchrechnung")) {
            if (!isNullOrBlank(vertragsdatenDto.getArbeitszeitmodellBis()) && isValidDate(vertragsdatenDto.getArbeitszeitmodellBis())) {
                if (parseDate(vertragsdatenDto.getArbeitszeitmodellBis()).isAfter(LocalDate.now()) &&
                        !isNullOrBlank(vertragsdatenDto.getEintritt()) &&
                        isValidDate(vertragsdatenDto.getEintritt()) &&
                        parseDate(vertragsdatenDto.getArbeitszeitmodellBis()).isAfter(parseDate(vertragsdatenDto.getEintritt()))) {
                    return true;
                } else if (parseDate(vertragsdatenDto.getArbeitszeitmodellBis()).isBefore(LocalDate.now())) {
                    vertragsdaten.addError("arbeitszeitmodellBis", "Das Datum muss in der Zukunft liegen.", validationUserHolder.getUsername());
                    return false;
                } else if (parseDate(vertragsdatenDto.getArbeitszeitmodellBis()).isAfter(LocalDate.now()) &&
                        !isNullOrBlank(vertragsdatenDto.getEintritt()) &&
                        isValidDate(vertragsdatenDto.getEintritt()) &&
                        parseDate(vertragsdatenDto.getArbeitszeitmodellBis()).isBefore(parseDate(vertragsdatenDto.getEintritt()))) {
                    vertragsdaten.addError("arbeitszeitmodellBis", "Das Datum muss später als Eintrittsdatum sein.", validationUserHolder.getUsername());
                    return false;
                }
            }
            vertragsdaten.addError("arbeitszeitmodellBis", "Das Feld ist leer", validationUserHolder.getUsername());
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldValidationRun() {
        return getSources().isEmpty();
    }
}