package com.communityos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "communities")
data class CommunityEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val totalBlocks: Int
)
