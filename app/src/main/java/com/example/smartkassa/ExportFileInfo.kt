package com.example.smartkassa

data class ExportFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val readableSize: String,
    val extension: String
)