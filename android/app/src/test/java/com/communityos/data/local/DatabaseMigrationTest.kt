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
}
