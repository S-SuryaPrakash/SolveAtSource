package com.example.resolveX.controller;

import java.util.List;

import jakarta.validation.Valid;
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
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public PaginatedIncidentResponse getAllIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String title) {
        return incidentService.getAllIncidents(page, size, status, priority, category, title);
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncidentById(@PathVariable Long id) {
        return incidentService.getIncidentById(id);
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody IncidentRequest request) {
        IncidentResponse response = incidentService.createIncident(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public IncidentResponse updateIncident(@PathVariable Long id, @Valid @RequestBody IncidentRequest request) {
        return incidentService.updateIncident(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable Long id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.noContent().build();
    }
}
