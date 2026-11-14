package com.example.app.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fails the build if a framework sneaks into the domain.
 */
@AnalyzeClasses(packages = "com.example.app.domain", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainPurityTest {

  @ArchTest
  static final ArchRule domain_is_framework_free = noClasses()
      .that().resideInAPackage("com.example.app.domain..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "jakarta..", "javax..", "com.vaadin..", "org.springframework..",
          "io.quarkus..", "org.hibernate..", "org.eclipse.persistence..",
          "java.sql..", "org.slf4j..")
      .because("the domain must stay pure Java so any UI or persistence technology can be swapped");
}
