package com.martmists.compose.grapheditor.compose.internal

import kotlin.time.Duration.Companion.milliseconds

// TODO: Allow styling these?

const val NODE_TITLE_HEIGHT = 30f
const val NODE_MIN_WIDTH = 180f
const val NODE_TYPE_LEFT_PADDING = 10f
const val NODE_TYPE_RIGHT_PADDING = 4f
const val NODE_SELECTED_HIGHLIGHT_SIZE = 2f
val NODE_CLICK_TIMEOUT = 100.milliseconds

const val PORT_PADDING = 7.5f
const val PORT_SIZE = 8f
const val PORT_INSET = 0f
const val PORT_CUTOUT = 3f
const val PORT_OFFSET = 10f

const val CONNECTION_WIDTH = 4f
const val CONNECTION_HIGHLIGHT_WIDTH = 8f
const val CONNECTION_PORT_SIZE = PORT_SIZE * 0.6f

// For selection intersection checking
const val CONNECTION_BEZIER_MAX_DEPTH = 10
