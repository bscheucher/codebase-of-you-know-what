package com.ibosng.fileimportservice.services.impl;

import com.ibosng.fileimportservice.services.PostService;
import com.ibosng.validationservice.services.ValidatorService;
import com.ibosng.workflowservice.dtos.WorkflowPayload;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Service
@Slf4j
public class PostServiceImpl implements PostService {
    @Getter
    @Value("${validateImportedTeilnehmerEndpoint:#{null}}")
    private String validateImportedTeilnehmerEndpoint;

    private final ValidatorService validatorService;

    public PostServiceImpl(ValidatorService validatorService) {
        this.validatorService = validatorService;
    }

    @Override
    public void postToValidationService(WorkflowPayload workflowPayload) {
        try {
            log.info("Sending to validation service payload: {}", workflowPayload.toString());
            log.info("Endpoint for validation service: {}", getValidateImportedTeilnehmerEndpoint());

            ResponseEntity<String> response = validatorService.validateImportedParticipants(workflowPayload);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully sent request to the validation service for import filename: {}", workflowPayload.getData());
            } else {
                log.error("Validation service responded with: {} for import filename: {}", response.getStatusCode(), workflowPayload.getData());
            }
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("HTTP error when sending request to validation service: {}", ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            log.error("Resource access error when sending request to validation service", ex);
        } catch (Exception ex) {
            log.error("Unexpected error when sending request to validation service", ex);
        }
    }

}