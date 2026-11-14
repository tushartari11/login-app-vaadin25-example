package com.example.app.ui;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Belt and braces: Maven scopes already block this at compile time.
 */
@AnalyzeClasses(packages = "com.example.app.ui", importOptions = ImportOption.DoNotIncludeTests.class)
class UiBoundaryTest {

  @ArchTest
  static final ArchRule ui_uses_application_only = noClasses()
      .that().resideInAPackage("com.example.app.ui..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "com.example.app.domain..", "com.example.app.infrastructure..",
          "jakarta.persistence..", "java.sql..");
}
