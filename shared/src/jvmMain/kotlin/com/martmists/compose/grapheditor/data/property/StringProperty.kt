package com.martmists.compose.grapheditor.data.property

import kotlinx.serialization.Serializable

@Serializable
data class StringProperty(
    override val name: String,
    override val default: String = "",
) : PropertyDefinition<String>
