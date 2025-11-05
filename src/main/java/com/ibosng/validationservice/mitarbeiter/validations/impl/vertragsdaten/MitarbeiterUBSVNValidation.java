package com.ibosng.validationservice.mitarbeiter.validations.impl.vertragsdaten;

import com.ibosng.dbservice.dtos.mitarbeiter.UnterhaltsberechtigteDto;
import com.ibosng.dbservice.entities.mitarbeiter.Unterhaltsberechtigte;
import com.ibosng.validationservice.Validation;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.validations.SVNValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class MitarbeiterUBSVNValidation implements Validation<UnterhaltsberechtigteDto, Unterhaltsberechtigte> {

    private final ValidationUserHolder validationUserHolder;

    @Override
    public boolean executeValidation(UnterhaltsberechtigteDto unterhaltsberechtigteDto, Unterhaltsberechtigte unterhaltsberechtigte) {
        if(!isNullOrBlank(unterhaltsberechtigteDto.getUSvnr())) {
            String svn = SVNValidation.validateSVN(unterhaltsberechtigteDto.getUSvnr(), unterhaltsberechtigteDto.getUGeburtsdatum() != null ? LocalDate.parse(unterhaltsberechtigteDto.getUGeburtsdatum()) : null);
            if (svn != null) {
                unterhaltsberechtigte.setSvn(svn);
                return true;
            } else {
                unterhaltsberechtigte.addError("usvnr", "Ungueltiges SVNR", validationUserHolder.getUsername());
                return false;
            }
        }
        unterhaltsberechtigte.addError("usvnr", "Das Feld ist leer", validationUserHolder.getUsername());
        return false;
    }
}
