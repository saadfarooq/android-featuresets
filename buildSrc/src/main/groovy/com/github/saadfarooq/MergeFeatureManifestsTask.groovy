package com.github.saadfarooq

import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.utils.ILogger
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class MergeFeatureManifestsTask extends DefaultTask implements ILogger {
    @Input File mainManifest
    @Input List<File> featureManifests
    @Input File outputFile
    @Input Logger logger

    @TaskAction
    def mergeManifests() {
        def document = ManifestMerger2.newMerger(mainManifest, this, ManifestMerger2.MergeType.APPLICATION)
                .withFeatures(ManifestMerger2.Invoker.Feature.NO_PLACEHOLDER_REPLACEMENT)
                .addFlavorAndBuildTypeManifests(*featureManifests)
                .merge()
                .getMergedDocument(MergingReport.MergedManifestKind.MERGED)
        outputFile.parentFile.mkdirs()
        info("Creating file: $outputFile")
        outputFile.createNewFile()
        outputFile.write(document)
    }

    @Override
    void error(Throwable throwable, String s, Object... objects) {
        logger.error(s, throwable, objects)
    }

    @Override
    void warning(String s, Object... objects) {
        logger.warn(s, objects)
    }

    @Override
    void info(String s, Object... objects) {
        logger.info(s, objects)
    }

    @Override
    void verbose(String s, Object... objects) {
        logger.trace(s, objects)
    }
}