package de.gerritstapper.bikegarage.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "part-migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `migration keeps existing parts readable with an empty inventory count`() = runBlocking {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
            createVersionOneSchema(database)
            database.execSQL(
                """
                INSERT INTO parts
                    (id, category, type, name, manufacturer, manufacturerNumber, description, createdAt)
                VALUES
                    (7, 'BRAKES', 'Belag', 'Bremsbelag', 'Shimano', 'B01S', 'Altbestand', 123)
                """.trimIndent(),
            )
            database.version = 1
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val part = database.partDao().observeWithRelations(7).first()?.part

        assertEquals("Bremsbelag", part?.name)
        assertEquals("Altbestand", part?.description)
        assertNull(part?.inventoryCount)
        database.close()
    }

    private fun createVersionOneSchema(database: android.database.sqlite.SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE bikes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nickname TEXT,
                brand TEXT NOT NULL,
                model TEXT NOT NULL,
                purchaseDate TEXT,
                shopId INTEGER,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(shopId) REFERENCES shops(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX index_bikes_shopId ON bikes(shopId)")
        database.execSQL(
            """
            CREATE TABLE bike_images (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bikeId INTEGER NOT NULL,
                storagePath TEXT NOT NULL,
                displayOrder INTEGER NOT NULL,
                isThumbnail INTEGER NOT NULL,
                FOREIGN KEY(bikeId) REFERENCES bikes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX index_bike_images_bikeId ON bike_images(bikeId)")
        database.execSQL(
            """
            CREATE TABLE parts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                category TEXT NOT NULL,
                type TEXT,
                name TEXT NOT NULL,
                manufacturer TEXT,
                manufacturerNumber TEXT,
                description TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX index_parts_category ON parts(category)")
        database.execSQL(
            """
            CREATE TABLE part_attributes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL,
                attributeKey TEXT NOT NULL,
                value TEXT NOT NULL,
                FOREIGN KEY(partId) REFERENCES parts(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE UNIQUE INDEX index_part_attributes_partId_attributeKey ON part_attributes(partId, attributeKey)",
        )
        database.execSQL(
            """
            CREATE TABLE bike_part_cross_refs (
                bikeId INTEGER NOT NULL,
                partId INTEGER NOT NULL,
                PRIMARY KEY(bikeId, partId),
                FOREIGN KEY(bikeId) REFERENCES bikes(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(partId) REFERENCES parts(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX index_bike_part_cross_refs_partId ON bike_part_cross_refs(partId)")
        database.execSQL(
            """
            CREATE TABLE shops (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                logoPath TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE part_shop_links (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL,
                shopId INTEGER NOT NULL,
                productUrl TEXT NOT NULL,
                FOREIGN KEY(partId) REFERENCES parts(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(shopId) REFERENCES shops(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX index_part_shop_links_partId ON part_shop_links(partId)")
        database.execSQL("CREATE INDEX index_part_shop_links_shopId ON part_shop_links(shopId)")
    }
}
