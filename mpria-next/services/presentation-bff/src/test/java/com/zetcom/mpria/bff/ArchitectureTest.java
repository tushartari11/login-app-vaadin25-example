package com.zetcom.mpria.bff;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * The layering, enforced.
 *
 * <p>MpRIA v8 did not lose its layer boundaries suddenly; it lost them one reasonable-looking
 * import at a time. These rules make each of those imports a build failure instead of a slow
 * erosion, and they are the single cheapest guardrail available on day one.
 */
@AnalyzeClasses(
        packages = "com.zetcom.mpria.bff",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers_are_respected = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..bff.domain..")
            .layer("Application").definedBy("..bff.application..")
            .layer("Infrastructure").definedBy("..bff.infrastructure..")
            .layer("Api").definedBy("..bff.api..")

            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Api", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Api");

    /**
     * The domain is pure. It may not know that Spring, JPA, HTTP or JSON exist — which is what
     * makes it testable in microseconds and portable if the framework ever changes again.
     *
     * <p>{@code pm-contract} is deliberately not on this list. It is a dependency-free library of
     * value records, not a framework, and a configured layout genuinely is domain data here.
     */
    @ArchTest
    static final ArchRule the_domain_depends_on_nothing =
            noClasses().that().resideInAPackage("..bff.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.servlet..",
                            "com.fasterxml.jackson..",
                            "tools.jackson..",
                            "org.hibernate..")
                    .because("the domain layer must not import a framework (ARCHITECTURE.md §6, P1)");

    /**
     * This service owns no data (ARCHITECTURE.md §5). Expressing that as a rule turns "someone
     * added a table to the BFF" from a design discussion into a red build.
     */
    @ArchTest
    static final ArchRule nothing_here_persists_anything =
            noClasses().should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..",
                            "org.springframework.data..",
                            "javax.sql..")
                    .because("the presentation model layer holds no database — it composes other services' data");

    /**
     * The assembler is the heart of the service and must stay unit-testable with plain fakes. It
     * loses that property the moment it learns what a request, a response or a servlet is.
     */
    @ArchTest
    static final ArchRule the_application_layer_does_not_speak_http =
            noClasses().that().resideInAPackage("..bff.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..",
                            "org.springframework.http..",
                            "jakarta.servlet..")
                    .because("assembly is orchestration and mapping; transport belongs to infrastructure and api");

    /**
     * Controllers talk to use cases, never to adapters. A controller holding an HTTP client is how
     * orchestration ends up in the presentation layer.
     */
    @ArchTest
    static final ArchRule controllers_do_not_reach_past_the_application_layer =
            noClasses().that().resideInAPackage("..bff.api..")
                    .should().dependOnClassesThat().resideInAPackage("..bff.infrastructure..")
                    .because("the API layer must go through use cases, not adapters");

    /**
     * A stateful BFF cannot be scaled horizontally, which is the entire reason this layer replaced
     * the Vaadin session (ARCHITECTURE.md §7.2).
     */
    @ArchTest
    static final ArchRule nothing_holds_a_session =
            noClasses().should().dependOnClassesThat()
                    .haveFullyQualifiedName("jakarta.servlet.http.HttpSession")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("jakarta.servlet.http.HttpSessionListener")
                    .because("the BFF is stateless: no session affinity to preserve (ARCHITECTURE.md §7.2)");

    /**
     * The application class declares no beans.
     *
     * <p>Every test slice loads the {@code @SpringBootApplication} class and instantiates whatever it
     * declares, even when the slice has filtered out the adapters those beans depend on. One
     * {@code @Bean} method needing a gateway is therefore enough to fail every {@code @WebMvcTest} with
     * an unsatisfied dependency. Wiring belongs in ordinary {@code @Configuration} classes, which a
     * slice excludes wholesale.
     */
    @ArchTest
    static final ArchRule the_application_class_declares_no_beans =
            noMethods().that().areDeclaredInClassesThat()
                    .areAnnotatedWith("org.springframework.boot.autoconfigure.SpringBootApplication")
                    .should().beAnnotatedWith("org.springframework.context.annotation.Bean")
                    .because("a bean declared on the application class is built by every test slice");

    /** Logging goes through SLF4J so it can be structured, correlated and shipped to OpenSearch. */
    @ArchTest
    static final ArchRule nothing_writes_to_standard_out =
            noClasses().should().accessField(System.class, "out")
                    .orShould().accessField(System.class, "err")
                    .because("logs must be structured and carry a trace id (ARCHITECTURE.md §11)");
}
