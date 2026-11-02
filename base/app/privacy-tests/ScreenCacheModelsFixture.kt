package com.music.spotui.data.entity

import kotlinx.serialization.builtins.serializer

// Only primitive serializers are needed to exercise cache persistence boundaries.
object HomeFeedModel { fun serializer() = String.serializer() }
object LibraryEntry { fun serializer() = String.serializer() }
object SongsModel { fun serializer() = String.serializer() }
object ContextPageModel { fun serializer() = String.serializer() }
