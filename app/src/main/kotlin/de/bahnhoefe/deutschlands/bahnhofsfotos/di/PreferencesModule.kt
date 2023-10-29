package de.bahnhoefe.deutschlands.bahnhofsfotos.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class PreferencesModule {

    @Singleton
    @Provides
    fun providePreferencesService(@ApplicationContext context: Context): PreferencesService {
        return PreferencesService(context)
    }

}