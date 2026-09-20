package com.communityos.visitors.model

data class SecurityVisitorDetail(
    val visitor: Visitor,
    val residentName: String,
    val residentPhone: String,
    val flatNumber: String,
    val block: String,
    val communityName: String
)
