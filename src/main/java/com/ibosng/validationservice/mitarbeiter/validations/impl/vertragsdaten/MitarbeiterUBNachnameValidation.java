package com.ibosng.validationservice.mitarbeiter.validations.impl.vertragsdaten;

import com.ibosng.dbservice.dtos.mitarbeiter.UnterhaltsberechtigteDto;
import com.ibosng.dbservice.entities.mitarbeiter.Unterhaltsberechtigte;
import com.ibosng.validationservice.Validation;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.validations.NachnameValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class MitarbeiterUBNachnameValidation implements Validation<UnterhaltsberechtigteDto, Unterhaltsberechtigte> {

    private final ValidationUserHolder validationUserHolder;

    @Override
    public boolean executeValidation(UnterhaltsberechtigteDto unterhaltsberechtigteDto, Unterhaltsberechtigte unterhaltsberechtigte) {
        if (!isNullOrBlank(unterhaltsberechtigteDto.getUNachname())) {
            boolean result = NachnameValidation.isNachnameValid(unterhaltsberechtigteDto.getUNachname());
            if (result) {
                unterhaltsberechtigte.setNachname(unterhaltsberechtigteDto.getUNachname());
                return true;
            } else {
                unterhaltsberechtigte.addError("unachname", "Das Feld ist ungültig", validationUserHolder.getUsername());
                return false;
            }
        }
        unterhaltsberechtigte.addError("unachname", "Das Feld ist leer", validationUserHolder.getUsername());
        return false;
    }
}
