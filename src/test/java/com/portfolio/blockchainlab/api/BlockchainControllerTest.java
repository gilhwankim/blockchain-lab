package com.portfolio.blockchainlab.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BlockchainControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesChainAndAllowsMiningBlock() throws Exception {
        mockMvc.perform(get("/api/chain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.height").value(0))
                .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(post("/api/chain/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"first block\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.header.index").value(1));
    }
}
