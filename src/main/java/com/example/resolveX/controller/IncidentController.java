package com.example.resolveX.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.resolveX.dto.IncidentRequest;
import com.example.resolveX.dto.IncidentResponse;
import com.example.resolveX.service.IncidentService;

@RestController
@RequestMapping("/incident")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public List<IncidentResponse> getAllIncidents() {
        return incidentService.getAllIncidents();
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncidentById(@PathVariable Long id) {
        return incidentService.getIncidentById(id);
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@RequestBody IncidentRequest request) {
        IncidentResponse response = incidentService.createIncident(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public IncidentResponse updateIncident(@PathVariable Long id, @RequestBody IncidentRequest request) {
        return incidentService.updateIncident(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable Long id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.noContent().build();
    }
}
