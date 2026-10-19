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
	void getIncidentsSupportsStatusAndPriorityFilters() throws Exception {
		mockMvc.perform(post("/incident")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Filter by OPEN","description":"Network outage","status":"OPEN","priority":"HIGH","category":"NETWORK"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/incident")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Filter by CLOSED","description":"Power issue","status":"CLOSED","priority":"LOW","category":"POWER"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/incidents")
				.param("status", "OPEN")
				.param("priority", "HIGH"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].status").value("OPEN"))
				.andExpect(jsonPath("$.items[0].priority").value("HIGH"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10));
	}

	@Test
	void getIncidentsSupportsCategoryAndTitleFilters() throws Exception {
		mockMvc.perform(post("/incident")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Network outage in east wing","description":"Server down","status":"OPEN","priority":"HIGH","category":"NETWORK"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/incident")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Power failure backup generator","description":"Power issue","status":"CLOSED","priority":"LOW","category":"POWER"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/incidents")
				.param("category", "NETWORK")
				.param("title", "wing"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].category").value("NETWORK"))
				.andExpect(jsonPath("$.items[0].title").value("Network outage in east wing"));
	}

	@Test
	void getIncidentsAppliesManualPagination() throws Exception {
		for (int i = 1; i <= 5; i++) {
			mockMvc.perform(post("/incident")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"title":"Pagination Incident %d","description":"Test %d","status":"PAGINATION_TEST","priority":"HIGH","category":"PAGINATION_TEST"}
							""".formatted(i, i)))
					.andExpect(status().isOk());
		}

		mockMvc.perform(get("/incidents")
				.param("page", "1")
				.param("size", "2")
				.param("status", "PAGINATION_TEST")
				.param("category", "PAGINATION_TEST"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].title").value("Pagination Incident 3"))
				.andExpect(jsonPath("$.items[1].title").value("Pagination Incident 4"))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalItems").value(5))
				.andExpect(jsonPath("$.hasNext").value(true));
	}

	@Test
	void getIncidentsSortsByAnySupportedField() throws Exception {
		mockMvc.perform(post("/incident").contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Zulu sort\",\"description\":\"z\",\"status\":\"OPEN\",\"priority\":\"LOW\",\"category\":\"SORT_TEST\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/incident").contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Alpha sort\",\"description\":\"a\",\"status\":\"CLOSED\",\"priority\":\"HIGH\",\"category\":\"SORT_TEST\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/incidents").param("category", "SORT_TEST")
				.param("sortBy", "title").param("sortDirection", "desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].title").value("Zulu sort"))
				.andExpect(jsonPath("$.items[1].title").value("Alpha sort"));
	}

	@Test
	void getIncidentsGroupsTheCurrentPageByPriority() throws Exception {
		mockMvc.perform(post("/incident").contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"High group\",\"description\":\"test\",\"status\":\"OPEN\",\"priority\":\"HIGH\",\"category\":\"GROUP_TEST\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/incident").contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Low group\",\"description\":\"test\",\"status\":\"OPEN\",\"priority\":\"LOW\",\"category\":\"GROUP_TEST\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/incidents").param("category", "GROUP_TEST")
				.param("sortBy", "priority").param("groupBy", "priority"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.groups[0].value").value("HIGH"))
				.andExpect(jsonPath("$.groups[0].items[0].priority").value("HIGH"))
				.andExpect(jsonPath("$.groups[1].value").value("LOW"));
	}

	@Test
	void getIncidentsRejectsUnsupportedSortAndGroupFields() throws Exception {
		mockMvc.perform(get("/incidents").param("sortBy", "reporter"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("sortBy must be one of: id, title, description, status, priority, category, createdAt, updatedAt"));
		mockMvc.perform(get("/incidents").param("groupBy", "owner"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("groupBy must be one of: id, title, description, status, priority, category, createdAt, updatedAt"));
	}

	@Test
	void getIncidentsRejectsNegativePageNumber() throws Exception {
		mockMvc.perform(get("/incidents")
				.param("page", "-1")
				.param("size", "10"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Page number must be greater than or equal to 0"));
	}

	@Test
	void getIncidentsRejectsZeroPageSize() throws Exception {
		mockMvc.perform(get("/incidents")
				.param("page", "0")
				.param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Page size must be greater than 0"));
	}

	@Test
	void getIncidentsRejectsUnreasonablyLargePageSize() throws Exception {
		mockMvc.perform(get("/incidents")
				.param("page", "0")
				.param("size", "1000"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Page size must not exceed 50"));
	}

	@Test
	void getIncidentsReturnsEmptyListWhenPageBeyondAvailableResults() throws Exception {
		mockMvc.perform(get("/incidents")
				.param("page", "99")
				.param("size", "10")
				.param("status", "PAGE_NOT_FOUND_STATUS"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items.length()").value(0))
				.andExpect(jsonPath("$.totalItems").value(0))
				.andExpect(jsonPath("$.hasNext").value(false));
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
