package com.communityos.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.communityos.data.local.di.DatabaseModule
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    @Test
    fun migration_2_to_3_createsComplaintsTableAndPreservesExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_2_3.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v2 tables: users, communities, flats, notices
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `users` (
                            `id` TEXT NOT NULL,
                            `phoneNumber` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `email` TEXT,
                            `role` TEXT NOT NULL,
                            `communityId` TEXT,
                            `flatId` TEXT,
                            `isApproved` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `communities` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `address` TEXT NOT NULL,
                            `city` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `flats` (
                            `id` TEXT NOT NULL,
                            `communityId` TEXT NOT NULL,
                            `block` TEXT NOT NULL,
                            `flatNumber` TEXT NOT NULL,
                            `floor` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `notices` (
                            `id` TEXT NOT NULL,
                            `communityId` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        var db = helper.writableDatabase

        // Insert pre-migration records
        db.execSQL("INSERT INTO users VALUES ('u1', '9876543210', 'Resident 1', 'res1@example.com', 'RESIDENT', 'c1', 'f1', 1, 1000)")
        db.execSQL("INSERT INTO communities VALUES ('c1', 'Orchard Heights', 'Greenfield', 'Bengaluru')")
        db.execSQL("INSERT INTO flats VALUES ('f1', 'c1', 'Block B', 'B-304', 3)")
        db.execSQL("INSERT INTO notices VALUES ('n1', 'c1', 'Notice 1', 'Content 1', 1000, NULL)")

        // Execute MIGRATION_2_3
        DatabaseModule.MIGRATION_2_3.migrate(db)

        // Verify pre-migration data is intact
        val userCursor = db.query("SELECT * FROM users WHERE id = 'u1'")
        assertTrue(userCursor.moveToFirst())
        assertEquals("Resident 1", userCursor.getString(userCursor.getColumnIndexOrThrow("name")))
        userCursor.close()

        val commCursor = db.query("SELECT * FROM communities WHERE id = 'c1'")
        assertTrue(commCursor.moveToFirst())
        assertEquals("Orchard Heights", commCursor.getString(commCursor.getColumnIndexOrThrow("name")))
        commCursor.close()

        val flatCursor = db.query("SELECT * FROM flats WHERE id = 'f1'")
        assertTrue(flatCursor.moveToFirst())
        assertEquals("B-304", flatCursor.getString(flatCursor.getColumnIndexOrThrow("flatNumber")))
        flatCursor.close()

        val noticeCursor = db.query("SELECT * FROM notices WHERE id = 'n1'")
        assertTrue(noticeCursor.moveToFirst())
        assertEquals("Notice 1", noticeCursor.getString(noticeCursor.getColumnIndexOrThrow("title")))
        noticeCursor.close()

        // Verify complaints table exists and accepts records with null updatedAt
        db.execSQL("INSERT INTO complaints VALUES ('comp_1', 'u1', 'c1', 'f1', 'Water', 'Balcony leak', 'SUBMITTED', 2000, NULL)")
        val compCursor = db.query("SELECT * FROM complaints WHERE id = 'comp_1'")
        assertTrue(compCursor.moveToFirst())
        assertEquals("Water", compCursor.getString(compCursor.getColumnIndexOrThrow("category")))
        assertEquals("Balcony leak", compCursor.getString(compCursor.getColumnIndexOrThrow("description")))
        assertEquals("SUBMITTED", compCursor.getString(compCursor.getColumnIndexOrThrow("status")))
        assertTrue(compCursor.isNull(compCursor.getColumnIndexOrThrow("updatedAt")))
        compCursor.close()

        // Explicitly verify PRAGMA table_info to confirm column nullability
        val pragmaCursor = db.query("PRAGMA table_info(complaints)")
        var foundUpdatedAt = false
        var foundCreatedAt = false
        while (pragmaCursor.moveToNext()) {
            val colName = pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name"))
            val notNull = pragmaCursor.getInt(pragmaCursor.getColumnIndexOrThrow("notnull"))
            if (colName == "updatedAt") {
                foundUpdatedAt = true
                assertEquals("updatedAt must be nullable (notnull == 0)", 0, notNull)
            }
            if (colName == "createdAt") {
                foundCreatedAt = true
                assertEquals("createdAt must NOT be nullable (notnull == 1)", 1, notNull)
            }
        }
        pragmaCursor.close()
        assertTrue("updatedAt column must exist in complaints table", foundUpdatedAt)
        assertTrue("createdAt column must exist in complaints table", foundCreatedAt)

        db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration_3_to_4_createsVisitorsTableAndPreservesExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_3_4.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v3 tables: users, communities, flats, notices, complaints
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `users` (
                            `id` TEXT NOT NULL,
                            `phoneNumber` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `email` TEXT,
                            `role` TEXT NOT NULL,
                            `communityId` TEXT,
                            `flatId` TEXT,
                            `isApproved` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `communities` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `address` TEXT NOT NULL,
                            `city` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `flats` (
                            `id` TEXT NOT NULL,
                            `communityId` TEXT NOT NULL,
                            `block` TEXT NOT NULL,
                            `flatNumber` TEXT NOT NULL,
                            `floor` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `notices` (
                            `id` TEXT NOT NULL,
                            `communityId` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `complaints` (
                            `id` TEXT NOT NULL,
                            `residentId` TEXT NOT NULL,
                            `communityId` TEXT NOT NULL,
                            `flatId` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        var db = helper.writableDatabase

        // Insert pre-migration records
        db.execSQL("INSERT INTO users VALUES ('u1', '9876543210', 'Resident 1', 'res1@example.com', 'RESIDENT', 'c1', 'f1', 1, 1000)")
        db.execSQL("INSERT INTO communities VALUES ('c1', 'Orchard Heights', 'Greenfield', 'Bengaluru')")
        db.execSQL("INSERT INTO flats VALUES ('f1', 'c1', 'Block B', 'B-304', 3)")
        db.execSQL("INSERT INTO notices VALUES ('n1', 'c1', 'Notice 1', 'Content 1', 1000, NULL)")
        db.execSQL("INSERT INTO complaints VALUES ('comp_1', 'u1', 'c1', 'f1', 'Water', 'Balcony leak', 'SUBMITTED', 1000, NULL)")

        // Execute MIGRATION_3_4
        DatabaseModule.MIGRATION_3_4.migrate(db)

        // Verify pre-migration data remains intact
        val userCursor = db.query("SELECT * FROM users WHERE id = 'u1'")
        assertTrue(userCursor.moveToFirst())
        assertEquals("Resident 1", userCursor.getString(userCursor.getColumnIndexOrThrow("name")))
        userCursor.close()

        val commCursor = db.query("SELECT * FROM communities WHERE id = 'c1'")
        assertTrue(commCursor.moveToFirst())
        assertEquals("Orchard Heights", commCursor.getString(commCursor.getColumnIndexOrThrow("name")))
        commCursor.close()

        val flatCursor = db.query("SELECT * FROM flats WHERE id = 'f1'")
        assertTrue(flatCursor.moveToFirst())
        assertEquals("B-304", flatCursor.getString(flatCursor.getColumnIndexOrThrow("flatNumber")))
        flatCursor.close()

        val noticeCursor = db.query("SELECT * FROM notices WHERE id = 'n1'")
        assertTrue(noticeCursor.moveToFirst())
        assertEquals("Notice 1", noticeCursor.getString(noticeCursor.getColumnIndexOrThrow("title")))
        noticeCursor.close()

        val compCursor = db.query("SELECT * FROM complaints WHERE id = 'comp_1'")
        assertTrue(compCursor.moveToFirst())
        assertEquals("Water", compCursor.getString(compCursor.getColumnIndexOrThrow("category")))
        compCursor.close()

        // Verify visitors table exists and accepts records with null optional fields
        db.execSQL(
            """
            INSERT INTO visitors VALUES (
                'vis_1', 'u1', 'c1', 'f1', 'John Doe', '9888877777', 'Guest', 'KA-05-MJ-1234',
                1700000000000, 'PRE_APPROVED', NULL, NULL, NULL, NULL, 1699999000000, NULL
            )
            """.trimIndent()
        )

        val visCursor = db.query("SELECT * FROM visitors WHERE id = 'vis_1'")
        assertTrue(visCursor.moveToFirst())
        assertEquals("John Doe", visCursor.getString(visCursor.getColumnIndexOrThrow("name")))
        assertEquals("9888877777", visCursor.getString(visCursor.getColumnIndexOrThrow("phoneNumber")))
        assertEquals("Guest", visCursor.getString(visCursor.getColumnIndexOrThrow("purpose")))
        assertEquals("KA-05-MJ-1234", visCursor.getString(visCursor.getColumnIndexOrThrow("vehicleNumber")))
        assertEquals("PRE_APPROVED", visCursor.getString(visCursor.getColumnIndexOrThrow("status")))
        assertTrue(visCursor.isNull(visCursor.getColumnIndexOrThrow("photoUri")))
        assertTrue(visCursor.isNull(visCursor.getColumnIndexOrThrow("checkInTime")))
        assertTrue(visCursor.isNull(visCursor.getColumnIndexOrThrow("checkOutTime")))
        assertTrue(visCursor.isNull(visCursor.getColumnIndexOrThrow("verifiedBySecurityId")))
        assertTrue(visCursor.isNull(visCursor.getColumnIndexOrThrow("updatedAt")))
        visCursor.close()

        // Explicitly verify PRAGMA table_info to confirm column nullability
        val pragmaCursor = db.query("PRAGMA table_info(visitors)")
        var foundVerifiedBy = false
        var foundCheckIn = false
        var foundCheckOut = false
        var foundUpdatedAt = false
        var foundCreatedAt = false
        while (pragmaCursor.moveToNext()) {
            val colName = pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name"))
            val notNull = pragmaCursor.getInt(pragmaCursor.getColumnIndexOrThrow("notnull"))
            when (colName) {
                "verifiedBySecurityId" -> {
                    foundVerifiedBy = true
                    assertEquals(0, notNull)
                }
                "checkInTime" -> {
                    foundCheckIn = true
                    assertEquals(0, notNull)
                }
                "checkOutTime" -> {
                    foundCheckOut = true
                    assertEquals(0, notNull)
                }
                "updatedAt" -> {
                    foundUpdatedAt = true
                    assertEquals(0, notNull)
                }
                "createdAt" -> {
                    foundCreatedAt = true
                    assertEquals(1, notNull)
                }
            }
        }
        pragmaCursor.close()
        assertTrue(foundVerifiedBy)
        assertTrue(foundCheckIn)
        assertTrue(foundCheckOut)
        assertTrue(foundUpdatedAt)
        assertTrue(foundCreatedAt)

        // Verify required indexes exist
        val indexCursor = db.query("PRAGMA index_list(visitors)")
        val indexNames = mutableSetOf<String>()
        while (indexCursor.moveToNext()) {
            indexNames.add(indexCursor.getString(indexCursor.getColumnIndexOrThrow("name")))
        }
        indexCursor.close()

        assertTrue(indexNames.contains("index_visitors_residentId"))
        assertTrue(indexNames.contains("index_visitors_communityId"))
        assertTrue(indexNames.contains("index_visitors_flatId"))
        assertTrue(indexNames.contains("index_visitors_status"))
        assertTrue(indexNames.contains("index_visitors_communityId_status"))

        db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration_4_to_5_createsMarketplaceListingsTableAndPreservesExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_4_5.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v4 tables: users, communities, flats, notices, complaints, visitors
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `users` (
                            `id` TEXT NOT NULL,
                            `phoneNumber` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `email` TEXT,
                            `role` TEXT NOT NULL,
                            `communityId` TEXT,
                            `flatId` TEXT,
                            `isApproved` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `communities` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `address` TEXT NOT NULL,
                            `city` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `flats` (
                            `id` TEXT NOT NULL,
                            `communityId` TEXT NOT NULL,
                            `block` TEXT NOT NULL,
                            `flatNumber` TEXT NOT NULL,
                            `floor` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `notices` (
                            `id` TEXT NOT NULL,
                            `communityId` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `complaints` (
                            `id` TEXT NOT NULL,
                            `residentId` TEXT NOT NULL,
                            `communityId` TEXT NOT NULL,
                            `flatId` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `visitors` (
                            `id` TEXT NOT NULL,
                            `residentId` TEXT NOT NULL,
                            `communityId` TEXT NOT NULL,
                            `flatId` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `phoneNumber` TEXT NOT NULL,
                            `purpose` TEXT NOT NULL,
                            `vehicleNumber` TEXT,
                            `scheduledArrivalDate` INTEGER NOT NULL,
                            `status` TEXT NOT NULL,
                            `photoUri` TEXT,
                            `checkInTime` INTEGER,
                            `checkOutTime` INTEGER,
                            `verifiedBySecurityId` TEXT,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        var db = helper.writableDatabase

        // Insert pre-migration records across all 6 existing tables
        db.execSQL("INSERT INTO users VALUES ('u1', '9876543210', 'Resident 1', 'res1@example.com', 'RESIDENT', 'c1', 'f1', 1, 1000)")
        db.execSQL("INSERT INTO communities VALUES ('c1', 'Greenwood', '123 Main St', 'City')")
        db.execSQL("INSERT INTO flats VALUES ('f1', 'c1', 'Block A', 'A-101', 1)")
        db.execSQL("INSERT INTO notices VALUES ('n1', 'c1', 'Notice 1', 'Content 1', 1000, NULL)")
        db.execSQL("INSERT INTO complaints VALUES ('comp_1', 'u1', 'c1', 'f1', 'Water', 'Desc', 'SUBMITTED', 1000, NULL)")
        db.execSQL("INSERT INTO visitors VALUES ('vis_1', 'u1', 'c1', 'f1', 'John Doe', '9888877777', 'Guest', NULL, 1700000000, 'PRE_APPROVED', NULL, NULL, NULL, NULL, 1000, NULL)")

        // Execute MIGRATION_4_5
        DatabaseModule.MIGRATION_4_5.migrate(db)

        // Verify pre-migration data remains completely intact
        val userCursor = db.query("SELECT * FROM users WHERE id = 'u1'")
        assertTrue(userCursor.moveToFirst())
        assertEquals("Resident 1", userCursor.getString(userCursor.getColumnIndexOrThrow("name")))
        userCursor.close()

        val commCursor = db.query("SELECT * FROM communities WHERE id = 'c1'")
        assertTrue(commCursor.moveToFirst())
        assertEquals("Greenwood", commCursor.getString(commCursor.getColumnIndexOrThrow("name")))
        commCursor.close()

        val flatCursor = db.query("SELECT * FROM flats WHERE id = 'f1'")
        assertTrue(flatCursor.moveToFirst())
        assertEquals("A-101", flatCursor.getString(flatCursor.getColumnIndexOrThrow("flatNumber")))
        flatCursor.close()

        val noticeCursor = db.query("SELECT * FROM notices WHERE id = 'n1'")
        assertTrue(noticeCursor.moveToFirst())
        assertEquals("Notice 1", noticeCursor.getString(noticeCursor.getColumnIndexOrThrow("title")))
        noticeCursor.close()

        val compCursor = db.query("SELECT * FROM complaints WHERE id = 'comp_1'")
        assertTrue(compCursor.moveToFirst())
        assertEquals("Water", compCursor.getString(compCursor.getColumnIndexOrThrow("category")))
        compCursor.close()

        val visCursor = db.query("SELECT * FROM visitors WHERE id = 'vis_1'")
        assertTrue(visCursor.moveToFirst())
        assertEquals("John Doe", visCursor.getString(visCursor.getColumnIndexOrThrow("name")))
        visCursor.close()

        // Verify marketplace_listings table exists and accepts records with null imageUri and updatedAt
        db.execSQL(
            """
            INSERT INTO marketplace_listings VALUES (
                'list_1', 'u1', 'c1', 'Dining Table', 'Teak wood table', 'FURNITURE', 4500.0,
                '9876543210', 'ACTIVE', NULL, 1700000000, NULL
            )
            """.trimIndent()
        )

        val listCursor = db.query("SELECT * FROM marketplace_listings WHERE id = 'list_1'")
        assertTrue(listCursor.moveToFirst())
        assertEquals("Dining Table", listCursor.getString(listCursor.getColumnIndexOrThrow("title")))
        assertEquals("Teak wood table", listCursor.getString(listCursor.getColumnIndexOrThrow("description")))
        assertEquals("FURNITURE", listCursor.getString(listCursor.getColumnIndexOrThrow("category")))
        assertEquals(4500.0, listCursor.getDouble(listCursor.getColumnIndexOrThrow("price")), 0.01)
        assertEquals("9876543210", listCursor.getString(listCursor.getColumnIndexOrThrow("contactPhone")))
        assertEquals("ACTIVE", listCursor.getString(listCursor.getColumnIndexOrThrow("status")))
        assertTrue(listCursor.isNull(listCursor.getColumnIndexOrThrow("imageUri")))
        assertEquals(1700000000L, listCursor.getLong(listCursor.getColumnIndexOrThrow("createdAt")))
        assertTrue(listCursor.isNull(listCursor.getColumnIndexOrThrow("updatedAt")))
        listCursor.close()

        // Verify column types and nullabilities via PRAGMA table_info
        val pragmaCursor = db.query("PRAGMA table_info(marketplace_listings)")
        var foundId = false
        var foundResidentId = false
        var foundCommunityId = false
        var foundTitle = false
        var foundDescription = false
        var foundCategory = false
        var foundPrice = false
        var foundContactPhone = false
        var foundStatus = false
        var foundImageUri = false
        var foundCreatedAt = false
        var foundUpdatedAt = false

        while (pragmaCursor.moveToNext()) {
            val colName = pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name"))
            val notNull = pragmaCursor.getInt(pragmaCursor.getColumnIndexOrThrow("notnull"))
            val colType = pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("type")).uppercase()
            when (colName) {
                "id" -> {
                    foundId = true
                    assertEquals(1, notNull)
                    assertEquals("TEXT", colType)
                }
                "residentId" -> {
                    foundResidentId = true
                    assertEquals(1, notNull)
                }
                "communityId" -> {
                    foundCommunityId = true
                    assertEquals(1, notNull)
                }
                "title" -> {
                    foundTitle = true
                    assertEquals(1, notNull)
                }
                "description" -> {
                    foundDescription = true
                    assertEquals(1, notNull)
                }
                "category" -> {
                    foundCategory = true
                    assertEquals(1, notNull)
                }
                "price" -> {
                    foundPrice = true
                    assertEquals(1, notNull)
                    assertEquals("REAL", colType)
                }
                "contactPhone" -> {
                    foundContactPhone = true
                    assertEquals(1, notNull)
                }
                "status" -> {
                    foundStatus = true
                    assertEquals(1, notNull)
                }
                "imageUri" -> {
                    foundImageUri = true
                    assertEquals(0, notNull)
                }
                "createdAt" -> {
                    foundCreatedAt = true
                    assertEquals(1, notNull)
                    assertEquals("INTEGER", colType)
                }
                "updatedAt" -> {
                    foundUpdatedAt = true
                    assertEquals(0, notNull)
                }
            }
        }
        pragmaCursor.close()

        assertTrue(foundId)
        assertTrue(foundResidentId)
        assertTrue(foundCommunityId)
        assertTrue(foundTitle)
        assertTrue(foundDescription)
        assertTrue(foundCategory)
        assertTrue(foundPrice)
        assertTrue(foundContactPhone)
        assertTrue(foundStatus)
        assertTrue(foundImageUri)
        assertTrue(foundCreatedAt)
        assertTrue(foundUpdatedAt)

        // Verify required indexes exist
        val indexCursor = db.query("PRAGMA index_list(marketplace_listings)")
        val indexNames = mutableSetOf<String>()
        while (indexCursor.moveToNext()) {
            indexNames.add(indexCursor.getString(indexCursor.getColumnIndexOrThrow("name")))
        }
        indexCursor.close()

        assertTrue(indexNames.contains("index_marketplace_listings_residentId"))
        assertTrue(indexNames.contains("index_marketplace_listings_communityId"))
        assertTrue(indexNames.contains("index_marketplace_listings_communityId_status"))
        assertTrue(indexNames.contains("index_marketplace_listings_category"))

        // Verify foreign keys exist
        val fkCursor = db.query("PRAGMA foreign_key_list(marketplace_listings)")
        val fkTables = mutableSetOf<String>()
        while (fkCursor.moveToNext()) {
            fkTables.add(fkCursor.getString(fkCursor.getColumnIndexOrThrow("table")))
        }
        fkCursor.close()

        assertTrue(fkTables.contains("users"))
        assertTrue(fkTables.contains("communities"))

        db.close()
        context.deleteDatabase(dbName)
    }
}

