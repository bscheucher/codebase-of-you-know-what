package com.ibosng.lhrservice.services.impl;

import com.ibosng.lhrservice.services.LHR2ValidationService;
import com.ibosng.lhrservice.services.LHRRestService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LHR2ValidationServiceImpl implements LHR2ValidationService {
    @Getter
    @Value("${validateSyncMitarbeiterWithUPN:#{null}}")
    private String validateSyncMitarbeiterEndpointWithUPN;


    private final LHRRestService restService;

    @Override
    public ResponseEntity<String> validateSyncMitarbeiterWithUPN(String upn) {
        log.info("Calling validation service to validate and sync mitarbeiter with upn: {}", upn);
        log.info("Validation service endpoint: {}", getValidateSyncMitarbeiterEndpointWithUPN());
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("upn", upn);
        return restService.sendRequest(
                HttpMethod.POST,
                getValidateSyncMitarbeiterEndpointWithUPN(),
                null,
                String.class,
                null,
                queryParams);
    }

}
