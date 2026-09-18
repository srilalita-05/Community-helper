package com.communityos.data.local

import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Idempotent initializer for Phase 1 development and demonstration seed data.
 *
 * NOTE: This is exclusively a Phase 1 local development utility to provide immediate
 * test data for community selection and flat binding. It is NOT the production backend
 * synchronization strategy.
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val communityDao: CommunityDao,
    private val flatDao: FlatDao
) {

    suspend fun seedDemoDataIfEmpty() = withContext(Dispatchers.IO) {
        if (communityDao.getCount() == 0) {
            val communities = listOf(
                CommunityEntity(
                    id = "community_orchard_heights",
                    name = "Orchard Heights Apartments",
                    address = "42 Greenfield Boulevard",
                    city = "Bengaluru",
                    totalBlocks = 4
                ),
                CommunityEntity(
                    id = "community_palm_meadows",
                    name = "Palm Meadows Villa",
                    address = "10 Sunrise Avenue",
                    city = "Hyderabad",
                    totalBlocks = 2
                )
            )
            communityDao.insertCommunities(communities)
        }

        if (flatDao.getCount() == 0) {
            val flats = listOf(
                FlatEntity(
                    id = "flat_a101",
                    communityId = "community_orchard_heights",
                    block = "Block A",
                    flatNumber = "A-101",
                    floor = 1
                ),
                FlatEntity(
                    id = "flat_b304",
                    communityId = "community_orchard_heights",
                    block = "Block B",
                    flatNumber = "B-304",
                    floor = 3
                ),
                FlatEntity(
                    id = "flat_c502",
                    communityId = "community_orchard_heights",
                    block = "Block C",
                    flatNumber = "C-502",
                    floor = 5
                ),
                FlatEntity(
                    id = "flat_v101",
                    communityId = "community_palm_meadows",
                    block = "Villa 1",
                    flatNumber = "V-101",
                    floor = 1
                ),
                FlatEntity(
                    id = "flat_v102",
                    communityId = "community_palm_meadows",
                    block = "Villa 2",
                    flatNumber = "V-102",
                    floor = 1
                )
            )
            flatDao.insertFlats(flats)
        }
    }
}
