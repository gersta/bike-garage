package de.gerritstapper.bikegarage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.gerritstapper.bikegarage.data.local.dao.BikeDao
import de.gerritstapper.bikegarage.data.local.dao.PartDao
import de.gerritstapper.bikegarage.data.local.dao.ShopDao
import de.gerritstapper.bikegarage.data.local.entity.BikeEntity
import de.gerritstapper.bikegarage.data.local.entity.BikeImageEntity
import de.gerritstapper.bikegarage.data.local.entity.BikePartCrossRefEntity
import de.gerritstapper.bikegarage.data.local.entity.PartAttributeEntity
import de.gerritstapper.bikegarage.data.local.entity.PartEntity
import de.gerritstapper.bikegarage.data.local.entity.PartImageEntity
import de.gerritstapper.bikegarage.data.local.entity.PartShopLinkEntity
import de.gerritstapper.bikegarage.data.local.entity.ShopEntity

@Database(
    entities = [
        BikeEntity::class,
        BikeImageEntity::class,
        PartEntity::class,
        PartImageEntity::class,
        PartAttributeEntity::class,
        BikePartCrossRefEntity::class,
        ShopEntity::class,
        PartShopLinkEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
    abstract fun partDao(): PartDao
    abstract fun shopDao(): ShopDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE parts ADD COLUMN inventoryCount INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS part_images (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        partId INTEGER NOT NULL,
                        storagePath TEXT NOT NULL,
                        displayOrder INTEGER NOT NULL,
                        isThumbnail INTEGER NOT NULL,
                        FOREIGN KEY(partId) REFERENCES parts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_part_images_partId ON part_images(partId)")
            }
        }
    }
}
