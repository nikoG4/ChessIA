package org.nko.chessia.models

import kotlinx.serialization.Serializable


@Serializable
data class AIProvider(
    val name: String,
    val endpoint: String,
    val method: String,
    val headers: Map<String, String>,
    val bodyTemplate: String,
    val extract: Extractor,
    var apiKey: String = ""
)