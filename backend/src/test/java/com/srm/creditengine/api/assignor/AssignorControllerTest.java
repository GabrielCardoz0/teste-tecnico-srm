package com.srm.creditengine.api.assignor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.srm.creditengine.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AssignorControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void listsOnlyActiveSeedAssignorsOrderedByLegalName() throws Exception {
		mockMvc.perform(get("/assignors"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].legalName").value("Alfa Comércio de Tecidos Ltda"))
				.andExpect(jsonPath("$[0].cnpj").value("11222333000181"))
				.andExpect(jsonPath("$[1].legalName").value("Beta Distribuidora de Alimentos S.A."))
				.andExpect(jsonPath("$[1].cnpj").value("12ABC34501DE35"))
				.andExpect(jsonPath("$[2].legalName").value("Delta Serviços Logísticos Ltda"))
				.andExpect(jsonPath("$[2].cnpj").value("A1B2C3D4000193"))
				.andExpect(jsonPath("$[?(@.cnpj == 'B7C8D9E0000120')]").isEmpty());
	}

	@Test
	void assignorsCannotBeManagedThroughTheApi() throws Exception {
		mockMvc.perform(post("/assignors").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(put("/assignors").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/assignors"))
				.andExpect(status().isMethodNotAllowed());
	}

}
