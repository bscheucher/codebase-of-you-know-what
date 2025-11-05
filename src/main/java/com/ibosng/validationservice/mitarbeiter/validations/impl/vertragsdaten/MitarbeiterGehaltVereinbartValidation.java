package com.ibosng.validationservice.mitarbeiter.validations.impl.vertragsdaten;

import com.ibosng.dbservice.dtos.mitarbeiter.VertragsdatenDto;
import com.ibosng.dbservice.entities.mitarbeiter.Vertragsdaten;
import com.ibosng.validationservice.Validation;
import com.ibosng.validationservice.config.ValidationUserHolder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MitarbeiterGehaltVereinbartValidation implements Validation<VertragsdatenDto, Vertragsdaten> {

    private final ValidationUserHolder validationUserHolder;

    @Override
    public boolean executeValidation(VertragsdatenDto vertragsdatenDto, Vertragsdaten vertragsdaten) {
        if (vertragsdatenDto.getGehaltVereinbart() != null) {
            return true;
        }
        vertragsdaten.addError("gehaltVereinbart", "Das Feld ist leer", validationUserHolder.getUsername());
        return false;
    }
}
