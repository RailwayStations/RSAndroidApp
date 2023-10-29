package de.bahnhoefe.deutschlands.bahnhofsfotos.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RSAPIModule {

    @Singleton
    @Provides
    fun provideRSAPIClient(@ApplicationContext context: Context): RSAPIClient {
        val dbAdapter = DbAdapter(context)
        dbAdapter.open()
        throw NotImplementedError()
    }

}