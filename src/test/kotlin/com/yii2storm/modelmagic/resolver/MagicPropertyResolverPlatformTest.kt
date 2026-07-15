package com.yii2storm.modelmagic.resolver

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.yii2storm.modelmagic.settings.ModelMagicProjectSettingsService
import com.yii2storm.modelmagic.settings.ModelMagicProjectSettingsState

class MagicPropertyResolverPlatformTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        ModelMagicProjectSettingsService.getInstance(project)
            .loadState(ModelMagicProjectSettingsState())
    }

    fun testCollectsGetterAndSingleParameterSetter() {
        val model = createModel(
            """
            public function getFullName(): string { return ''; }
            public function setEmail(string ${'$'}email): void {}
            public function setBroken(string ${'$'}first, string ${'$'}second): void {}
            """.trimIndent(),
        )

        val properties = MagicPropertyResolver.getInstance(project)
            .getModelProperties(model)
            .associateBy(MagicProperty::name)

        assertEquals(PropertyKind.GETTER, properties["fullName"]?.kind)
        assertEquals(PropertyKind.SETTER, properties["email"]?.kind)
        assertFalse(properties.containsKey("broken"))
    }

    fun testMatchesSnakeCaseUsageToCamelCaseGetter() {
        val model = createModel(
            "public function getFullName(): string { return ''; }",
        )

        assertTrue(
            MagicPropertyResolver.getInstance(project)
                .hasProperty(model, "full_name"),
        )
    }

    fun testSettingsChangeInvalidatesCachedProperties() {
        val model = createModel(
            "public function getFullName(): string { return ''; }",
        )
        val resolver = MagicPropertyResolver.getInstance(project)
        val settings = ModelMagicProjectSettingsService.getInstance(project)

        assertTrue(resolver.hasProperty(model, "fullName"))

        settings.loadState(
            ModelMagicProjectSettingsState(enableGetter = false),
        )

        assertFalse(resolver.hasProperty(model, "fullName"))
    }

    fun testPreservesPhpDocUnionTypes() {
        val model = createModel(
            body = "",
            classDoc = "/** @property string|null ${'$'}nickname */",
        )

        val property = MagicPropertyResolver.getInstance(project)
            .getModelProperties(model)
            .single { it.name == "nickname" }

        val normalizedTypes = property.type?.types
            ?.map { it.removePrefix("\\") }
            ?.toSet()
        assertEquals(setOf("string", "null"), normalizedTypes)
    }

    private fun createModel(body: String, classDoc: String = ""): PhpClass {
        val file = myFixture.configureByText(
            PhpFileType.INSTANCE,
            """
            <?php
            namespace yii\base;
            class Model {}

            $classDoc
            class User extends Model {
                $body
            }
            """.trimIndent(),
        )

        return PsiTreeUtil.findChildrenOfType(file, PhpClass::class.java)
            .single { it.name == "User" }
    }

}
