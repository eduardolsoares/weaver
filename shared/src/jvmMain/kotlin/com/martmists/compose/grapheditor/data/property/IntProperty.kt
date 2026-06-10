package com.martmists.compose.grapheditor.data.property

import kotlinx.serialization.Serializable

@Serializable
data class IntProperty(
    override val name: String,
    override val default: Int = 0,
    val min: Int = Int.MIN_VALUE,
    val max: Int = Int.MAX_VALUE,
) : PropertyDefinition<Int> {
    override fun validate(value: Int) = value in min..max
}
