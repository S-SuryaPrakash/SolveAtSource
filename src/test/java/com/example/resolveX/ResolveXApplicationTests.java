package com.example.resolveX;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.resolveX.dto.IncidentRequest;
import com.example.resolveX.service.IncidentService;

@SpringBootTest
class ResolveXApplicationTests {
	@Autowired
	private Validator validator;

	@Test
	void contextLoads() {
	}

	@Test
	void updateWithInvalidIdReturnsNotFoundWithInvalidIdMessage() {
		IncidentService service = new IncidentService();

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> service.updateIncident(999L, null));

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
		assertEquals("Invalid incident ID: 999", exception.getReason());
	}

	@Test
	void deleteWithInvalidIdReturnsNotFoundWithInvalidIdMessage() {
		IncidentService service = new IncidentService();

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> service.deleteIncident(999L));

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
		assertEquals("Invalid incident ID: 999", exception.getReason());
	}

	@Test
	void incidentRequestRejectsMissingRequiredFieldsAndOversizedDescription() {
		IncidentRequest request = new IncidentRequest(" ", "x".repeat(2001), "", " ", "");

		assertEquals(5, validator.validate(request).size());
	}

}
