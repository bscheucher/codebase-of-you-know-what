package com.ibosng.gatewayservice.services.impl;

import com.ibosng.dbservice.entities.Benutzer;
import com.ibosng.dbservice.entities.masterdata.Personalnummer;
import com.ibosng.dbservice.services.BenutzerService;
import com.ibosng.gatewayservice.config.GatewayUserHolder;
import com.ibosng.gatewayservice.exceptions.BusinessLogicException;
import com.ibosng.gatewayservice.services.EnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnvironmentServiceImpl implements EnvironmentService {

    private final GatewayUserHolder gatewayUserHolder;
    private final BenutzerService benutzerService;

    @Value("${isProduction:#{null}}")
    private String isProduction;

    @Override
    public boolean isProduction() {
        return Boolean.parseBoolean(isProduction);
    }

    @Override
    public Personalnummer getPersonalnummer() {
        //benutzer check
        if (gatewayUserHolder.getUserId() == null) {
            throw new BusinessLogicException("User not exists");
        }
        Benutzer benutzer = benutzerService.findById(gatewayUserHolder.getUserId()).orElse(null);
        if (benutzer == null) {
            throw new BusinessLogicException("User not exists");
        }
        Personalnummer personalnummer = benutzer.getPersonalnummer();
        if (personalnummer == null) {
            throw new BusinessLogicException("Personalnummer not found");
        }
        return personalnummer;
    }
}
