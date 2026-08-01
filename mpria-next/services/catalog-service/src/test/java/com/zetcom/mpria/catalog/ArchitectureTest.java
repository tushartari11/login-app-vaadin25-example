package com.zetcom.mpria.catalog;

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
        packages = "com.zetcom.mpria.catalog",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers_are_respected = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..catalog.domain..")
            .layer("Application").definedBy("..catalog.application..")
            .layer("Infrastructure").definedBy("..catalog.infrastructure..")
            .layer("Api").definedBy("..catalog.api..")

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
            noClasses().that().resideInAPackage("..catalog.domain..")
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
            noClasses().that().resideInAPackage("..catalog.api..")
                    .should().dependOnClassesThat().resideInAPackage("..catalog.infrastructure..")
                    .because("the API layer must go through use cases, not adapters");

    /** Persistence details must not leak outward through the port. */
    @ArchTest
    static final ArchRule entities_stay_inside_the_persistence_adapter =
            classes().that().haveSimpleNameEndingWith("Entity")
                    .should().resideInAPackage("..catalog.infrastructure.persistence..")
                    .because("a JPA entity is a mapping detail, not a domain or API type");

    /** Transactions are an application-layer concern; declaring them elsewhere hides the boundary. */
    @ArchTest
    static final ArchRule transactions_are_declared_in_the_application_layer =
            noClasses().that().resideInAnyPackage("..catalog.api..", "..catalog.domain..")
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("org.springframework.transaction.annotation.Transactional")
                    .because("the transaction boundary is one aggregate, owned by the use case");

    /** Logging goes through SLF4J so it can be structured, correlated and shipped to OpenSearch. */
    @ArchTest
    static final ArchRule nothing_writes_to_standard_out =
            noClasses().should().accessField(System.class, "out")
                    .orShould().accessField(System.class, "err")
                    .because("logs must be structured and carry a trace id (ARCHITECTURE.md §11)");
}
