package com.communityos.marketplace.repository

import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.dao.MarketplaceDao
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.entity.MarketplaceListingEntity
import com.communityos.data.local.entity.UserEntity
import com.communityos.data.local.session.SessionManager
import com.communityos.marketplace.model.ListingStatus
import com.communityos.marketplace.model.MarketplaceCategory
import com.communityos.marketplace.model.MarketplaceListing
import com.communityos.marketplace.model.MarketplaceListingDetail
import com.communityos.models.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val communityDao: CommunityDao,
    private val flatDao: FlatDao,
    private val marketplaceDao: MarketplaceDao
) : MarketplaceRepository {

    private suspend fun getAuthenticatedResident(): Result<UserEntity> {
        val session = sessionManager.getSession()
            ?: return Result.failure(IllegalStateException("No active session found"))
        val user = userDao.getUserById(session.userId)
            ?: return Result.failure(IllegalStateException("User not found"))
        if (user.role != UserRole.RESIDENT) {
            return Result.failure(SecurityException("Unauthorized: Only residents have access to marketplace"))
        }
        if (user.communityId == null) {
            return Result.failure(IllegalStateException("Resident is not associated with a community"))
        }
        return Result.success(user)
    }

    override suspend fun getCommunityListings(category: MarketplaceCategory?): Result<List<MarketplaceListing>> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()
        val communityId = resident.communityId!!

        val entities = if (category == null || category == MarketplaceCategory.ALL) {
            marketplaceDao.getActiveListingsForCommunity(communityId)
        } else {
            marketplaceDao.getActiveListingsByCategoryForCommunity(communityId, category)
        }
        return Result.success(entities.map { it.toDomain() })
    }

    override fun observeCommunityListings(category: MarketplaceCategory?): Flow<List<MarketplaceListing>> = flow {
        val session = sessionManager.getSession() ?: return@flow
        val user = userDao.getUserById(session.userId) ?: return@flow
        if (user.role != UserRole.RESIDENT || user.communityId == null) return@flow
        val communityId = user.communityId

        val flow = if (category == null || category == MarketplaceCategory.ALL) {
            marketplaceDao.observeActiveListingsForCommunity(communityId)
        } else {
            marketplaceDao.observeActiveListingsByCategoryForCommunity(communityId, category)
        }
        emitAll(flow.map { list -> list.map { it.toDomain() } })
    }

    override suspend fun getMyListings(): Result<List<MarketplaceListing>> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()
        val entities = marketplaceDao.getListingsForResident(resident.id)
        return Result.success(entities.map { it.toDomain() })
    }

    override fun observeMyListings(): Flow<List<MarketplaceListing>> = flow {
        val session = sessionManager.getSession() ?: return@flow
        val user = userDao.getUserById(session.userId) ?: return@flow
        if (user.role != UserRole.RESIDENT) return@flow

        emitAll(marketplaceDao.observeListingsForResident(user.id).map { list -> list.map { it.toDomain() } })
    }

    override suspend fun getListingDetails(listingId: String): Result<MarketplaceListingDetail> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val currentResident = residentResult.getOrThrow()
        val communityId = currentResident.communityId!!

        val entity = marketplaceDao.getListingByIdAndCommunity(listingId, communityId)
            ?: return Result.failure(NoSuchElementException("Listing not found or does not belong to your community"))

        val seller = userDao.getUserById(entity.residentId)
        val flat = seller?.flatId?.let { flatDao.getFlatById(it) }

        val detail = MarketplaceListingDetail(
            listing = entity.toDomain(),
            sellerName = seller?.name ?: "Resident",
            flatNumber = flat?.flatNumber,
            block = flat?.block,
            contactPhone = entity.contactPhone,
            isOwner = (entity.residentId == currentResident.id)
        )
        return Result.success(detail)
    }

    override suspend fun createListing(
        title: String,
        description: String,
        category: MarketplaceCategory,
        price: Double,
        contactPhone: String
    ): Result<MarketplaceListing> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val resident = residentResult.getOrThrow()

        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanPhone = contactPhone.trim()

        if (cleanTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Title cannot be empty"))
        }
        if (cleanDesc.isBlank()) {
            return Result.failure(IllegalArgumentException("Description cannot be empty"))
        }
        if (price <= 0.0) {
            return Result.failure(IllegalArgumentException("Price must be greater than 0"))
        }
        val phoneRegex = Regex("^[6-9]\\d{9}$")
        if (cleanPhone.isBlank() || !cleanPhone.matches(phoneRegex)) {
            return Result.failure(IllegalArgumentException("Enter a valid 10-digit mobile number"))
        }

        val entity = MarketplaceListingEntity(
            id = UUID.randomUUID().toString(),
            residentId = resident.id,
            communityId = resident.communityId!!,
            title = cleanTitle,
            description = cleanDesc,
            category = category,
            price = price,
            contactPhone = cleanPhone,
            status = ListingStatus.ACTIVE,
            imageUri = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null
        )

        marketplaceDao.insertListing(entity)
        return Result.success(entity.toDomain())
    }

    override suspend fun updateListing(
        listingId: String,
        title: String,
        description: String,
        category: MarketplaceCategory,
        price: Double,
        contactPhone: String,
        status: ListingStatus
    ): Result<MarketplaceListing> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val currentResident = residentResult.getOrThrow()

        val existing = marketplaceDao.getListingByIdAndCommunity(listingId, currentResident.communityId!!)
            ?: return Result.failure(NoSuchElementException("Listing not found"))

        if (existing.residentId != currentResident.id) {
            return Result.failure(SecurityException("Unauthorized: You do not own this listing"))
        }

        // State transition enforcement: only ACTIVE listings can be edited
        if (existing.status != ListingStatus.ACTIVE) {
            return Result.failure(IllegalStateException("Cannot edit a ${existing.status} listing"))
        }

        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanPhone = contactPhone.trim()

        if (cleanTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Title cannot be empty"))
        }
        if (cleanDesc.isBlank()) {
            return Result.failure(IllegalArgumentException("Description cannot be empty"))
        }
        if (price <= 0.0) {
            return Result.failure(IllegalArgumentException("Price must be greater than 0"))
        }
        val phoneRegex = Regex("^[6-9]\\d{9}$")
        if (cleanPhone.isBlank() || !cleanPhone.matches(phoneRegex)) {
            return Result.failure(IllegalArgumentException("Enter a valid 10-digit mobile number"))
        }

        val updated = existing.copy(
            title = cleanTitle,
            description = cleanDesc,
            category = category,
            price = price,
            contactPhone = cleanPhone,
            status = status,
            updatedAt = System.currentTimeMillis()
        )

        marketplaceDao.updateListing(updated)
        return Result.success(updated.toDomain())
    }

    override suspend fun markListingAsSold(listingId: String): Result<MarketplaceListing> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val currentResident = residentResult.getOrThrow()

        val existing = marketplaceDao.getListingByIdAndCommunity(listingId, currentResident.communityId!!)
            ?: return Result.failure(NoSuchElementException("Listing not found"))

        if (existing.residentId != currentResident.id) {
            return Result.failure(SecurityException("Unauthorized: You do not own this listing"))
        }
        if (existing.status != ListingStatus.ACTIVE) {
            return Result.failure(IllegalStateException("Only active listings can be marked as sold"))
        }

        val updated = existing.copy(
            status = ListingStatus.SOLD,
            updatedAt = System.currentTimeMillis()
        )
        marketplaceDao.updateListing(updated)
        return Result.success(updated.toDomain())
    }

    override suspend fun cancelListing(listingId: String): Result<MarketplaceListing> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val currentResident = residentResult.getOrThrow()

        val existing = marketplaceDao.getListingByIdAndCommunity(listingId, currentResident.communityId!!)
            ?: return Result.failure(NoSuchElementException("Listing not found"))

        if (existing.residentId != currentResident.id) {
            return Result.failure(SecurityException("Unauthorized: You do not own this listing"))
        }
        if (existing.status != ListingStatus.ACTIVE) {
            return Result.failure(IllegalStateException("Only active listings can be cancelled"))
        }

        val updated = existing.copy(
            status = ListingStatus.CANCELLED,
            updatedAt = System.currentTimeMillis()
        )
        marketplaceDao.updateListing(updated)
        return Result.success(updated.toDomain())
    }

    override suspend fun deleteListing(listingId: String): Result<Unit> {
        val residentResult = getAuthenticatedResident()
        if (residentResult.isFailure) {
            return Result.failure(residentResult.exceptionOrNull()!!)
        }
        val currentResident = residentResult.getOrThrow()

        val existing = marketplaceDao.getListingByIdAndCommunity(listingId, currentResident.communityId!!)
            ?: return Result.failure(NoSuchElementException("Listing not found"))

        if (existing.residentId != currentResident.id) {
            return Result.failure(SecurityException("Unauthorized: You do not own this listing"))
        }

        marketplaceDao.deleteListing(existing)
        return Result.success(Unit)
    }
}
