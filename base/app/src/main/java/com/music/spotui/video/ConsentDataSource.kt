package com.music.spotui.video

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException

/** Non-prompting check on every new request, including ranges from a cached MediaSource. */
internal class ConsentDataSource(
    private val upstream: DataSource,
    private val allowed: () -> Boolean,
) : DataSource by upstream {
    override fun open(dataSpec: DataSpec): Long {
        if (!allowed()) throw IOException("Optional video consent required")
        return upstream.open(dataSpec)
    }

    class Factory(private val upstream: DataSource.Factory, private val allowed: () -> Boolean) : DataSource.Factory {
        override fun createDataSource(): DataSource = ConsentDataSource(upstream.createDataSource(), allowed)
    }
}
