package com.ibosng.validationservice.teilnehmer.validations.manual;

import com.ibosng.dbservice.dtos.teilnehmer.TeilnehmerDto;
import com.ibosng.dbservice.entities.Rgs;
import com.ibosng.dbservice.entities.teilnehmer.Teilnehmer2Seminar;
import com.ibosng.dbservice.services.RgsService;
import com.ibosng.validationservice.config.ValidationUserHolder;
import com.ibosng.validationservice.teilnehmer.validations.AbstractValidation;
import com.ibosng.validationservice.utils.Parsers;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

import static com.ibosng.dbservice.utils.Parsers.isNullOrBlank;

@RequiredArgsConstructor
public class TeilnehmerDtoRgsValidation extends AbstractValidation<TeilnehmerDto, Teilnehmer2Seminar> {
    private final RgsService rgsService;
    private final ValidationUserHolder validationUserHolder;

    @Override
    public boolean executeValidation(TeilnehmerDto teilnehmerDto, Teilnehmer2Seminar teilnehmer2Seminar) {
        if (!isNullOrBlank(teilnehmerDto.getRgs())) {
            if (!teilnehmerDto.getRgs().matches("^\\d+$")) {
                teilnehmer2Seminar.getTeilnehmer().addError("rgs", "Ungültige RGS angegeben", validationUserHolder.getUsername());
                return false;
            } else {
                Rgs findByRgs = rgsService.findByRgs(Parsers.parseStringToInteger(teilnehmerDto.getRgs()));
                if (Objects.isNull(findByRgs) || !Objects.equals(findByRgs.getBezeichnung(), teilnehmerDto.getRgsBezeichnung())) {
                    teilnehmer2Seminar.getTeilnehmer().addError("rgs", "Ungültige RGS angegeben", validationUserHolder.getUsername());
                    return false;
                }
                List<Integer> allRgs = rgsService.getAllRgs();
                Integer rgs = Parsers.parseStringToInteger(teilnehmerDto.getRgs());
                if (allRgs.contains(rgs)) {
                    teilnehmer2Seminar.setRgs(findByRgs);
                    return true;
                }
                teilnehmer2Seminar.getTeilnehmer().addError("rgs", "Ungültige RGS angegeben", validationUserHolder.getUsername());
                return false;
            }
        }
        teilnehmer2Seminar.getTeilnehmer().addError("rgs", "Ungültige RGS angegeben", validationUserHolder.getUsername());

        return false;
    }
}
