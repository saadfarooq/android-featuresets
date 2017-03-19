package com.github.saadfarooq

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.logging.Logger

final class FeatureSetsPlugin implements Plugin<Project> {
    Logger LOGGER
    Project project

    void apply(Project project, AppExtension androidExtension) {
        this.project = project
        this.LOGGER = project.logger
        project.extensions.add("featureSets", project.container(FeatureSetContainer.class))
        project.afterEvaluate {
            def containers = project.extensions.getByName("featureSets") as NamedDomainObjectCollection<FeatureSetContainer>

            validateFeatureSets(containers, androidExtension.buildTypes)

            containers.each { featureSetContainer ->
                LOGGER.info("Processing feature set: ${featureSetContainer.name} with features: ${featureSetContainer.features}")
                def srcSet = androidExtension.sourceSets.findByName(featureSetContainer.name)
                def testSrcSetName = featureSetContainer.name == "main" ? "test" : "test${featureSetContainer.name.capitalize()}"
                def testSrcSet = androidExtension.sourceSets.findByName(testSrcSetName)

                if (!featureSetContainer.features.isEmpty() && srcSet != null) {
                    featureSetContainer.features.each { feature ->
                        srcSet.java.srcDir "src/${feature}/java"
                        srcSet.res.srcDir "src/${feature}/res"
                        srcSet.resources.srcDir "src/${feature}/resources"
                        srcSet.assets.srcDir "src/${feature}/assets"

                        if (featureSetContainer.encapsulateTests) {
                            testSrcSet.java.srcDir "src/${feature}/test/java"
                            testSrcSet.res.srcDir "src/${feature}/test/res"
                            testSrcSet.resources.srcDir "src/${feature}/test/resources"
                            testSrcSet.assets.srcDir "src/${feature}/test/assets"
                        } else if (featureSetContainer.name != "main") {
                            testSrcSet.java.srcDir "src/test${feature.capitalize()}/java"
                            testSrcSet.res.srcDir "src/test${feature.capitalize()}/res"
                            testSrcSet.resources.srcDir "src/test${feature.capitalize()}/resources"
                            testSrcSet.assets.srcDir "src/test${feature.capitalize()}/assets"
                        }
                    }

                    def existingManifestFiles = featureSetContainer.features
                            .collect { project.file("src/$it/AndroidManifest.xml") }
                            .findAll { it.exists() }

                    if (!existingManifestFiles.isEmpty()) {
                        LOGGER.info("Adding a task for merging featureSet manifests ${featureSetContainer.getTaskName()}")
                        def manifestOut = project.file("${project.buildDir}/intermediates/manifests/featureSets/${srcSet.name}/AndroidManifest.xml")
                        project.tasks.create([name: featureSetContainer.getTaskName(),
                                              description: "Generates merged XML files for featuresSets",
                                              type: MergeFeatureManifestsTask.class], {
                            logger = LOGGER
                            mainManifest = srcSet.manifest.srcFile
                            featureManifests = featureSetContainer.features.collect {
                                project.file("src/$it/AndroidManifest.xml")
                            }.findAll { it.exists() }
                            outputFile = manifestOut
                        })
                        srcSet.manifest { srcFile(manifestOut) }
                    }
                }
            }

            project.android.applicationVariants.all { ApplicationVariant variant ->
                def sources = variant.sourceSets.collect { it.java.srcDirs }
                        .inject { acc, fileSet -> acc.plus(fileSet) }
                variant.javaCompiler.source(sources)

                if (variant.unitTestVariant != null) {
                    def unitTestSources = variant.unitTestVariant.sourceSets
                            .collect { it.java.srcDirs }
                            .inject { acc, fileSet -> acc.plus(fileSet) }
                    variant.unitTestVariant.javaCompiler.source(unitTestSources)
                }

                containers.collect { project.getTasksByName(it.getTaskName(), false) }
                        .flatten()
                        .each {
                    LOGGER.info("Adding $it to variant: ${variant.name}")
                    variant.checkManifest.dependsOn(it)
                    variant.checkManifest.mustRunAfter(it)
                }
            }
        }
    }

    static void validateFeatureSets(containers, buildTypes) {
        def diff = containers.collect { it.name }.minus(buildTypes.collect { it.name }).minus("main") // buildTypes doesn't need to contain main
        if (!diff.isEmpty()) {
            throw new IllegalArgumentException("FeatureSets must have a corresponding buildType, following featureSets don't: $diff")
        }
    }

    @Override
    public void apply(Project project) {
        if (!["com.android.application",
              "android",
              "com.android.library",
              "android-library"].any { project.plugins.findPlugin(it) }) {
            throw new ProjectConfigurationException("Please apply 'com.android.application' or 'com.android.library' plugin before applying 'featuresets' plugin", null)
        }
        apply(project, project.extensions.getByName("android"))
    }
}

