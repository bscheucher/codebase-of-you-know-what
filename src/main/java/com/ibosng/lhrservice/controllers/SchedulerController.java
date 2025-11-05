package com.ibosng.lhrservice.controllers;

import com.ibosng.lhrservice.services.SchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/sync")
@Tag(name = "Scheduler controller")
@RequiredArgsConstructor
@Profile({"development", "QA", "preProd"})
public class SchedulerController {
    private final SchedulerService schedulerService;

    @GetMapping("/abwesenheiten")
    public void syncAbwesenheiten() {
        schedulerService.syncMAAbwesenheitenData();
    }

    @Operation(
            summary = "Download zeitnachweis document",
            description = "Download zeitnachweis document by personalnummer and date",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Not Found"),
                    @ApiResponse(responseCode = "422", description = "Unprocessable entity"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            })
    @GetMapping("/documents")
    public ResponseEntity<Void> downloadLHRDocuments() {
        schedulerService.syncLhrDocuments();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/close-monaten")
    public void closeMonaten() {
        schedulerService.closeMonaten();
    }

    @GetMapping("/sync-leistungserfassung")
    public void syncLeistungserfassung() {
        schedulerService.resyncLeistungserfassungData();
    }
}
