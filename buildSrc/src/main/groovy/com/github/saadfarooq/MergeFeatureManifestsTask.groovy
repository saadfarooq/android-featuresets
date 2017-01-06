package com.github.saadfarooq

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class MergeFeatureManifestsTask extends DefaultTask {
    def List<File> inputFiles
    def File outputFile

    @TaskAction
    def execute(IncrementalTaskInputs inputs) {
        println inputs.incremental ? "-----> CHANGED inputs considered out of date"
                : "----> ALL inputs considered out of date"
        println("--------> Input files: $inputFiles, output: $outputFile")
        def manifests = inputFiles
                .findAll { it.exists() }
                .collect { new XmlSlurper(false, false).parse(it) }
        println("-----> $manifests")
        if (!manifests.empty) {
            def outXml = manifests
                    .inject
                    { xml1, xml2 ->
                        xml2.application.childNodes().each {
                            xml1.application.appendNode(it)
                        }; return xml1
                    }
            outputFile.withWriter { outWriter ->
                XmlUtil.serialize(outXml, outWriter)
                outWriter.close()
//                LOGGER.info("Merged manifest for $srcSet.name created in: $manifestFile.path")
            }
        }
    }
}