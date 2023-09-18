package com.simplemobiletools.commons.models

import androidx.annotation.Keep

@Keep
data class StorageLocation(
    val treeUri: String,
    val name: String
)
