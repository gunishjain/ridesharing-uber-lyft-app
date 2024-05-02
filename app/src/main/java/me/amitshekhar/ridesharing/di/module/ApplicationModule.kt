package me.amitshekhar.ridesharing.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.amitshekhar.ridesharing.data.network.NetworkService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {

    @Provides
    @Singleton
    fun provideNetworkService(): NetworkService {
        return NetworkService()
    }

}