package com.martmists.compose.grapheditor.data.property

import kotlinx.serialization.Serializable

@Serializable
data class BoolProperty(
    override val name: String,
    override val default: Boolean = false,
) : PropertyDefinition<Boolean>
