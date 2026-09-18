package com.communityos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flats",
    indices = [
        Index(value = ["communityId"]),
        Index(value = ["communityId", "flatNumber"], unique = true)
    ]
)
data class FlatEntity(
    @PrimaryKey
    val id: String,
    val communityId: String,
    val block: String,
    val flatNumber: String,
    val floor: Int
)
