package com.github.saadfarooq

class FeatureSetsExtension {
    List<String> debug = []
    List<String> main = []
    List<String> release = []

    @Override
    String toString() {
        return "$debug, $release, $main"
    }
}
