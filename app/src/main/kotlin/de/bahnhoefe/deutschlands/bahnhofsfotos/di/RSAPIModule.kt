package de.bahnhoefe.deutschlands.bahnhofsfotos.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RSAPIModule {

    @Singleton
    @Provides
    fun provideRSAPIClient(
        @ApplicationContext context: Context,
        preferencesService: PreferencesService,
    ): RSAPIClient {
        return RSAPIClient(
            preferencesService, context.getString(R.string.rsapiClientId), context.getString(
                R.string.rsapiRedirectScheme
            ) + "://" + context.getString(R.string.rsapiRedirectHost)
        )
    }

}