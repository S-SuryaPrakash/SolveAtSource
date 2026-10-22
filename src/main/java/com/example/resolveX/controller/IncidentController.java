package com.example.resolveX.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.resolveX.dto.IncidentRequest;
import com.example.resolveX.dto.IncidentResponse;
import com.example.resolveX.dto.PaginatedIncidentResponse;
import com.example.resolveX.service.IncidentService;

@RestController
@RequestMapping({"/incident", "/incidents"})
@Tag(name = "Incidents", description = "Create, query, update, and delete incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    @Operation(summary = "List incidents", description = "Filters incidents, then sorts, paginates, and optionally groups the current page.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidents returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, pagination, sort, or group parameter", content = @Content)
    })
    public PaginatedIncidentResponse getAllIncidents(
            @Parameter(description = "Zero-based page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of incidents per page; maximum 50", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Exact status filter: New, Work In Progress, Solution Proposed, or Closed", example = "Work In Progress") @RequestParam(required = false) String status,
            @Parameter(description = "Exact priority filter: P4 - Low, P3 - Medium, P2 - High, or P1 - Critical", example = "P2 - High") @RequestParam(required = false) String priority,
            @Parameter(description = "Exact category filter", example = "NETWORK") @RequestParam(required = false) String category,
            @Parameter(description = "Case-insensitive partial title filter", example = "outage") @RequestParam(required = false) String title,
            @Parameter(description = "Field used for sorting: id, title, description, status, priority, category, createdAt, or updatedAt", example = "createdAt") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort order: asc or desc", example = "desc") @RequestParam(defaultValue = "asc") String sortDirection,
            @Parameter(description = "Optional field used to group the current page; supports the same fields as sortBy", example = "priority") @RequestParam(required = false) String groupBy) {
        return incidentService.getAllIncidents(
                page, size, status, priority, category, title, sortBy, sortDirection, groupBy);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an incident by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident returned"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content)
    })
    public IncidentResponse getIncidentById(@PathVariable Long id) {
        return incidentService.getIncidentById(id);
    }

    @PostMapping
    @Operation(summary = "Create an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident created", content = @Content(schema = @Schema(implementation = IncidentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content)
    })
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody IncidentRequest request) {
        IncidentResponse response = incidentService.createIncident(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content)
    })
    public IncidentResponse updateIncident(@PathVariable Long id, @Valid @RequestBody IncidentRequest request) {
        return incidentService.updateIncident(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an incident")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Incident deleted"),
            @ApiResponse(responseCode = "404", description = "Incident not found", content = @Content)
    })
    public ResponseEntity<Void> deleteIncident(@PathVariable Long id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.noContent().build();
    }
}
