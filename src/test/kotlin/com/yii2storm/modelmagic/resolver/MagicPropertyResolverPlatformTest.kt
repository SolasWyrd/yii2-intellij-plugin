package com.yii2storm.modelmagic.resolver

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method
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

    fun testResolvesRelationTargetTypesAndCardinality() {
        myFixture.addFileToProject(
            "yii/base/Model.php",
            "<?php namespace yii\\base; class Model {}",
        )
        myFixture.addFileToProject(
            "app/entities/Profile.php",
            "<?php namespace app\\entities; class Profile extends \\yii\\base\\Model {}",
        )
        myFixture.addFileToProject(
            "app/models/Post.php",
            "<?php namespace app\\models; class Post extends \\yii\\base\\Model {}",
        )
        val file = myFixture.configureByText(
            PhpFileType.INSTANCE,
            """
            <?php
            namespace app\models;
            use app\entities\Profile as AccountProfile;

            class User extends \yii\base\Model {
                public function getProfile() {
                    return ${'$'}this->hasOne(AccountProfile::class, ['id' => 'profile_id'])->where([]);
                }

                public function getPosts() {
                    return ${'$'}this->hasMany(Post::class, ['user_id' => 'id'])->inverseOf('user');
                }

                public function getDynamicRelation(): \yii\db\ActiveQuery {
                    return ${'$'}this->hasOne(${'$'}this->relationClass(), ['id' => 'target_id']);
                }
            }
            """.trimIndent(),
        )
        val model = PsiTreeUtil.findChildrenOfType(file, PhpClass::class.java)
            .single { it.name == "User" }

        val properties = MagicPropertyResolver.getInstance(project)
            .getModelProperties(model)
            .associateBy(MagicProperty::name)

        assertEquals(PropertyKind.RELATION, properties["profile"]?.kind)
        assertEquals(setOf("\\app\\entities\\Profile"), properties["profile"]?.type?.types?.toSet())
        assertEquals(PropertyKind.RELATION, properties["posts"]?.kind)
        assertEquals(setOf("\\app\\models\\Post[]"), properties["posts"]?.type?.types?.toSet())
        assertEquals(PropertyKind.RELATION, properties["dynamicRelation"]?.kind)
        assertNull(properties["dynamicRelation"]?.type)
    }

    fun testCollectsInheritedRelationFromParentModel() {
        myFixture.addFileToProject(
            "yii/base/Model.php",
            "<?php namespace yii\\base; class Model {}",
        )
        myFixture.addFileToProject(
            "app/models/Post.php",
            "<?php namespace app\\models; class Post extends \\yii\\base\\Model {}",
        )
        myFixture.addFileToProject(
            "app/models/BaseUser.php",
            """
            <?php
            namespace app\models;
            class BaseUser extends \yii\base\Model {
                public function getPosts() {
                    return ${'$'}this->hasMany(Post::class, ['user_id' => 'id']);
                }
            }
            """.trimIndent(),
        )
        val file = myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php namespace app\\models; class User extends BaseUser {}",
        )
        val model = PsiTreeUtil.findChildrenOfType(file, PhpClass::class.java)
            .single { it.name == "User" }

        val property = MagicPropertyResolver.getInstance(project)
            .getModelProperties(model)
            .single { it.name == "posts" }

        assertEquals(PropertyKind.RELATION, property.kind)
        assertEquals(setOf("\\app\\models\\Post[]"), property.type?.types?.toSet())
        assertEquals("getPosts", (property.source as? Method)?.name)
    }

    fun testResolvesLegacyClassNameRelationTarget() {
        myFixture.addFileToProject(
            "yii/base/Model.php",
            """
            <?php
            namespace yii\base;
            class Model {
                public static function className(): string { return static::class; }
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "app/entities/Post.php",
            "<?php namespace app\\entities; class Post extends \\yii\\base\\Model {}",
        )
        val file = myFixture.configureByText(
            PhpFileType.INSTANCE,
            """
            <?php
            namespace app\models;
            use app\entities\Post as Article;

            class User extends \yii\base\Model {
                public function getPosts() {
                    return ${'$'}this->hasMany(Article::className(), ['user_id' => 'id']);
                }
            }
            """.trimIndent(),
        )
        val model = PsiTreeUtil.findChildrenOfType(file, PhpClass::class.java)
            .single { it.name == "User" }

        val property = MagicPropertyResolver.getInstance(project)
            .getModelProperties(model)
            .single { it.name == "posts" }

        assertEquals(PropertyKind.RELATION, property.kind)
        assertEquals(setOf("\\app\\entities\\Post[]"), property.type?.types?.toSet())
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
