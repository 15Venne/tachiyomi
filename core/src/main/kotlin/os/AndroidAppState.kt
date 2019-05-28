/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.core.os

import android.app.Application
import android.net.NetworkInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import timber.log.Timber
import timber.log.debug
import javax.inject.Inject

class AndroidAppState @Inject constructor(
  context: Application
) : AppState, LifecycleObserver {

  override var hasNetwork = false
    private set(value) {
      field = value
      networkChannel.offer(value)
    }

  override var isInForeground = false
    private set(value) {
      field = value
      foregroundChannel.offer(value)
    }

  private val foregroundChannel = ConflatedBroadcastChannel<Boolean>()
  override val foregroundRelay = foregroundChannel.asFlow()

  private val networkChannel = ConflatedBroadcastChannel<Boolean>()
  override val networkRelay = networkChannel.asFlow()

  init {
    ProcessLifecycleOwner.get().lifecycle.addObserver(this)

    @Suppress("CheckResult")
    ReactiveNetwork.observeNetworkConnectivity(context)
      .map { it.state() == NetworkInfo.State.CONNECTED }
      .distinctUntilChanged()
      .subscribe { hasNetwork = it }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  private fun setForeground() {
    Timber.debug { "Application now in foreground" }
    isInForeground = true
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  private fun setBackground() {
    Timber.debug { "Application went to background" }
    isInForeground = false
  }

}
