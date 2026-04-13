package com.example.smartkassa

// Класс для информации о файле бэкапа
data class BackupFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val readableSize: String
)