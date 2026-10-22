package com.example.resolveX.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.resolveX.dto.IncidentGroupResponse;
import com.example.resolveX.dto.IncidentRequest;
import com.example.resolveX.dto.IncidentResponse;
import com.example.resolveX.dto.PaginatedIncidentResponse;
import com.example.resolveX.entity.Incident;
import com.example.resolveX.entity.IncidentPriority;
import com.example.resolveX.entity.IncidentStatus;
import com.example.resolveX.exception.IncidentNotFoundException;
import com.example.resolveX.repository.IncidentRepository;

@Service
public class IncidentService {

    private static final int MAX_PAGE_SIZE = 50;

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public PaginatedIncidentResponse getAllIncidents(int page, int size, String status, String priority,
            String category, String title, String sortBy, String sortDirection, String groupBy) {
        validatePagination(page, size);
        String normalizedSortBy = normalizeField(sortBy, "sortBy");
        String normalizedGroupBy = groupBy == null || groupBy.isBlank() ? null : normalizeField(groupBy, "groupBy");
        Sort.Direction direction = parseSortDirection(sortDirection);
        IncidentStatus statusFilter = status == null || status.isBlank() ? null : IncidentStatus.fromValue(status);
        IncidentPriority priorityFilter = priority == null || priority.isBlank() ? null : IncidentPriority.fromValue(priority);

        Specification<Incident> filters = hasEnumValue("status", statusFilter)
                .and(hasEnumValue("priority", priorityFilter))
                .and(hasExactValue("category", category))
                .and(titleContains(title));
        Sort sort = Sort.by(direction, normalizedSortBy).and(Sort.by(Sort.Direction.ASC, "id"));
        Page<Incident> incidentPage = incidentRepository.findAll(filters, PageRequest.of(page, size, sort));
        List<IncidentResponse> pageItems = incidentPage.getContent().stream().map(this::toResponse).toList();

        return new PaginatedIncidentResponse(pageItems, page, size, incidentPage.getTotalElements(),
                incidentPage.hasNext(), normalizedGroupBy == null ? null : groupIncidents(pageItems, normalizedGroupBy));
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        return toResponse(findIncident(id));
    }

    public IncidentResponse createIncident(IncidentRequest request) {
        Incident incident = new Incident(
                request == null || request.title() == null ? "" : request.title(),
                request == null || request.description() == null ? "" : request.description(),
                request == null || request.status() == null ? IncidentStatus.NEW : request.status(),
                request == null || request.priority() == null ? IncidentPriority.P3_MEDIUM : request.priority(),
                request == null || request.category() == null || request.category().isBlank() ? "GENERAL" : request.category());
        return toResponse(incidentRepository.save(incident));
    }

    @Transactional
    public IncidentResponse updateIncident(Long id, IncidentRequest request) {
        Incident existing = findIncident(id);
        existing.setTitle(request == null || request.title() == null ? existing.getTitle() : request.title());
        existing.setDescription(request == null || request.description() == null ? existing.getDescription() : request.description());
        existing.setStatus(request == null || request.status() == null ? existing.getStatus() : request.status());
        existing.setPriority(request == null || request.priority() == null ? existing.getPriority() : request.priority());
        existing.setCategory(request == null || request.category() == null || request.category().isBlank() ? existing.getCategory() : request.category());
        return toResponse(incidentRepository.save(existing));
    }

    @Transactional
    public void deleteIncident(Long id) {
        incidentRepository.delete(findIncident(id));
    }

    private Incident findIncident(Long id) {
        return incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
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

    private Specification<Incident> hasExactValue(String field, String value) {
        return (root, query, criteriaBuilder) -> value == null || value.isBlank()
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(criteriaBuilder.lower(root.get(field)), value.trim().toLowerCase(Locale.ROOT));
    }

    private Specification<Incident> hasEnumValue(String field, Enum<?> value) {
        return (root, query, criteriaBuilder) -> value == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get(field), value);
    }

    private Specification<Incident> titleContains(String title) {
        return (root, query, criteriaBuilder) -> title == null || title.isBlank()
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.like(criteriaBuilder.lower(root.get("title")),
                        "%" + title.trim().toLowerCase(Locale.ROOT) + "%");
    }

    private Sort.Direction parseSortDirection(String sortDirection) {
        return Sort.Direction.fromOptionalString(sortDirection)
                .orElseThrow(() -> new IllegalArgumentException("sortDirection must be either asc or desc"));
    }

    private List<IncidentGroupResponse> groupIncidents(List<IncidentResponse> incidentsToGroup, String groupBy) {
        Map<String, List<IncidentResponse>> groups = new LinkedHashMap<>();
        for (IncidentResponse incident : incidentsToGroup) {
            String groupValue = switch (groupBy) {
                case "id" -> String.valueOf(incident.id());
                case "title" -> incident.title();
                case "description" -> incident.description();
                case "status" -> incident.status().getDisplayValue();
                case "priority" -> incident.priority().getDisplayValue();
                case "category" -> incident.category();
                case "createdAt" -> String.valueOf(incident.createdAt());
                case "updatedAt" -> String.valueOf(incident.updatedAt());
                default -> throw unsupportedField("groupBy");
            };
            groups.computeIfAbsent(groupValue == null ? "" : groupValue, ignored -> new java.util.ArrayList<>()).add(incident);
        }
        return groups.entrySet().stream()
                .map(entry -> new IncidentGroupResponse(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    private String normalizeField(String field, String parameterName) {
        String normalizedField = field == null || field.isBlank() ? "id" : field.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedField) {
            case "id", "title", "description", "status", "priority", "category" -> normalizedField;
            case "createdat" -> "createdAt";
            case "updatedat" -> "updatedAt";
            default -> throw unsupportedField(parameterName);
        };
    }

    private IllegalArgumentException unsupportedField(String parameterName) {
        return new IllegalArgumentException(parameterName
                + " must be one of: id, title, description, status, priority, category, createdAt, updatedAt");
    }

    private IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus(),
                incident.getPriority(),
                incident.getCategory(),
                incident.getCreatedAt(),
                incident.getUpdatedAt());
    }
}
