package com.yii2storm.modelmagic

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.Method
import com.yii2storm.modelmagic.inspection.MagicPropertyInspection
import com.yii2storm.modelmagic.navigation.MagicPropertyGotoDeclarationHandler
import com.yii2storm.modelmagic.settings.ModelMagicProjectSettingsService
import com.yii2storm.modelmagic.settings.ModelMagicProjectSettingsState

class MagicPropertyFeaturesPlatformTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        ModelMagicProjectSettingsService.getInstance(project)
            .loadState(ModelMagicProjectSettingsState())
        addYiiModelFixture()
    }

    fun testCompletesGetterBackedProperty() {
        configureUsage("${'$'}user-><caret>")

        val lookupStrings = myFixture.completeBasic()
            ?.map { it.lookupString }
            .orEmpty()

        assertTrue("Completion variants: $lookupStrings", lookupStrings.contains("fullName"))
    }

    fun testGotoDeclarationTargetsGetter() {
        configureUsage("${'$'}user->fullNa<caret>me;")

        val sourceElement = myFixture.file.findElementAt(myFixture.caretOffset - 1)
        val targets = MagicPropertyGotoDeclarationHandler()
            .getGotoDeclarationTargets(sourceElement, myFixture.caretOffset, myFixture.editor)
            .orEmpty()
        val getter = targets
            .filterIsInstance<Method>()
            .firstOrNull { it.name == "getFullName" }

        assertNotNull("Targets: ${targets.toList()}", getter)
    }

    fun testInfersGetterReturnType() {
        configureUsage("${'$'}user->fullNa<caret>me;")

        val fieldReference = PsiTreeUtil.findChildOfType(myFixture.file, FieldReference::class.java)
        assertNotNull("No FieldReference in ${myFixture.file.text}", fieldReference)
        val completedType = fieldReference
            ?.type
            ?.let { PhpIndex.getInstance(project).completeType(project, it, mutableSetOf()) }
        val normalizedTypes = completedType
            ?.types
            ?.map { it.removePrefix("\\") }
            .orEmpty()

        assertTrue("Inferred types: $normalizedTypes", normalizedTypes.contains("string"))
    }

    fun testReportsUnknownPropertyWithoutGeneratedQuickFixes() {
        myFixture.enableInspections(MagicPropertyInspection())
        configureUsage("${'$'}user->missingProperty;")

        val highlights = myFixture.doHighlighting()
        val problems = highlights.filter(::isUnknownPropertyProblem)

        assertEquals("Highlights: ${highlights.map { it.description }}", 1, problems.size)
        assertEmpty(myFixture.availableIntentions.filter { it.text.contains("missingProperty") })
    }

    private fun addYiiModelFixture() {
        myFixture.addFileToProject(
            "yii/base/Model.php",
            "<?php namespace yii\\base; class Model {}",
        )
        myFixture.addFileToProject(
            "app/models/User.php",
            """
            <?php
            namespace app\models;
            class User extends \yii\base\Model {
                public function getFullName(): string { return ''; }
            }
            """.trimIndent(),
        )
    }

    private fun configureUsage(expression: String) {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            """
            <?php
            function inspectUser(\app\models\User ${'$'}user): void {
                $expression
            }
            """.trimIndent(),
        )
        assertNotNull(
            "No FieldReference in ${myFixture.file.text}",
            PsiTreeUtil.findChildOfType(myFixture.file, FieldReference::class.java),
        )
    }

    private fun isUnknownPropertyProblem(info: HighlightInfo): Boolean {
        return info.description?.contains("Unknown Yii2 model property") == true
    }
}
