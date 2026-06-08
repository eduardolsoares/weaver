package com.martmists.compose.grapheditor.data

import kotlinx.serialization.Serializable

@Serializable
data class PortDefinition(
    val name: String,
    val kind: PortKind,
    val dataType: String = "any",
)
