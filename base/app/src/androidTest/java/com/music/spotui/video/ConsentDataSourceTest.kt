package com.music.spotui.video

import android.net.Uri
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ConsentDataSourceTest {
    private val spec = DataSpec(Uri.parse("https://example.com/owned-video"))

    @Test fun revokedConsentBlocksReopeningAnAlreadyCreatedSource() {
        var allowed = true
        val source = ConsentDataSource.Factory(DataSource.Factory { ByteArrayDataSource(byteArrayOf(7, 8)) }) { allowed }.createDataSource()
        assertEquals(2L, source.open(spec))
        source.close()
        allowed = false
        assertThrows(IOException::class.java) { source.open(spec) }
        source.close()
        // An explicit new grant permits the same cached source to be used again.
        allowed = true
        assertEquals(2L, source.open(spec))
        source.close()
    }

    @Test fun revokeAllowsAnAlreadyStartedReadButBlocksTheNextRangeOpen() {
        var allowed = true
        val source = ConsentDataSource.Factory(DataSource.Factory { ByteArrayDataSource(byteArrayOf(7, 8)) }) { allowed }.createDataSource()
        source.open(spec)
        allowed = false
        val bytes = ByteArray(2)
        assertEquals(2, source.read(bytes, 0, 2))
        assertArrayEquals(byteArrayOf(7, 8), bytes)
        source.close()
        assertThrows(IOException::class.java) { source.open(spec.subrange(1)) }
        source.close()
    }

    @Test fun sourceCreatedWithoutConsentCanOpenOnlyAfterExplicitGrant() {
        var allowed = false
        val source = ConsentDataSource.Factory(DataSource.Factory { ByteArrayDataSource(byteArrayOf(7)) }) { allowed }.createDataSource()
        assertThrows(IOException::class.java) { source.open(spec) }
        source.close()
        allowed = true
        assertEquals(1L, source.open(spec))
        source.close()
    }
}
