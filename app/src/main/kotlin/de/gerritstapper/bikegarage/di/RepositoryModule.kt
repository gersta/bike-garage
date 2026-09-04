package de.gerritstapper.bikegarage.di

import de.gerritstapper.bikegarage.data.image.ImageStorage
import de.gerritstapper.bikegarage.data.image.InternalImageStorage
import de.gerritstapper.bikegarage.data.repository.BikeRepositoryImpl
import de.gerritstapper.bikegarage.data.repository.PartRepositoryImpl
import de.gerritstapper.bikegarage.data.repository.ShopRepositoryImpl
import de.gerritstapper.bikegarage.domain.repository.BikeRepository
import de.gerritstapper.bikegarage.domain.repository.PartRepository
import de.gerritstapper.bikegarage.domain.repository.ShopRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindBikeRepository(implementation: BikeRepositoryImpl): BikeRepository

    @Binds
    abstract fun bindPartRepository(implementation: PartRepositoryImpl): PartRepository

    @Binds
    abstract fun bindShopRepository(implementation: ShopRepositoryImpl): ShopRepository

    @Binds
    abstract fun bindImageStorage(implementation: InternalImageStorage): ImageStorage
}
