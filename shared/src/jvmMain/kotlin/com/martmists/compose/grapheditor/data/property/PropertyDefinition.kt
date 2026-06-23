package com.martmists.compose.grapheditor.data.property

import kotlinx.serialization.Serializable

@Serializable
sealed interface PropertyDefinition<T : Any> {
    val name: String
    val default: T

    fun validate(value: T): Boolean = true
}
