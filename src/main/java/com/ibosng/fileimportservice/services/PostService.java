package com.ibosng.fileimportservice.services;

import com.ibosng.workflowservice.dtos.WorkflowPayload;

public interface PostService {
    void postToValidationService(WorkflowPayload workflowPayload);
}
