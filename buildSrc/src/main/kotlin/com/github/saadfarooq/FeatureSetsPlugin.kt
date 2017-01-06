package com.github.saadfarooq;

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.tasks.factory.AndroidJavaCompile
import com.github.saadfarooq.model.FeatureSet
import org.atteo.xmlcombiner.XmlCombiner
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import java.io.File

class FeatureSetsPlugin : Plugin<Project> {
    private lateinit var LOGGER: Logger

    override fun apply(project: Project?) {
        this.LOGGER = project!!.logger // this shouldn't be null
        val featureSetContainer = project.container(FeatureSet::class.java)

        project.extensions.add("featureSets", featureSetContainer)
        project.plugins.all {
            when (it) {
                is AppPlugin -> processSourceSets(project,
                        project.extensions.getByType(AppExtension::class.java))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun processSourceSets(project: Project, androidExtension: AppExtension) {
        project.afterEvaluate {
            val featureSets = project.extensions.getByName("featureSets") as NamedDomainObjectCollection<FeatureSet>
            featureSets
                    .apply {
                        map { it.name }
                                .filter { it != "main" } // main buildtypes always exists even if not defined
                                .let {
                                    val diff = it.minus(androidExtension.buildTypes.map { it.name })
                                    if (diff.isNotEmpty()) {
                                        throw IllegalArgumentException("Feature sets must match up to build types, " +
                                                "following feature sets don't: $diff")
                                    }
                                }
                    }.filter { it.features.isNotEmpty() }
                    .forEach { featureSet ->
                        println("processing: ${featureSet.name}")
                        androidExtension.sourceSets.getByName(featureSet.name) // debug, main, release
                                ?.let { srcSet ->
                                    featureSet.features.forEach { srcSet.addSrcFolders("src/$it") }
                                    LOGGER.info("Creating merge${featureSet.name.capitalize()}FeatureManifest task $srcSet")
                                    val srcManifest = project.buildDir.resolve("intermediates/manifests/featureSets/${srcSet.name}/AndroidManifest.xml")
                                    project.tasks.create("merge${featureSet.name.capitalize()}FeatureManifest").doLast {
                                        mergeManifests(project, srcSet, featureSet.features, srcManifest, srcSet.manifest.srcFile)
                                    }
                                    srcSet.manifest.srcFile(srcManifest)
                                }
                        val testSet = if (featureSet.name == "main") "test" else "test${featureSet.name.capitalize()}"
                        androidExtension.sourceSets.findByName(testSet)
                                ?.let { srcSet ->
                                    featureSet.features.forEach { srcSet.addSrcFolders("src/$it/test") }
                                }
                    }

            androidExtension.applicationVariants.forEach { variant ->
                val files = variant.sourceSets.map { it.javaDirectories }
                        .reduce { acc, i -> acc.plus(i) }
                val dummyGeneratingTask = project.task("register${variant.name.capitalize()}FeatureSetSrcs")
                variant.registerJavaGeneratingTask(dummyGeneratingTask, files)
                featureSets.map { project.getTasksByName("merge${it.name.capitalize()}FeatureManifest", false) }
                        .flatten()
                        .forEach {
                            LOGGER.info("Adding $it to variant: ${variant.name}")
                            variant.checkManifest.dependsOn(it)
                            variant.checkManifest.mustRunAfter(it)
                        }
            }
        }
    }

    private fun mergeManifests(project: Project, srcSet: AndroidSourceSet, features: Set<String>) {
        project.tasks.create("processFeatureManifest", FeatureManifestTask::class.java)
        features.map { project.file("src/$it/AndroidManifest.xml") }
                .plus(srcSet.manifest.srcFile)
                .filter(File::exists)
                .let { if (it.isNotEmpty())
                    it.fold(XmlCombiner(), { xmlCombiner, manifestFile -> xmlCombiner.let { it.combine(manifestFile.toPath()); it }})
                            .let {
                                val srcManifest = project.buildDir.resolve("intermediates/manifests/featureSets/${srcSet.name}/AndroidManifest.xml")
                                LOGGER.info("Writing featureSet manifest to ${srcManifest.path}")
                                it.buildDocument(
                                        srcManifest
                                                .apply { parentFile.mkdirs(); createNewFile() }
                                                .outputStream())
                                srcSet.manifest.srcFile(srcManifest)
                            }
                }
    }

    fun AndroidSourceSet.addSrcFolders(folder: String) {
        this.java.srcDir("$folder/java")
        this.res.srcDir("$folder/res")
        this.resources.srcDir("$folder/resources")
        this.assets.srcDir("$folder/assets")
    }
}


