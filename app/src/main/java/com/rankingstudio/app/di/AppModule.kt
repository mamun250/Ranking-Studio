package com.rankingstudio.app.di

import android.content.Context
import androidx.room.Room
import com.rankingstudio.app.data.local.AppDatabase
import com.rankingstudio.app.data.local.ProjectDao
import com.rankingstudio.app.data.remote.TikTokApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ranking_studio.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    @Singleton
    fun provideTikTokApiService(): TikTokApiService {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // Android Emulator localhost bridge or default server
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TikTokApiService::class.java)
    }
}
