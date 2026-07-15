package com.yii2storm.modelmagic.type

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import com.yii2storm.modelmagic.resolver.MagicPropertyResolver
import com.yii2storm.modelmagic.util.MagicPropertyPsiUtil

/**
 * Provides types for Yii2 model magic-property field references.
 */
class MagicPropertyTypeProvider : PhpTypeProvider4 {

    override fun getKey(): Char = KEY

    override fun getType(psiElement: PsiElement): PhpType? {
        val fieldReference = psiElement as? FieldReference ?: return null
        val propertyName = fieldReference.name ?: return null
        val project = fieldReference.project
        val resolver = MagicPropertyResolver.getInstance(project)
        val modelClasses = MagicPropertyPsiUtil.resolveModelClasses(project, fieldReference)
        if (modelClasses.isEmpty()) {
            return null
        }

        val resolvedTypes = modelClasses
            .mapNotNull { resolver.getPropertyType(it, propertyName) }
            .flatMap { it.types }
            .distinct()
        if (resolvedTypes.isEmpty()) {
            return null
        }

        return PhpType().add(SIGNATURE_PREFIX + resolvedTypes.joinToString(TYPE_SEPARATOR))
    }

    override fun complete(expression: String, project: Project): PhpType? {
        val types = signaturePayload(expression)
            ?.split(TYPE_SEPARATOR)
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            .orEmpty()
        if (types.isEmpty()) {
            return null
        }

        return PhpType().apply {
            types.forEach(::add)
        }
    }

    override fun getBySignature(
        expression: String,
        visited: MutableSet<String>,
        depth: Int,
        project: Project,
    ): MutableCollection<out PhpNamedElement> {
        if (depth > MAX_DEPTH) {
            return mutableListOf()
        }

        return signaturePayload(expression)
            ?.split(TYPE_SEPARATOR)
            ?.map(String::trim)
            ?.filter { it.startsWith("\\") }
            ?.flatMap { PhpIndex.getInstance(project).getClassesByFQN(it) }
            ?.distinctBy(PhpNamedElement::getFQN)
            ?.toMutableList()
            ?: mutableListOf()
    }

    private fun signaturePayload(expression: String): String? {
        return expression
            .takeIf { it.startsWith(SIGNATURE_PREFIX) }
            ?.removePrefix(SIGNATURE_PREFIX)
    }

    companion object {
        private const val KEY = '\u03A8'
        private const val MAX_DEPTH = 10
        private const val TYPE_SEPARATOR = "|"
        private const val SIGNATURE_PREFIX = "#$KEY"
    }
}
