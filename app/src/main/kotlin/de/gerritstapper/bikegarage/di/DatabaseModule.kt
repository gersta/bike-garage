package de.gerritstapper.bikegarage.di

import android.content.Context
import androidx.room.Room
import de.gerritstapper.bikegarage.data.local.AppDatabase
import de.gerritstapper.bikegarage.data.local.dao.BikeDao
import de.gerritstapper.bikegarage.data.local.dao.PartDao
import de.gerritstapper.bikegarage.data.local.dao.ShopDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bike-garage.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideBikeDao(database: AppDatabase): BikeDao = database.bikeDao()

    @Provides
    fun providePartDao(database: AppDatabase): PartDao = database.partDao()

    @Provides
    fun provideShopDao(database: AppDatabase): ShopDao = database.shopDao()
}
