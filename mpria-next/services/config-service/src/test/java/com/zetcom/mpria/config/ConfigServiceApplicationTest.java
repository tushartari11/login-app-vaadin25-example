package com.zetcom.mpria.config;

import com.zetcom.mpria.config.infrastructure.memory.InMemoryConfigurationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The service as it actually starts: real wiring, real seed, real filter chain.
 *
 * <p>The slice tests mock the use cases, so none of them would notice a seed that failed to load
 * or a bean that was never wired. This one would — and since every other service in the walking
 * skeleton reads its configuration from here, that failure has to surface in this build rather
 * than in somebody else's.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConfigServiceApplicationTest {

    @Autowired
    private MockMvc mvc;

    /** Nothing decodes a real token here; the resource-server chain just needs a decoder bean. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void serves_the_seeded_record_type_schema() throws Exception {
        mvc.perform(get("/api/v1/record-types/{recordType}/schema", "object").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleFieldId").value("title"))
                .andExpect(jsonPath("$.fields.length()").value(5))
                .andExpect(jsonPath("$.fields[4].allowedValues[0]").value("GOOD"));
    }

    @Test
    void serves_the_seeded_screen_layout_as_the_real_pm_contract_type() throws Exception {
        mvc.perform(get("/api/v1/screens/{screenId}", "object-detail").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionId").value("rev-1"))
                .andExpect(jsonPath("$.layout.regions[0].children[1].id").value("grp-acquisition"));
    }

    @Test
    void answers_404_for_a_record_type_nobody_has_configured() throws Exception {
        mvc.perform(get("/api/v1/record-types/{recordType}/schema", "acquisition").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void opens_a_draft_the_admin_console_can_edit_on_a_cold_start() throws Exception {
        mvc.perform(get("/api/v1/admin/drafts/{draftId}", InMemoryConfigurationRepository.SEED_DRAFT_ID)
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.screens[0].screenId").value("object-detail"));
    }

    /**
     * Publishing moves the current-revision pointer, and the application context is shared with
     * every other test in this class — so this one hands back a clean one rather than leaving the
     * suite's outcome dependent on execution order.
     */
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void publishes_the_seeded_draft_through_the_real_gate() throws Exception {
        mvc.perform(post("/api/v1/admin/drafts/{draftId}/publish",
                        InMemoryConfigurationRepository.SEED_DRAFT_ID).with(publisher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    private static JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_mpria-config-admin"));
    }

    private static JwtRequestPostProcessor publisher() {
        return jwt().authorities(
                new SimpleGrantedAuthority("ROLE_mpria-config-admin"),
                new SimpleGrantedAuthority("ROLE_mpria-config-publisher"));
    }
}
