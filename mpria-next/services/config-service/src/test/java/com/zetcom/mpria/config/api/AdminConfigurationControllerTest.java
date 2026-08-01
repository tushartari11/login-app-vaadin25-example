package com.zetcom.mpria.config.api;

import java.util.List;

import com.zetcom.mpria.config.application.ConfigurationDiff;
import com.zetcom.mpria.config.application.ConfigurationDraftService;
import com.zetcom.mpria.config.application.DraftNotFoundException;
import com.zetcom.mpria.config.application.ImportReport;
import com.zetcom.mpria.config.application.StaleDraftException;
import com.zetcom.mpria.config.domain.ConfigurationError;
import com.zetcom.mpria.config.domain.ConfigurationValidationException;
import com.zetcom.mpria.config.infrastructure.security.SecurityConfig;
import com.zetcom.mpria.config.testing.ConfigFixtures;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The administration API.
 *
 * <p>A configuration bundle can change what the whole application does, so the authorisation rules
 * are part of the contract and are asserted here rather than assumed from the filter chain.
 */
@WebMvcTest(AdminConfigurationController.class)
@Import(SecurityConfig.class)
class AdminConfigurationControllerTest {

    private static final String LAYOUT_JSON = """
            {"screenId":"object-detail","recordType":"object","regions":[
              {"kind":"REGION","id":"main","grid":{"columns":{"base":1},"gap":"md"},"children":[]}]}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConfigurationDraftService drafts;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    class Authorisation {

        @Test
        void refuses_a_principal_without_the_configuration_admin_role() throws Exception {
            mvc.perform(get("/api/v1/admin/drafts/{draftId}", "draft-1").with(curator()))
                    .andExpect(status().isForbidden());

            then(drafts).should(never()).draft("draft-1");
        }

        @Test
        void refuses_an_unauthenticated_caller() throws Exception {
            mvc.perform(get("/api/v1/admin/drafts/{draftId}", "draft-1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void admits_a_configuration_admin() throws Exception {
            given(drafts.draft("draft-1")).willReturn(ConfigFixtures.draftRevision());

            mvc.perform(get("/api/v1/admin/drafts/{draftId}", "draft-1").with(admin()))
                    .andExpect(status().isOk());
        }

        @Test
        void refuses_publication_to_an_admin_who_is_not_also_a_publisher() throws Exception {
            // Editing a draft affects nobody. Publishing changes what every operator sees.
            mvc.perform(post("/api/v1/admin/drafts/{draftId}/publish", "draft-1").with(admin()))
                    .andExpect(status().isForbidden());

            then(drafts).should(never()).publish("draft-1");
        }

        @Test
        void refuses_publication_to_a_publisher_who_is_not_a_configuration_admin() throws Exception {
            mvc.perform(post("/api/v1/admin/drafts/{draftId}/publish", "draft-1").with(publisherOnly()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class Drafts {

        @Test
        void returns_the_draft_with_the_etag_the_next_write_must_echo() throws Exception {
            given(drafts.draft("draft-1")).willReturn(ConfigFixtures.draftRevision());

            mvc.perform(get("/api/v1/admin/drafts/{draftId}", "draft-1").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                    .andExpect(jsonPath("$.draftId").value("draft-1"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.recordTypes[0].recordType").value("object"))
                    .andExpect(jsonPath("$.screens[0].layout.regions[0].kind").value("REGION"));
        }

        @Test
        void answers_404_for_a_draft_that_does_not_exist() throws Exception {
            willThrow(new DraftNotFoundException("draft-9")).given(drafts).draft("draft-9");

            mvc.perform(get("/api/v1/admin/drafts/{draftId}", "draft-9").with(admin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type")
                            .value("https://docs.mpria.zetcom.com/problems/draft-not-found"));
        }

        @Test
        void replaces_a_screen_layout_and_returns_the_advanced_etag() throws Exception {
            given(drafts.replaceLayout(eq("draft-1"), eq("object-detail"), any(), eq("\"0\"")))
                    .willReturn(ConfigFixtures.draftRevision().withScreen(ConfigFixtures.objectDetailScreen()));

            mvc.perform(put("/api/v1/admin/drafts/{draftId}/layouts/{screenId}", "draft-1", "object-detail")
                            .with(admin())
                            .header(HttpHeaders.IF_MATCH, "\"0\"")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LAYOUT_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ETAG, "\"1\""));
        }

        @Test
        void answers_412_when_the_client_edited_a_copy_that_has_since_moved_on() throws Exception {
            willThrow(new StaleDraftException("draft-1", "\"0\"", "\"3\""))
                    .given(drafts).replaceLayout(eq("draft-1"), eq("object-detail"), any(), eq("\"0\""));

            mvc.perform(put("/api/v1/admin/drafts/{draftId}/layouts/{screenId}", "draft-1", "object-detail")
                            .with(admin())
                            .header(HttpHeaders.IF_MATCH, "\"0\"")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LAYOUT_JSON))
                    .andExpect(status().isPreconditionFailed())
                    .andExpect(jsonPath("$.type")
                            .value("https://docs.mpria.zetcom.com/problems/stale-draft"));
        }
    }

    @Nested
    class Publishing {

        @Test
        void returns_the_new_revision_pointer() throws Exception {
            given(drafts.publish("draft-1")).willReturn(ConfigFixtures.publishedRevision());

            mvc.perform(post("/api/v1/admin/drafts/{draftId}/publish", "draft-1").with(publisher()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.revisionId").value("rev-1"))
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));
        }

        @Test
        void answers_422_listing_every_problem_rather_than_a_stack_trace() throws Exception {
            willThrow(new ConfigurationValidationException(List.of(
                    new ConfigurationError("object-detail", "slot-x",
                            "binds field 'provenance', which record type 'object' does not define"),
                    new ConfigurationError("object-list", null,
                            "references record type 'thing', which this revision does not define"))))
                    .given(drafts).publish("draft-1");

            mvc.perform(post("/api/v1/admin/drafts/{draftId}/publish", "draft-1").with(publisher()))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.type")
                            .value("https://docs.mpria.zetcom.com/problems/invalid-configuration"))
                    .andExpect(jsonPath("$.errors[0].screenId").value("object-detail"))
                    .andExpect(jsonPath("$.errors[0].nodeId").value("slot-x"))
                    .andExpect(jsonPath("$.errors[0].message").value(
                            "binds field 'provenance', which record type 'object' does not define"))
                    // A screen-level problem belongs to no node, and says so by omission.
                    .andExpect(jsonPath("$.errors[1].nodeId").doesNotExist());
        }
    }

    @Nested
    class BundleTransfer {

        @Test
        void exports_a_revision_as_a_re_importable_bundle() throws Exception {
            given(drafts.revision("rev-1")).willReturn(ConfigFixtures.publishedRevision());

            mvc.perform(get("/api/v1/admin/revisions/{revisionId}/export", "rev-1").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.revisionId").value("rev-1"))
                    .andExpect(jsonPath("$.publishedAt").value("2026-08-01T10:15:30Z"))
                    .andExpect(jsonPath("$.recordTypes[0].fields[0].id").value("inventoryNumber"))
                    .andExpect(jsonPath("$.screens[0].screenId").value("object-detail"));
        }

        @Test
        void dry_runs_an_import_and_reports_the_effective_diff() throws Exception {
            given(drafts.importBundle(any(), eq(true))).willReturn(new ImportReport(
                    true, List.of(), null,
                    new ConfigurationDiff(List.of(), List.of(), List.of("object"),
                            List.of("object-list"), List.of(), List.of())));

            mvc.perform(post("/api/v1/admin/import").param("dryRun", "true")
                            .with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bundleJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.draftId").doesNotExist())
                    .andExpect(jsonPath("$.diff.recordTypesChanged[0]").value("object"))
                    .andExpect(jsonPath("$.diff.screensAdded[0]").value("object-list"));
        }

        @Test
        void treats_an_import_as_a_dry_run_unless_told_otherwise() throws Exception {
            // Writing is the surprising outcome, so it has to be the one the caller asks for.
            given(drafts.importBundle(any(), eq(true))).willReturn(
                    new ImportReport(true, List.of(), null, ConfigurationDiff.unchanged()));

            mvc.perform(post("/api/v1/admin/import")
                            .with(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bundleJson()))
                    .andExpect(status().isOk());

            then(drafts).should(never()).importBundle(any(), eq(false));
        }

        private String bundleJson() {
            return """
                    {"revisionId":"rev-1","publishedAt":"2026-08-01T10:15:30Z",
                     "recordTypes":[],"screens":[]}""";
        }
    }

    private static JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_mpria-config-admin"));
    }

    private static JwtRequestPostProcessor publisher() {
        return jwt().authorities(
                new SimpleGrantedAuthority("ROLE_mpria-config-admin"),
                new SimpleGrantedAuthority("ROLE_mpria-config-publisher"));
    }

    private static JwtRequestPostProcessor publisherOnly() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_mpria-config-publisher"));
    }

    private static JwtRequestPostProcessor curator() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_mpria-user"));
    }
}
