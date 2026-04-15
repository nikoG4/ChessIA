package org.nko.chessia.models

import kotlinx.serialization.Serializable

@Serializable
data class Extractor(
    val type: String, // "regex" | "jsonpath"
    val pattern: String? = null,
    val path: String? = null
)