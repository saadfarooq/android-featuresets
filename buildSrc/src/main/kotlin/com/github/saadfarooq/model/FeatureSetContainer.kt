package com.github.saadfarooq.model

import java.util.*

class FeatureSetContainer(val name: String) {
    var features: Set<String> = HashSet()

    fun features(vararg args: String) {
        this.features = args.toHashSet()
    }

    override fun toString(): String {
        return "{ $name, $features }"
    }
}
