/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.app.initializers

import android.app.Application
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tachiyomi.core.util.CoroutineDispatchers
import javax.inject.Inject

class EmojiCompatInitializer @Inject constructor(
  app: Application,
  dispatchers: CoroutineDispatchers
) {

  init {
    val config = BundledEmojiCompatConfig(app)
      .setReplaceAll(true)
      .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL)

    EmojiCompat.init(config)

    GlobalScope.launch(dispatchers.computation) {
      EmojiCompat.get().load()
    }
  }

}
