package io.github.artemptushkin.ai.assistants.test.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

class DutchPackageArchitectureTest {

    @Test
    fun `classes in foo_baz_dutch_should_only_be_accessed_by_classes_in_same_package`() {
        val importedClasses = ClassFileImporter().importPackages("io.github.artemptushkin.ai.assistants.dutch")

        val rule = classes()
            .that().resideOutsideOfPackage("io.github.artemptushkin.ai.assistants.dutch")
            .should().onlyDependOnClassesThat().resideOutsideOfPackage("io.github.artemptushkin.ai.assistants.dutch")

        rule.check(importedClasses)
    }
}