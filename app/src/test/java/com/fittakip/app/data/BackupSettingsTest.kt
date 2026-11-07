package com.fittakip.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupSettingsTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun programAndGymSurviveBackupRoundTrip() {
        val store = SettingsStore(ctx)
        store.update {
            it.copy(
                weekPlan = "Upper A;OFF;Lower;OFF;Upper B;OFF;OFF", planMode = "ROTATION",
                unavailable = setOf("Hack Squat"), userName = "Adnan", autoBackupUri = "content://cihaza-ozel"
            )
        }
        val saved = Backup.exportPrefs(ctx)
        assertFalse("cihaza özel ayar yedeğe girmemeli", saved.has("autoBackupUri"))

        // telefon sıfırlandı
        ctx.getSharedPreferences(SettingsStore.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        Backup.importPrefs(ctx, saved)
        val st = SettingsStore(ctx).state.value

        assertEquals(listOf("Upper A", "OFF", "Lower", "OFF", "Upper B", "OFF", "OFF"), st.plan)
        assertEquals("ROTATION", st.planMode)
        assertEquals(setOf("Hack Squat"), st.unavailable)
        assertEquals("Adnan", st.userName)
    }
}
