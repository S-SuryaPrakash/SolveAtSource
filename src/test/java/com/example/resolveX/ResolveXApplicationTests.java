package com.example.resolveX;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.example.resolveX.exception.IncidentNotFoundException;
import com.example.resolveX.service.IncidentService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResolveXApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void updateWithInvalidIdReturnsNotFoundWithInvalidIdMessage() {
		IncidentService service = new IncidentService();

		IncidentNotFoundException exception = assertThrows(IncidentNotFoundException.class,
				() -> service.updateIncident(999L, null));

		assertEquals("Incident not found with ID: 999", exception.getMessage());
	}

	@Test
	void deleteWithInvalidIdReturnsNotFoundWithInvalidIdMessage() {
		IncidentService service = new IncidentService();

		IncidentNotFoundException exception = assertThrows(IncidentNotFoundException.class,
				() -> service.deleteIncident(999L));

		assertEquals("Incident not found with ID: 999", exception.getMessage());
	}

	@Test
	void getIncidentWithMissingIdReturnsNotFound() throws Exception {
		mockMvc.perform(get("/incident/9999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Incident not found with ID: 9999"));
	}

	@Test
	void createIncidentAcceptsValidTitleAndDescription() throws Exception {
		mockMvc.perform(post("/incident")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"%s","description":"Network outage","status":"OPEN","priority":"HIGH","category":"NETWORK"}
						""".formatted("T".repeat(120))))
				.andExpect(status().isOk());
	}

	@Test
	void createIncidentRejectsEmptyTitle() throws Exception {
		mockMvc.perform(post("/incident")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":" ","description":"Network outage","status":"OPEN","priority":"HIGH","category":"NETWORK"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Request validation failed"))
				.andExpect(jsonPath("$.errors[0]").value("title: Title is required"));
	}

	@Test
	void createIncidentRejectsMissingRequiredField() throws Exception {
		mockMvc.perform(post("/incident")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Network outage","description":"Network outage","status":"OPEN","priority":"HIGH"}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createIncidentRejectsTitleLongerThan120Characters() throws Exception {
		String title = "T".repeat(121);
		mockMvc.perform(post("/incident")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"%s","description":"Network outage","status":"OPEN","priority":"HIGH","category":"NETWORK"}
						""".formatted(title)))
				.andExpect(status().isBadRequest());
	}

}
