package com.gdrn.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.gdrn", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {
    // Empty selections are intentional until the first business slice exists.
    // Each rule still imports all production classes and applies automatically as packages grow.
    @ArchTest
    static final ArchRule domain_is_framework_independent = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "javax.persistence..",
                    "org.hibernate..", "org.postgresql..", "java.sql..", "javax.sql..",
                    "com.fasterxml.jackson..", "jakarta.servlet..", "javax.servlet..",
                    "java.net.http..", "org.apache.http..", "org.apache.hc..",
                    "..infrastructure..", "..api..", "..application..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_does_not_depend_on_outer_layers = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "..api..", "org.springframework.data..",
                    "org.springframework.web..", "org.springframework.security..",
                    "jakarta.persistence..", "org.hibernate..", "org.postgresql..",
                    "java.sql..", "javax.sql..", "jakarta.servlet..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule api_does_not_access_persistence = noClasses()
            .that().resideInAnyPackage("..api..", "..controller..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure.persistence..", "..domain.repository..",
                    "org.springframework.data..", "jakarta.persistence..", "org.hibernate..",
                    "org.postgresql..", "java.sql..", "javax.sql..")
            .allowEmptyShould(true);

    @ArchTest
    static void modules_do_not_access_other_modules_infrastructure(JavaClasses classes) {
        for (String module : new String[]{"identity", "disaster", "reporting", "rescue",
                "resource", "alert", "geo", "shared"}) {
            // Configuration is wired by Spring; consumers must use inner-layer abstractions.
            noClasses().that().resideOutsideOfPackage("com.gdrn." + module + "..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.gdrn." + module + ".infrastructure..")
                    .allowEmptyShould(true).check(classes);
        }
    }
}
