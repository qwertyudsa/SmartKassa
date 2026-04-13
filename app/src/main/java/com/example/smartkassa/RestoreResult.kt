package com.example.smartkassa

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val backupData: BackupData? = null,
    val restoredCount: Map<String, Int> = emptyMap()
)