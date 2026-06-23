package com.martmists.compose.grapheditor.data.property

import kotlinx.serialization.Serializable

@Serializable
data class FloatProperty(
    override val name: String,
    override val default: Float = 0f,
    val min: Float = Float.NEGATIVE_INFINITY,
    val max: Float = Float.POSITIVE_INFINITY,
) : PropertyDefinition<Float> {
    override fun validate(value: Float) = value in min..max
}
