package ir.hesabino.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hesabino.app.data.local.db.HesabinoDatabase
import ir.hesabino.app.engine.parser.ParserRegistry
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): HesabinoDatabase =
        Room.databaseBuilder(context, HesabinoDatabase::class.java, HesabinoDatabase.NAME)
            .addMigrations(*HesabinoDatabase.MIGRATIONS)
            .build()

    @Provides
    @Singleton
    fun parserRegistry(): ParserRegistry = ParserRegistry()
}
