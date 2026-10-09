package com.example.resolveX.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.resolveX.dto.IncidentRequest;
import com.example.resolveX.dto.IncidentResponse;

@Service
public class IncidentService {

    private final Map<Long, IncidentResponse> incidents = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    public List<IncidentResponse> getAllIncidents() {
        return new ArrayList<>(incidents.values());
    }

    public IncidentResponse getIncidentById(Long id) {
        IncidentResponse incident = incidents.get(id);
        if (incident == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        }
        return incident;
    }

    public IncidentResponse createIncident(IncidentRequest request) {
        long id = sequence.getAndIncrement();
        IncidentResponse response = new IncidentResponse(
                id,
                request == null || request.description() == null ? "" : request.description(),
                request == null || request.priority() == null || request.priority().isBlank() ? "MEDIUM" : request.priority(),
                request == null || request.category() == null || request.category().isBlank() ? "GENERAL" : request.category()
        );
        incidents.put(id, response);
        return response;
    }

    public IncidentResponse updateIncident(Long id, IncidentRequest request) {
        if (!incidents.containsKey(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid incident ID: " + id);
        }

        IncidentResponse updated = new IncidentResponse(
                id,
                request == null || request.description() == null ? "" : request.description(),
                request == null || request.priority() == null || request.priority().isBlank() ? "MEDIUM" : request.priority(),
                request == null || request.category() == null || request.category().isBlank() ? "GENERAL" : request.category()
        );
        incidents.put(id, updated);
        return updated;
    }

    public void deleteIncident(Long id) {
        if (incidents.remove(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid incident ID: " + id);
        }
    }
}
