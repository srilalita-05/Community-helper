package com.communityos.data.local

import android.content.Context
import com.communityos.data.local.dao.CommunityDao
import com.communityos.data.local.dao.FlatDao
import com.communityos.data.local.entity.CommunityEntity
import com.communityos.data.local.entity.FlatEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Idempotent initializer for developer-managed master community seed data.
 *
 * Loads initial communities, blocks, and flats from assets/community_data.json
 * into Room persistence. Never wipes existing user or session data.
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val communityDao: CommunityDao,
    private val flatDao: FlatDao
) {

    suspend fun seedDemoDataIfEmpty() = withContext(Dispatchers.IO) {
        val jsonString = try {
            context.assets.open(COMMUNITY_DATA_ASSET).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            // Fallback for JVM/Robolectric test environments where assets are on disk
            val fileCandidates = listOf(
                File("src/main/assets/$COMMUNITY_DATA_ASSET"),
                File("app/src/main/assets/$COMMUNITY_DATA_ASSET"),
                File("../app/src/main/assets/$COMMUNITY_DATA_ASSET")
            )
            val fallbackFile = fileCandidates.firstOrNull { it.exists() }
            fallbackFile?.readText() ?: return@withContext
        }
        seedFromJson(jsonString)
    }

    suspend fun seedFromJson(jsonString: String) = withContext(Dispatchers.IO) {
        val jsonObject = JSONObject(jsonString)
        val communitiesArray = jsonObject.optJSONArray("communities") ?: return@withContext

        val communitiesToInsert = mutableListOf<CommunityEntity>()
        val flatsToInsert = mutableListOf<FlatEntity>()

        for (i in 0 until communitiesArray.length()) {
            val commObj = communitiesArray.getJSONObject(i)
            val communityId = commObj.getString("id")
            val communityName = commObj.getString("name")
            val address = commObj.optString("address", "")
            val city = commObj.optString("city", "")

            val blocksArray = commObj.optJSONArray("blocks")
            val totalBlocks = blocksArray?.length() ?: 0

            val communityEntity = CommunityEntity(
                id = communityId,
                name = communityName,
                address = address,
                city = city,
                totalBlocks = totalBlocks
            )
            communitiesToInsert.add(communityEntity)

            if (blocksArray != null) {
                for (b in 0 until blocksArray.length()) {
                    val blockObj = blocksArray.getJSONObject(b)
                    val blockId = blockObj.optString("id", "")
                    val blockName = blockObj.optString("name", blockId)
                    val flatsArray = blockObj.optJSONArray("flats")

                    if (flatsArray != null) {
                        for (f in 0 until flatsArray.length()) {
                            val flatObj = flatsArray.getJSONObject(f)
                            val flatNumber = flatObj.optString("number").ifEmpty {
                                flatObj.optString("flatNumber")
                            }
                            val flatId = flatObj.optString("id").ifEmpty {
                                "${communityId}_${blockId}_${flatNumber.replace("-", "").lowercase()}"
                            }
                            val floor = flatObj.optInt("floor", 1)

                            val flatEntity = FlatEntity(
                                id = flatId,
                                communityId = communityId,
                                block = blockName,
                                flatNumber = flatNumber,
                                floor = floor
                            )
                            flatsToInsert.add(flatEntity)
                        }
                    }
                }
            }
        }

        // Idempotent insertion:
        if (communityDao.getCount() == 0) {
            communityDao.insertCommunities(communitiesToInsert)
        } else {
            for (community in communitiesToInsert) {
                if (communityDao.getCommunityById(community.id) == null) {
                    communityDao.insertCommunity(community)
                }
            }
        }

        if (flatDao.getCount() == 0) {
            flatDao.insertFlats(flatsToInsert)
        } else {
            for (flat in flatsToInsert) {
                if (flatDao.getFlatByNumber(flat.communityId, flat.flatNumber) == null) {
                    flatDao.insertFlat(flat)
                }
            }
        }
    }

    companion object {
        const val COMMUNITY_DATA_ASSET = "community_data.json"
    }
}
