package com.communityos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.communityos.notices.model.Notice

@Entity(
    tableName = "notices",
    indices = [
        Index(value = ["communityId"])
    ]
)
data class NoticeEntity(
    @PrimaryKey
    val id: String,
    val communityId: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long? = null
) {
    fun toDomain(): Notice = Notice(
        id = id,
        communityId = communityId,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(notice: Notice): NoticeEntity = NoticeEntity(
            id = notice.id,
            communityId = notice.communityId,
            title = notice.title,
            content = notice.content,
            createdAt = notice.createdAt,
            updatedAt = notice.updatedAt
        )
    }
}
