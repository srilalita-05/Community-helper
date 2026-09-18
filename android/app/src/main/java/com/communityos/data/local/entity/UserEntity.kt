package com.communityos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.communityos.models.User
import com.communityos.models.UserRole

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["phoneNumber"], unique = true),
        Index(value = ["communityId"]),
        Index(value = ["flatId"])
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val phoneNumber: String,
    val name: String,
    val email: String? = null,
    val role: UserRole = UserRole.RESIDENT,
    val communityId: String? = null,
    val flatId: String? = null,
    val isApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): User = User(
        id = id,
        phoneNumber = phoneNumber,
        name = name,
        email = email,
        role = role,
        communityId = communityId,
        flatId = flatId,
        isApproved = isApproved,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            phoneNumber = user.phoneNumber,
            name = user.name,
            email = user.email,
            role = user.role,
            communityId = user.communityId,
            flatId = user.flatId,
            isApproved = user.isApproved,
            createdAt = user.createdAt
        )
    }
}
