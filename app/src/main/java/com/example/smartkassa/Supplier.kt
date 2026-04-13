package com.example.smartkassa

data class Supplier(
    val id: String,
    val name: String,
    val contactPerson: String? = null,
    val phone: String? = null,
    val email: String? = null
)