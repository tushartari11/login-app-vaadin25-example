package com.zetcom.mpria.config;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The layering, enforced.
 *
 * <p>MpRIA v8 did not lose its layer boundaries suddenly; it lost them one reasonable-looking
 * import at a time. These rules make each of those imports a build failure instead of a slow
 * erosion, and they are the single cheapest guardrail available on day one.
 */
@AnalyzeClasses(
        packages = "com.zetcom.mpria.config",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers_are_respected = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..config.domain..")
            .layer("Application").definedBy("..config.application..")
            .layer("Infrastructure").definedBy("..config.infrastructure..")
            .layer("Api").definedBy("..config.api..")

            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Api", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Api");

    /**
     * The domain is pure. It may not know that Spring, JPA, HTTP or JSON exist — which is what
     * makes it testable in microseconds and portable if the framework ever changes again.
     */
    @ArchTest
    static final ArchRule the_domain_depends_on_nothing =
            noClasses().that().resideInAPackage("..config.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.servlet..",
                            "com.fasterxml.jackson..",
                            "tools.jackson..",
                            "org.hibernate..")
                    .because("the domain layer must not import a framework (ARCHITECTURE.md §6, P1)");

    /**
     * Controllers talk to use cases, never to storage. A controller holding a repository is how
     * business logic ends up in the presentation layer.
     */
    @ArchTest
    static final ArchRule controllers_do_not_reach_past_the_application_layer =
            noClasses().that().resideInAPackage("..config.api..")
                    .should().dependOnClassesThat().resideInAPackage("..config.infrastructure..")
                    .because("the API layer must go through use cases, not adapters");

    /**
     * The publish gate is a domain rule. Re-implementing "does this field exist" in a controller
     * is how two validators end up disagreeing, and the lenient one always wins.
     */
    @ArchTest
    static final ArchRule only_the_domain_decides_what_is_publishable =
            classes().that().haveSimpleNameEndingWith("Validator")
                    .should().resideInAPackage("..config.domain..")
                    .because("what may be published is a business rule, not a request concern");

    /** Storage details must not leak outward through the port. */
    @ArchTest
    static final ArchRule the_configuration_store_stays_behind_its_port =
            classes().that().haveSimpleNameEndingWith("ConfigurationRepository")
                    .and().areNotInterfaces()
                    .should().resideInAPackage("..config.infrastructure..")
                    .because("an in-memory map is an adapter, and Phase 2 replaces it with PostgreSQL");

    /** Logging goes through SLF4J so it can be structured, correlated and shipped to OpenSearch. */
    @ArchTest
    static final ArchRule nothing_writes_to_standard_out =
            noClasses().should().accessField(System.class, "out")
                    .orShould().accessField(System.class, "err")
                    .because("logs must be structured and carry a trace id (ARCHITECTURE.md §11)");
}
