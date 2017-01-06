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
            containers.each { featureSetContainer ->
                def srcSet = androidExtension.sourceSets.findByName(featureSetContainer.name)
                println("------> ${featureSetContainer.name} - ${srcSet.name}")
                if (!featureSetContainer.features.isEmpty() && srcSet != null) {
                    featureSetContainer.features.each { feature ->
                        srcSet.java.srcDir "src/${feature}/java"
                        srcSet.res.srcDir "src/${feature}/res"
                    }
                    def manifestFiles = featureSetContainer.features
                            .collect { project.file("src/$it/AndroidManifest.xml") }
                            .findAll { it.exists() }
                    if (!manifestFiles.isEmpty()) {
                        def outManifest = project.file("${project.buildDir}/intermediates/manifests/featureSets/${srcSet.name}/AndroidManifest.xml")
//                        project.tasks.create(featureSetContainer.getTaskName(), MergeFeatureManifestsTask)
                        project.tasks.create([name: featureSetContainer.getTaskName(),
                                               description: "Generates merged XML files for featuresSets",
                                               type: MergeFeatureManifestsTask.class], {
                            inputFiles = featureSetContainer.features.plus(srcSet.name).collect { project.file("src/$it/AndroidManifest.xml") }
                            outputFile = outManifest
                        })
                        srcSet.manifest { srcFile(outManifest) }
                    }
                }
            }

            project.android.applicationVariants.all { ApplicationVariant variant ->
                def sources = variant.sourceSets.collect { it.java.srcDirs }
                    .inject { acc, fileSet -> acc.plus(fileSet) }
                def dummyTask = project.task("register${variant.name}FeatureSetSources")
                variant.registerJavaGeneratingTask(dummyTask, sources)

                containers.collect { project.getTasksByName(it.getTaskName(), false)}
                    .flatten()
                    .each {
                        LOGGER.info("Adding $it to variant: ${variant.name}")
                        variant.checkManifest.dependsOn(it)
                        variant.checkManifest.mustRunAfter(it)
                    }
//            validateFeatureSets()
//            containers.findByName(variant)

//            updateSourceSets (androidExtension, ext)
                // have to make sure the compiler sources are updated for some reason

//                def sources = variant.sourceSets.collect { AndroidSourceSet srcSet -> srcSet.java.srcDirs }
//                    .inject {Set<File> acc, Set<File> fileSet -> acc.plus(fileSet)}
//                variant.registerJavaGeneratingTask()
            }
        }
    }

    /*def mergeManifests(Set<String> featureSets, AndroidSourceSet srcSet) {
        def manifests = featureSets.plus(srcSet.name)
                .collect { project.file("src/$it/AndroidManifest.xml") }
                .findAll { it.exists() }
                .collect { new XmlSlurper(false, false).parse(it) }
        if (!manifests.empty) {
            def outXml = manifests
                    .inject
                    { xml1, xml2 ->
                        xml2.application.childNodes().each {
                            xml1.application.appendNode(it)
                        }; return xml1
                    }
            File manifestDir = project.file([project.buildDir.path, "intermediates", "manifests", "featuresets", srcSet.name].join(File.separator))
            manifestDir.mkdirs();
            def manifestFile = project.file("$manifestDir/AndroidManifest.xml")
            manifestFile.withWriter { outWriter ->
                XmlUtil.serialize(outXml, outWriter)
                outWriter.close()
                LOGGER.info("Merged manifest for $srcSet.name created in: $manifestFile.path")
            }
        }
    }*/

//    def updateSourceSets (ExtensionAware androidExtension, FeatureSetsExtension ext) {
//        def buildTypes = androidExtension.buildTypes // will be useful later when featureSets is NamedObjectCollection
//                .collect { it.name }
//                .plus("main")
//
//        def sets = ['main', 'debug', 'release', 'test', 'testDebug', 'testRelease']
//        androidExtension.sourceSets.findAll {
//            sets.contains(it.name)
//        }.each { AndroidSourceSet srcSet ->
//            if (srcSet.name.contains('test')) {
//                if (srcSet.name == 'test') {
//                    addTestSrcs(srcSet, 'main', ext)
//                } else {
//                    def feature = srcSet.name.minus('test').toLowerCase()
//                    addTestSrcs(srcSet, feature, ext)
//                }
//            } else {
//                addSrcsToSet(srcSet, ext[srcSet.name])
//            }
//        }
//    }
//
//    def addTestSrcs(AndroidSourceSet srcSet, String featureName, FeatureSetsExtension ext) {
//        ['java', 'resources'].each { folder ->
//            ext[featureName]
//                .collect { "src/$srcSet.name/$it/$folder" }
//                .each { srcSet[folder].srcDir it }
//        }
//    }
//
//    def addSrcsToSet(AndroidSourceSet srcSet, List<String> featureSets) {
//        ['java', 'res', 'resources', 'assets'].each { folder ->
//            featureSets
//                .collect { "src/$it/$folder" }
//                .each { srcSet[folder].srcDir it }
//            def dir = srcSet[folder].srcDirs
//            LOGGER.debug("Adding sources for $srcSet.name: $dir")
//        }
//    }

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

