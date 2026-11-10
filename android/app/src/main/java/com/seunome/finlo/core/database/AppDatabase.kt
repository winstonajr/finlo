package com.seunome.finlo.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seunome.finlo.core.database.dao.AccountDao
import com.seunome.finlo.core.database.dao.CategoryDao
import com.seunome.finlo.core.database.dao.GoalDao
import com.seunome.finlo.core.database.dao.RecurringRuleDao
import com.seunome.finlo.core.database.dao.SyncChangeDao
import com.seunome.finlo.core.database.dao.TransactionDao
import com.seunome.finlo.core.database.entity.AccountEntity
import com.seunome.finlo.core.database.entity.CategoryEntity
import com.seunome.finlo.core.database.entity.GoalEntity
import com.seunome.finlo.core.database.entity.RecurringRuleEntity
import com.seunome.finlo.core.database.entity.SyncChangeEntity
import com.seunome.finlo.core.database.entity.TransactionEntity
import com.seunome.finlo.core.util.UuidV7
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        GoalEntity::class,
        RecurringRuleEntity::class,
        SyncChangeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun syncChangeDao(): SyncChangeDao

    companion object {
        private const val DB_NAME = "finlo.db"

        fun buildDatabase(context: Context, passphrase: ByteArray? = null): AppDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration()
            .addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDefaultCategories(db)
                    }
                }
            })

            if (passphrase != null && passphrase.isNotEmpty()) {
                val factory = SupportFactory(passphrase)
                builder.openHelperFactory(factory)
            }

            return builder.build()
        }

        private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val defaultCategories = listOf(
                Triple("Alimentação", "restaurant", "#FF5722"),
                Triple("Transporte", "directions_car", "#2196F3"),
                Triple("Moradia", "home", "#9C27B0"),
                Triple("Saúde", "favorite", "#E91E63"),
                Triple("Lazer", "sports_esports", "#4CAF50"),
                Triple("Educação", "school", "#FF9800"),
                Triple("Salário", "attach_money", "#009688"),
                Triple("Investimentos", "trending_up", "#3F51B5"),
                Triple("Outros", "more_horiz", "#607D8B")
            )

            for ((name, icon, color) in defaultCategories) {
                val id = UuidV7.generate(now)
                db.execSQL(
                    "INSERT INTO categories (id, name, icon, colorHex, budgetCents, updatedAt, deletedAt, syncStatus) VALUES ('$id', '$name', '$icon', '$color', 0, $now, NULL, 'SYNCED')"
                )
            }
        }
    }
}
