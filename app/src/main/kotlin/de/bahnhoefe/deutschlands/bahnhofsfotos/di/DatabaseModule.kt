package de.bahnhoefe.deutschlands.bahnhofsfotos.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun provideDbAdapter(@ApplicationContext context: Context): DbAdapter {
        val dbAdapter = DbAdapter(context)
        dbAdapter.open()
        return dbAdapter
    }

}