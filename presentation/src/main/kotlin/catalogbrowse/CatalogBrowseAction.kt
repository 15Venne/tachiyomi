/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.catalogbrowse

import tachiyomi.core.util.replaceFirst
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangasPage
import tachiyomi.ui.catalogbrowse.Action.ErrorDelivered
import tachiyomi.ui.catalogbrowse.Action.LoadMore
import tachiyomi.ui.catalogbrowse.Action.Loading
import tachiyomi.ui.catalogbrowse.Action.LoadingError
import tachiyomi.ui.catalogbrowse.Action.MangaInitialized
import tachiyomi.ui.catalogbrowse.Action.PageReceived
import tachiyomi.ui.catalogbrowse.Action.QueryModeUpdated
import tachiyomi.ui.catalogbrowse.Action.SwapDisplayMode

/**
 * List of actions that can be used to request and mutate the view state.
 *
 * [SwapDisplayMode] is used to change the layout manager to a grid or a list.
 * [LoadMore] is used to request the next page of the catalog's current query.
 * [SetListing] is used to set a new query on the catalog with the given listing.
 * [SetFilters] is used to set a new query on the catalog with the given filters.
 * [ErrorDelivered] is used to notify the presenter that the UI has received the error.
 *
 * [QueryModeUpdated] sets a new query mode. It's the result of applying a [SetListing] or
 *   [SetFilters] action.
 * [PageReceived] adds a page received from the catalog to the current list.
 * [DisplayModeUpdated] sets the new display mode. It's the result of applying a [SwapDisplayMode]
 *   action.
 * [MangaInitialized] replaces the initialized manga with the non-initialized on the current list.
 * [Loading] sets the loading state, and also sets an empty list of manga if it's the first page.
 * [LoadingError] sets the error that can occur when the requested page fails to load.
 */
sealed class Action {

  object LoadMore : Action()

  data class ToggleFavorite(val manga: Manga) : Action()

  sealed class SetSearchMode : Action() {
    data class Listing(val index: Int) : SetSearchMode()
    data class Filters(val filters: List<FilterWrapper<*>>) : SetSearchMode()
  }

  data class QueryModeUpdated(val mode: QueryMode) : Action() {
    override fun reduce(state: ViewState) =
      state.copy(
        queryMode = mode,
        mangas = emptyList(),
        currentPage = 0,
        hasMorePages = true,
        isLoading = false
      )
  }

  data class PageReceived(val page: MangasPage) : Action() {
    override fun reduce(state: ViewState) =
      state.copy(
        mangas = state.mangas + page.mangas,
        isLoading = false,
        currentPage = page.number,
        hasMorePages = page.hasNextPage
      )
  }

  object SwapDisplayMode : Action() {
    override fun reduce(state: ViewState) =
      state.copy(isGridMode = !state.isGridMode)
  }

  data class MangaInitialized(val manga: Manga) : Action() {
    override fun reduce(state: ViewState) =
      state.copy(mangas = state.mangas
        .replaceFirst({ it.id == manga.id }, manga)
      )
  }

  data class Loading(val isLoading: Boolean, val page: Int) : Action() {
    override fun reduce(state: ViewState) =
      state.copy(isLoading = isLoading)
  }

  data class LoadingError(val error: Throwable?) : Action() {
    override fun reduce(state: ViewState) =
      state.copy(error = error, isLoading = false)
  }

  object ErrorDelivered : Action() {
    override fun reduce(state: ViewState) =
      state.copy(error = null)
  }

  open fun reduce(state: ViewState) = state

}
