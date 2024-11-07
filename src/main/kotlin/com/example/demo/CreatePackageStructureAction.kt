package com.example.demo
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.io.File

class GenerateModuleStructureAction : AnAction() {

    private val modules = listOf("data", "domain", "presentation", "shared")

    override fun actionPerformed(e: AnActionEvent) {
        // Show input dialog to get the package name
        val packageName = Messages.showInputDialog(
            e.project,
            "Enter the package name (e.g., com.example.cleanarch):",
            "Package Name",
            Messages.getQuestionIcon()
        ) ?: return

        val baseDir = File(e.project?.basePath ?: return)
        val basePackagePath = packageName.replace('.', '/')

        // Create the base package directory
        val packageDir = File(baseDir, "feature/$basePackagePath")
        packageDir.mkdirs()

        // Create each module structure within the base package
        modules.forEach { module ->
            createModuleStructure(packageDir, module, packageName)
        }

        updateSettingsGradle(baseDir, packageName)

    }

    private fun createModuleStructure(basePackageDir: File, module: String, packageName: String) {
        val moduleDir = File(basePackageDir, module)
        moduleDir.mkdirs()

        // Create sub-packages and respective classes for each module
        when (module) {
            "data" -> createDataSubpackages(moduleDir, packageName)
            "domain" -> createDomainSubpackages(moduleDir, packageName)
            "presentation" -> createPresentationSubpackages(moduleDir, packageName)
            "shared" -> createSharedSubpackages(moduleDir, packageName)
        }

        // Write additional files within each module directory
        writeBuildGradle(File(basePackageDir, "$module/build.gradle"))
        writeAndroidManifest(File(moduleDir.parentFile, "AndroidManifest.xml"), packageName, module)
        writeAdditionalFiles(File(basePackageDir, module))
    }

    private fun createDataSubpackages(moduleDir: File, packageName: String) {
        listOf("di", "repository").forEach { subPackage ->
            File(moduleDir, subPackage).mkdirs()
            if (subPackage == "repository") {
                val repoImplFile = File(moduleDir, "repository/${moduleDir.name.capitalize()}RepositoryImpl.kt")
                repoImplFile.writeText(repositoryImplTemplate(packageName))
            }
        }
    }

    private fun createDomainSubpackages(moduleDir: File, packageName: String) {
        listOf("di", "model", "repository", "usecase", "utils").forEach {
            File(moduleDir, it).mkdirs()
        }
    }

    private fun createPresentationSubpackages(moduleDir: File, packageName: String) {
        listOf("analytics", "di", "navigation", "screens", "utils").forEach {
            File(moduleDir, it).mkdirs()
        }
        writeScreenClasses(File(moduleDir, "screens"), packageName)
        writeAnalyticsClasses(File(moduleDir, "analytics"), packageName)
    }

    private fun createSharedSubpackages(moduleDir: File, packageName: String) {
        listOf("di", "navigation", "ui", "repository", "usecase").forEach {
            File(moduleDir, it).mkdirs()
        }
    }

    private fun writeScreenClasses(screensDir: File, packageName: String) {
        screensDir.mkdirs()
        val className = packageName.split('.').last().capitalize()
        File(screensDir, "${className}ViewModel.kt").writeText(viewModelTemplate(packageName, className))
        File(screensDir, "${className}Event.kt").writeText(eventTemplate(packageName, className))
        File(screensDir, "${className}State.kt").writeText(stateTemplate(packageName, className))
    }

    private fun writeAnalyticsClasses(analyticsDir: File, packageName: String) {
        analyticsDir.mkdirs()
        File(analyticsDir, "Analytics.kt").writeText(analyticsObjectTemplate(packageName))
        File(analyticsDir, "AnalyticsHelper.kt").writeText(analyticsHelperTemplate(packageName))
    }

    private fun writeBuildGradle(file: File) {
        file.writeText(buildGradleTemplate)
    }

    private fun writeAndroidManifest(file: File, packageName: String, module: String) {
        file.writeText(androidManifestTemplate(packageName, module))
    }

    private fun writeAdditionalFiles(moduleDir: File) {
        File(moduleDir, ".gitignore").writeText(gitignoreContent)
        File(moduleDir, "gradle.properties").writeText(gradlePropertiesContent)
        File(moduleDir, "proguard-rules.pro").writeText(proguardRulesContent)
    }

    private val gitignoreContent = """
        # Gradle
        .gradle/
        build/
        .idea/
        .DS_Store
        *.iml
        local.properties
    """.trimIndent()

    private val gradlePropertiesContent = """
        # Project-wide Gradle settings
    """.trimIndent()

    private val proguardRulesContent = """
        -dontwarn okhttp3.**
        -keep class com.example.** { *; }
    """.trimIndent()

    private fun repositoryImplTemplate(packageName: String) = """
        package $packageName.data.repository

        class ${packageName.split('.').last().capitalize()}RepositoryImpl {
            // Repository implementation logic here
        }
    """.trimIndent()

    private fun viewModelTemplate(packageName: String, className: String) = """
        package $packageName.presentation.screens

        import androidx.lifecycle.ViewModel
        import dagger.hilt.android.lifecycle.HiltViewModel
        import javax.inject.Inject

        @HiltViewModel
        class ${className}ViewModel @Inject constructor(
            mAnalyticsHelper: IAnalyticsHelper
        ) : ViewModel() {
            // ViewModel logic here
        }
    """.trimIndent()

    private fun eventTemplate(packageName: String, className: String) = """
        package $packageName.presentation.screens

        sealed class ${className}Event {
            object EventOne : ${className}Event()
            object EventTwo : ${className}Event()
        }
    """.trimIndent()

    private fun stateTemplate(packageName: String, className: String) = """
        package $packageName.presentation.screens

        data class ${className}State(
            val isLoading: Boolean = false,
            val data: String? = null
        )
    """.trimIndent()

    private fun analyticsObjectTemplate(packageName: String) = """
        package $packageName.presentation.analytics

        import javax.inject.Singleton

        @Singleton
        object Analytics {
            init {
                // Initialization logic here
            }
        }
    """.trimIndent()

    private fun analyticsHelperTemplate(packageName: String) = """
        package $packageName.presentation.analytics

        import android.content.Context
        import dagger.hilt.android.qualifiers.ApplicationContext
        import javax.inject.Inject
        import javax.inject.Singleton

        @Singleton
        class AnalyticsHelper @Inject constructor(
            @ApplicationContext val context: Context,
        )
    """.trimIndent()

    private val buildGradleTemplate = """
        apply plugin: 'com.android.library'

        android {
            compileSdkVersion 30
            defaultConfig {
                minSdkVersion 21
                targetSdkVersion 30
                testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
            }
            buildTypes {
                release {
                    minifyEnabled false
                    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                }
            }
        }

        dependencies {
            implementation "androidx.core:core-ktx:1.6.0"
            implementation "androidx.appcompat:appcompat:1.3.1"
            testImplementation "junit:junit:4.13.2"
            androidTestImplementation "androidx.test.ext:junit:1.1.3"
            androidTestImplementation "androidx.test.espresso:espresso-core:3.4.0"
        }
    """

    private fun androidManifestTemplate(packageName: String, module: String) = """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="$packageName.$module">
            <application />
        </manifest>
    """.trimIndent()

    private fun updateSettingsGradle(baseDir: File, moduleName: String) {
        val settingsFile = File(baseDir, "settings.gradle")
        if (settingsFile.exists()) {
            val includeStatement = "include ':feature:$moduleName'\ninclude ':feature:$moduleName:data'\ninclude ':feature:$moduleName:domain'\ninclude ':feature:$moduleName:presentation'\ninclude ':feature:$moduleName:shared'"
            settingsFile.appendText("\n$includeStatement")
        } else {
            println("settings.gradle file not found in project root.")
        }
    }
}
