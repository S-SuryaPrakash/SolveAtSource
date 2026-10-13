package com.example.resolveX.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.example.resolveX.dto.IncidentRequest;
import com.example.resolveX.dto.IncidentResponse;
import com.example.resolveX.dto.PaginatedIncidentResponse;
import com.example.resolveX.entity.Incident;
import com.example.resolveX.exception.IncidentNotFoundException;

@Service
public class IncidentService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final Map<Long, Incident> incidents = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    public PaginatedIncidentResponse getAllIncidents(int page, int size, String status, String priority,
            String category, String title) {
        validatePagination(page, size);

        List<Incident> filteredIncidents = incidents.values().stream()
                .filter(incident -> matchesFilter(incident.getStatus(), status))
                .filter(incident -> matchesFilter(incident.getPriority(), priority))
                .filter(incident -> matchesFilter(incident.getCategory(), category))
                .filter(incident -> title == null || title.isBlank() ||
                        (incident.getTitle() != null && incident.getTitle().toLowerCase().contains(title.toLowerCase())))
                .toList();

        int totalItems = filteredIncidents.size();
        int fromIndex = page * size;
        if (fromIndex >= totalItems) {
            return new PaginatedIncidentResponse(List.of(), page, size, 0, false);
        }

        int toIndex = Math.min(fromIndex + size, totalItems);
        List<IncidentResponse> pageItems = filteredIncidents.subList(fromIndex, toIndex).stream()
                .map(this::toResponse)
                .toList();

        boolean hasNext = toIndex < totalItems;
        return new PaginatedIncidentResponse(pageItems, page, size, totalItems, hasNext);
    }

    public IncidentResponse getIncidentById(Long id) {
        Incident incident = incidents.get(id);
        if (incident == null) {
            throw new IncidentNotFoundException(id);
        }
        return toResponse(incident);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be greater than or equal to 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must not exceed 50");
        }
    }

    public IncidentResponse createIncident(IncidentRequest request) {
        long id = sequence.getAndIncrement();
        Incident incident = new Incident(
                request == null || request.title() == null ? "" : request.title(),
                request == null || request.description() == null ? "" : request.description(),
                request == null || request.status() == null || request.status().isBlank() ? "OPEN" : request.status(),
                request == null || request.priority() == null || request.priority().isBlank() ? "MEDIUM" : request.priority(),
                request == null || request.category() == null || request.category().isBlank() ? "GENERAL" : request.category()
        );
        incident.setId(id);
        incidents.put(id, incident);
        return toResponse(incident);
    }

    public IncidentResponse updateIncident(Long id, IncidentRequest request) {
        Incident existing = incidents.get(id);
        if (existing == null) {
            throw new IncidentNotFoundException(id);
        }

        existing.setTitle(request == null || request.title() == null ? existing.getTitle() : request.title());
        existing.setDescription(request == null || request.description() == null ? existing.getDescription() : request.description());
        existing.setStatus(request == null || request.status() == null || request.status().isBlank() ? existing.getStatus() : request.status());
        existing.setPriority(request == null || request.priority() == null || request.priority().isBlank() ? existing.getPriority() : request.priority());
        existing.setCategory(request == null || request.category() == null || request.category().isBlank() ? existing.getCategory() : request.category());

        incidents.put(id, existing);
        return toResponse(existing);
    }

    public void deleteIncident(Long id) {
        if (incidents.remove(id) == null) {
            throw new IncidentNotFoundException(id);
        }
    }

    private boolean matchesFilter(String actualValue, String filterValue) {
        if (filterValue == null || filterValue.isBlank()) {
            return true;
        }
        return actualValue != null && actualValue.equalsIgnoreCase(filterValue.trim());
    }

    private IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus(),
                incident.getPriority(),
                incident.getCategory()
        );
    }
}
