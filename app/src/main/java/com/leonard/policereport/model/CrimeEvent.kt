package com.leonard.policereport.model

data class CrimeEvent(
    val category: String?,
    val context: String,
    val id: Int,
    val location: Location,
    val location_subtype: String,
    val location_type: String,
    val month: String,
    val persistent_id: String
)