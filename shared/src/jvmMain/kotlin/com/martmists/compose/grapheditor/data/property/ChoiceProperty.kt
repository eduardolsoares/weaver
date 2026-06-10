package com.martmists.compose.grapheditor.data.property

import kotlinx.serialization.Serializable

@Serializable
data class ChoiceProperty(
    override val name: String,
    val choices: List<String>,
    override val default: String = choices.first(),
) : PropertyDefinition<String> {
    override fun validate(value: String) = value in choices
}
