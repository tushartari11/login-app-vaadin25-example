package com.zetcom.mpria.config.api;

import com.zetcom.mpria.config.application.PublishedConfigurationService;
import com.zetcom.mpria.config.application.PublishedScreen;
import com.zetcom.mpria.config.application.RecordTypeNotFoundException;
import com.zetcom.mpria.config.application.ScreenNotFoundException;
import com.zetcom.mpria.config.domain.ComponentRegistry;
import com.zetcom.mpria.config.infrastructure.security.SecurityConfig;
import com.zetcom.mpria.config.testing.ConfigFixtures;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The read API, which every other service in the platform calls.
 *
 * <p>{@code catalog-service} is already built against the schema endpoint, so the shape below is
 * pinned as an exact string rather than field by field: a renamed key, a reordered payload or a
 * suppressed null would all pass a laxer assertion and break a shipped service.
 */
@WebMvcTest(PublishedConfigurationController.class)
@Import(SecurityConfig.class)
class PublishedConfigurationControllerTest {

    private static final String SCHEMA_JSON = """
            {"recordType":"object","schemaVersion":1,"titleFieldId":"title","fields":[\
            {"id":"inventoryNumber","type":"STRING","label":"config.field.inventoryNumber",\
            "required":true,"maxLength":50,"allowedValues":[]},\
            {"id":"title","type":"STRING","label":"config.field.title",\
            "required":true,"maxLength":255,"allowedValues":[]},\
            {"id":"description","type":"TEXT","label":"config.field.description",\
            "required":false,"maxLength":null,"allowedValues":[]},\
            {"id":"acquiredOn","type":"DATE","label":"config.field.acquiredOn",\
            "required":false,"maxLength":null,"allowedValues":[]},\
            {"id":"condition","type":"ENUM","label":"config.field.condition",\
            "required":false,"maxLength":null,"allowedValues":["GOOD","FAIR","POOR"]}]}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PublishedConfigurationService configuration;

    /** The resource-server filter chain needs a decoder even when no token is ever decoded. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void serves_the_schema_in_exactly_the_shape_catalog_service_parses() throws Exception {
        given(configuration.recordType("object")).willReturn(ConfigFixtures.objectRecordType());

        mvc.perform(get("/api/v1/record-types/{recordType}/schema", "object")
                        .header("X-Tenant-Id", "11111111-1111-1111-1111-111111111111")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string(SCHEMA_JSON));
    }

    @Test
    void answers_404_as_a_problem_document_when_the_record_type_is_not_configured() throws Exception {
        willThrow(new RecordTypeNotFoundException("acquisition"))
                .given(configuration).recordType("acquisition");

        mvc.perform(get("/api/v1/record-types/{recordType}/schema", "acquisition").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type")
                        .value("https://docs.mpria.zetcom.com/problems/record-type-not-found"))
                .andExpect(jsonPath("$.detail").value(containsString("acquisition")));
    }

    @Test
    void serves_a_screen_layout_together_with_the_revision_that_makes_it_cacheable() throws Exception {
        given(configuration.screen("object-detail"))
                .willReturn(new PublishedScreen("rev-1", ConfigFixtures.objectDetailScreen()));

        mvc.perform(get("/api/v1/screens/{screenId}", "object-detail").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screenId").value("object-detail"))
                .andExpect(jsonPath("$.recordType").value("object"))
                .andExpect(jsonPath("$.revisionId").value("rev-1"))
                // The real pm-contract type, not a parallel shape: clients dispatch on `kind`.
                .andExpect(jsonPath("$.layout.regions[0].kind").value("REGION"))
                .andExpect(jsonPath("$.layout.regions[0].children[0].kind").value("GROUP"))
                .andExpect(jsonPath("$.layout.regions[0].children[0].span.xl").value(8))
                .andExpect(jsonPath("$.layout.regions[0].children[0].children[0].component.fieldId")
                        .value("inventoryNumber"));
    }

    @Test
    void answers_404_for_a_screen_the_published_revision_does_not_configure() throws Exception {
        willThrow(new ScreenNotFoundException("object-list")).given(configuration).screen("object-list");

        mvc.perform(get("/api/v1/screens/{screenId}", "object-list").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type")
                        .value("https://docs.mpria.zetcom.com/problems/screen-not-found"));
    }

    @Test
    void serves_the_current_revision_pointer() throws Exception {
        given(configuration.currentRevision()).willReturn(ConfigFixtures.publishedRevision());

        mvc.perform(get("/api/v1/revisions/current").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("""
                        {"revisionId":"rev-1","publishedAt":"2026-08-01T10:15:30Z","status":"PUBLISHED"}"""));
    }

    @Test
    void serves_the_component_registry_that_drives_the_palette() throws Exception {
        given(configuration.components()).willReturn(ComponentRegistry.standard().entries());

        mvc.perform(get("/api/v1/components").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TEXT_FIELD"))
                .andExpect(jsonPath("$[0].label").value("Text field"))
                .andExpect(jsonPath("$[0].acceptsFieldTypes[0]").value("STRING"))
                .andExpect(jsonPath("$[0].options.multiline.type").value("boolean"))
                // Serialised as `default`, which is a Java keyword and so cannot be the field name.
                .andExpect(jsonPath("$[0].options.multiline.default").value(false))
                .andExpect(jsonPath("$[6].type").value("MEDIA_GALLERY"))
                .andExpect(jsonPath("$[6].acceptsFieldTypes").isEmpty());
    }

    @Test
    void refuses_an_unauthenticated_caller() throws Exception {
        mvc.perform(get("/api/v1/components")).andExpect(status().isUnauthorized());
    }

    @Test
    void serves_a_read_endpoint_to_any_authenticated_principal_holding_no_configuration_role() throws Exception {
        // Every operator client resolves layouts. Reading configuration is not an admin privilege.
        given(configuration.components()).willReturn(ComponentRegistry.standard().entries());

        mvc.perform(get("/api/v1/components").with(jwt())).andExpect(status().isOk());
    }
}
