package com.communityos.notices.model

data class Notice(
    val id: String,
    val communityId: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long? = null
)
