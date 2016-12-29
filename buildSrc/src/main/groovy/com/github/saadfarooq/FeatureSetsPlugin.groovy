package com.github.saadfarooq

import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.ApplicationVariant
import groovy.xml.XmlUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionAware

final class FeatureSetsPlugin implements Plugin<Project> {
    Logger LOGGER
    Project project

    void apply(Project project, Object androidExtension) {
        this.project = project
        this.LOGGER = project.logger
        FeatureSetsExtension ext = project.android.extensions.create('featureSets', FeatureSetsExtension)
        project.afterEvaluate {
            updateSourceSets (androidExtension, ext)
            // have to make sure the compiler sources are updated for some reason
            project.android.applicationVariants.all { ApplicationVariant variant ->
                def sources = variant.sourceSets.collect { AndroidSourceSet srcSet -> srcSet.java.srcDirs }
                    .inject {Set<File> acc, Set<File> fileSet -> acc.plus(fileSet)}
                variant.javaCompiler.source(sources)
            }
        }
    }

    def updateSourceSets (ExtensionAware androidExtension, FeatureSetsExtension ext) {
        def buildTypes = androidExtension.buildTypes // will be useful later when featureSets is NamedObjectCollection
                .collect { it.name }
                .plus("main")

        def sets = ['main', 'debug', 'release', 'test', 'testDebug', 'testRelease']
        androidExtension.sourceSets.findAll {
            sets.contains(it.name)
        }.each { AndroidSourceSet srcSet ->
            if (srcSet.name.contains('test')) {
                if (srcSet.name == 'test') {
                    addTestSrcs(srcSet, 'main', ext)
                } else {
                    def feature = srcSet.name.minus('test').toLowerCase()
                    addTestSrcs(srcSet, feature, ext)
                }
            } else {
                addSrcsToSet(srcSet, ext[srcSet.name])
            }
        }
    }

    def addTestSrcs(AndroidSourceSet srcSet, String featureName, FeatureSetsExtension ext) {
        ['java', 'resources'].each { folder ->
            ext[featureName]
                .collect { "src/$srcSet.name/$it/$folder" }
                .each { srcSet[folder].srcDir it }
        }
    }

    def addSrcsToSet(AndroidSourceSet srcSet, List<String> featureSets) {
        ['java', 'res', 'resources', 'assets'].each { folder ->
            featureSets
                .collect { "src/$it/$folder" }
                .each { srcSet[folder].srcDir it }
            def dir = srcSet[folder].srcDirs
            LOGGER.debug("Adding sources for $srcSet.name: $dir")
        }

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
                srcSet.manifest { srcFile(manifestFile) }
            }
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

