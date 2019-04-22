/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.library

import com.freeletics.coredux.SideEffect
import com.freeletics.coredux.createStore
import com.jakewharton.rxrelay2.BehaviorRelay
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.domain.library.interactor.GetLibraryCategory
import tachiyomi.domain.library.interactor.GetUserCategories
import tachiyomi.domain.library.interactor.SetCategoriesForMangas
import tachiyomi.domain.library.interactor.SetCategoryFilters
import tachiyomi.domain.library.interactor.SetCategorySorting
import tachiyomi.domain.library.interactor.SetCategoryUseOwnFilters
import tachiyomi.domain.library.interactor.UpdateLibraryCategory
import tachiyomi.domain.library.model.Category
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.prefs.LibraryPreferences
import tachiyomi.ui.presenter.BasePresenter
import tachiyomi.ui.presenter.EmptySideEffect
import tachiyomi.ui.presenter.FlowSwitchSideEffect
import javax.inject.Inject

class LibraryPresenter @Inject constructor(
  private val getUserCategories: GetUserCategories,
  private val getLibraryCategory: GetLibraryCategory,
  private val setCategoriesForMangas: SetCategoriesForMangas,
  private val libraryPreferences: LibraryPreferences,
  private val setCategoryFilters: SetCategoryFilters,
  private val setCategorySorting: SetCategorySorting,
  private val setCategoryUseOwnFilters: SetCategoryUseOwnFilters,
  private val updateLibraryCategory: UpdateLibraryCategory
) : BasePresenter() {

  val state = BehaviorRelay.create<ViewState>()
  private val lastSortPreference = libraryPreferences.lastSorting()

  private val filtersPreference = libraryPreferences.filters()

  private val lastUsedCategoryPreference = libraryPreferences.lastUsedCategory()

  private val store = scope.createStore(
    name = "Library presenter",
    initialState = getInitialViewState(),
    sideEffects = getSideEffects(),
    logSinks = getLogSinks(),
    reducer = { state, action -> action.reduce(state) }
  )

  init {
    store.subscribeToChangedStateUpdatesInMain { state.accept(it) }
    store.dispatch(Action.Init)
  }

  private fun getInitialViewState(): ViewState {
    val lastSort = lastSortPreference.get()
    return ViewState(
      categories = emptyList(),
      library = emptyList(),
      filters = emptyList(),
      sort = lastSort
    )
  }

  private fun getSideEffects(): List<SideEffect<ViewState, Action>> {
    val sideEffects = mutableListOf<SideEffect<ViewState, Action>>()

    sideEffects += FlowSwitchSideEffect("Subscribe user categories") f@{ _, action ->
      if (action !is Action.Init) return@f null

      suspend {
        getUserCategories.subscribe(true).flatMapConcat { categories ->
          val lastCategoryId = lastUsedCategoryPreference.get()
          val category = categories.find { it.id == lastCategoryId } ?: categories.firstOrNull()

          flowOf(Action.CategoriesUpdate(categories), Action.SetSelectedCategory(category))
        }
      }
    }

    var subscribedCategory: Category? = null
    sideEffects += FlowSwitchSideEffect("Subscribe selected category") f@{ stateFn, action ->
      if (action !is Action.SetSelectedCategory) return@f null
      val category = stateFn().selectedCategory
      if (subscribedCategory == category) return@f null
      subscribedCategory = category

      suspend {
        if (category == null) {
          flowOf(Action.LibraryUpdate(emptyList()))
        } else {
          lastUsedCategoryPreference.set(category.id)
          val sorting = if (category.useOwnFilters) category.sort else lastSortPreference.get()
          val filters = if (category.useOwnFilters) category.filters else filtersPreference.get()

          getLibraryCategory.subscribe(category.id, sorting).map { Action.LibraryUpdate(it) }
        }
      }
    }

    sideEffects += FlowSwitchSideEffect("Update selected category") f@{ stateFn, action ->
      if (action !is Action.UpdateCategory) return@f null
      val categoryId = stateFn().selectedCategory?.id ?: return@f null

      suspend {
        GlobalScope.launch {
          updateLibraryCategory.execute(categoryId).awaitWork()
        }

        flow {
          emit(Action.ShowUpdatingCategory(true))
          delay(1000)
          emit(Action.ShowUpdatingCategory(false))
        }
      }
    }

    sideEffects += EmptySideEffect("Update filters and sorting") f@{ stateFn, action ->
      when (action) {
        is Action.SetFilters -> suspend {
          stateFn().selectedCategory?.let { setCategoryFilters.await(it, action.filters) }
        }
        is Action.SetSorting -> suspend {
          stateFn().selectedCategory?.let { setCategorySorting.await(it, action.sort) }
        }
        Action.ToggleGlobalFilters -> suspend {
          stateFn().selectedCategory?.let { setCategoryUseOwnFilters.await(it, !it.useOwnFilters) }
        }
        else -> null
      }
    }

    return sideEffects
  }

  fun setSelectedCategory(position: Int) {
    val category = state.value?.categories?.getOrNull(position) ?: return
    store.dispatch(Action.SetSelectedCategory(category))
  }

  fun updateSelectedCategory() {
    store.dispatch(Action.UpdateCategory)
  }

  fun toggleMangaSelection(manga: LibraryManga) {
    store.dispatch(Action.ToggleSelection(manga))
  }

  fun unselectMangas() {
    store.dispatch(Action.UnselectMangas)
  }

  fun setCategoriesForMangas(categoryIds: Collection<Long>, mangaIds: Collection<Long>) {
    scope.launch {
      val result = setCategoriesForMangas.await(categoryIds, mangaIds)
      if (result is SetCategoriesForMangas.Result.Success) {
        unselectMangas()
      }
    }
  }

  fun getCategories(): List<Category> {
    return state.value?.categories.orEmpty()
  }

}
