package com.github.saadfarooq

final class FeatureSetContainer {
    Set<String> features
    String name
    boolean encapsulateTests

    FeatureSetContainer(String name) {
        this.name = name
    }

    def features(Set<String> feats) {
        this.features = feats
    }

    def features(String... feats) {
        this.features = feats
    }

    def encapsulateTests(boolean value) {
        this.encapsulateTests = value
    }

    String getTaskName() {
        return "merge${name.capitalize()}FeatureSetManifests"
    }
}