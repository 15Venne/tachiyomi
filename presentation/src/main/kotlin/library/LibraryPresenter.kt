/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.library

import android.util.Log
import com.freeletics.rxredux.StateAccessor
import com.freeletics.rxredux.reduxStore
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.ofType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tachiyomi.core.rx.RxSchedulers
import tachiyomi.core.rx.addTo
import tachiyomi.domain.library.interactor.SetCategoriesForMangas
import tachiyomi.domain.library.interactor.SubscribeLibraryCategory
import tachiyomi.domain.library.interactor.SubscribeUserCategories
import tachiyomi.domain.library.interactor.UpdateLibraryCategory
import tachiyomi.domain.library.model.Category
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.prefs.LibraryPreferences
import tachiyomi.ui.presenter.BasePresenter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LibraryPresenter @Inject constructor(
  private val subscribeUserCategories: SubscribeUserCategories,
  private val subscribeLibraryCategory: SubscribeLibraryCategory,
  private val setCategoriesForMangas: SetCategoriesForMangas,
  private val libraryPreferences: LibraryPreferences,
  private val updateLibraryCategory: UpdateLibraryCategory,
  private val schedulers: RxSchedulers
) : BasePresenter() {

  val state = BehaviorRelay.create<ViewState>()

  private val actions = PublishRelay.create<Action>()

  private val lastSortPreference = libraryPreferences.lastSorting()

  private val filtersPreference = libraryPreferences.filters()

  private val lastUsedCategoryPreference = libraryPreferences.lastUsedCategory()

  init {
    actions
      .observeOn(schedulers.io)
      .reduxStore(
        initialState = getInitialViewState(),
        sideEffects = listOf(
          ::categoriesSideEffect,
          ::setSelectedCategorySideEffect,
          ::setFiltersSideEffect,
          ::setSortSideEffect,
          ::updateCategorySideEffect
        ),
        reducer = { state, action -> action.reduce(state) }
      )
      .distinctUntilChanged()
      .logOnNext()
      .observeOn(schedulers.main)
      .subscribe(state::accept)
      .addTo(disposables)
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

  @Suppress("unused_parameter")
  private fun categoriesSideEffect(
    actions: Observable<Action>,
    stateFn: StateAccessor<ViewState>
  ): Observable<Action> {
    val shared = subscribeUserCategories.interact(true)
      .share()

    return Observable.merge(
      shared.map { Action.CategoriesUpdate(it) },
      shared.take(1).map {
        val lastCategoryId = lastUsedCategoryPreference.get()
        val state = stateFn()
        val category = state.categories.find { it.id == lastCategoryId }
          ?: state.categories.firstOrNull()

        Action.SetSelectedCategory(category)
      }
    )
  }

  private fun setSelectedCategorySideEffect(
    actions: Observable<Action>,
    stateFn: StateAccessor<ViewState>
  ): Observable<Action.LibraryUpdate> {
    return actions.map { stateFn() }
      .distinctUntilChanged(Function<ViewState, Long?> { it.selectedCategoryId })
      .switchMap { state ->
        val selectedId = state.selectedCategoryId
        if (selectedId == null) {
          Observable.just(Action.LibraryUpdate(emptyList()))
        } else {
          lastUsedCategoryPreference.set(selectedId)

          subscribeLibraryCategory.interact(selectedId)
            .subscribeOn(schedulers.io)
            .map(Action::LibraryUpdate)
        }
      }
  }

//  @Suppress("unused_parameter")
//  private fun librarySideEffect(
//    actions: Observable<Action>,
//    stateFn: StateAccessor<ViewState>
//  ): Observable<Action.LibraryUpdate> {
//    val state = stateFn()
//    //getLibrary.setFilters(state.filters)
//    getLibrary.setSorting(state.sort)
//
//    return actions.ofType<Action.SetFilters>()
//      .startWith(Action.SetFilters(state.filters)) // TODO check threading
//      .switchMap { getLibrary.interact(it.filters).onBackpressureLatest().toObservable() }
//      .subscribeOn(schedulers.io)
//      .map(Action::LibraryUpdate)
//  }

  @Suppress("unused_parameter")
  private fun setFiltersSideEffect(
    actions: Observable<Action>,
    stateFn: StateAccessor<ViewState>
  ): Observable<Action> {
    return actions.ofType<Action.SetFilters>()
      .flatMap { action ->
        filtersPreference.set(action.filters)
        //getLibrary.setFilters(action.filters)
        Observable.empty<Action>()
      }
  }

  @Suppress("unused_parameter")
  private fun setSortSideEffect(
    actions: Observable<Action>,
    stateFn: StateAccessor<ViewState>
  ): Observable<Action> {
    return actions.ofType<Action.SetSorting>()
      .flatMap { action ->
        lastSortPreference.set(action.sort)
        //getLibrary.setSorting(action.sort)
        Observable.empty<Action>()
      }
  }

  @Suppress("unused_parameter")
  private fun updateCategorySideEffect(
    actions: Observable<Action>,
    stateFn: StateAccessor<ViewState>
  ): Observable<Action> {
    return actions.ofType<Action.UpdateCategory>()
      .flatMap<Action> { action ->
        if (action.loading) {
          val categoryId = stateFn().selectedCategoryId
          if (categoryId != null) {
            GlobalScope.launch {
              updateLibraryCategory.execute(categoryId).awaitWork()
              Log.d("P", "Job completed on ${Thread.currentThread()}")
            }

            Observable.timer(1, TimeUnit.SECONDS).map { Action.UpdateCategory(false) }

//            updateLibraryCategory.interact(categoryId)
//              .subscribeOn(schedulers.io)
//              .toObservable()
//              .flatMap { result ->
//                if (result is LibraryUpdater.QueueResult.Executing) {
//                  Observable.timer(1, TimeUnit.SECONDS)
//                    .map { Action.UpdateCategory(false) }
//                } else {
//                  Observable.just(Action.UpdateCategory(false))
//                }
//              }
          } else {
            Observable.just(Action.UpdateCategory(false))
          }
        } else {
          Observable.empty()
        }
      }
  }

  fun setSelectedCategory(position: Int) {
    val category = state.value?.categories?.getOrNull(position) ?: return
    actions.accept(Action.SetSelectedCategory(category))
  }

  fun toggleMangaSelection(manga: LibraryManga) {
    actions.accept(Action.ToggleSelection(manga))
  }

  fun unselectMangas() {
    actions.accept(Action.UnselectMangas)
  }

  fun setCategoriesForMangas(categoryIds: Collection<Long>, mangaIds: Collection<Long>) {
    setCategoriesForMangas.interact(categoryIds, mangaIds)
      .subscribeOn(schedulers.io)
      .doOnSuccess { result ->
        when (result) {
          SetCategoriesForMangas.Result.Success -> unselectMangas()
          is SetCategoriesForMangas.Result.InternalError -> {} // do nothing
        }
      }
      .subscribe()
  }

  fun getCategories(): List<Category> {
    return state.value?.categories.orEmpty()
  }

  fun updateSelectedCategory() {
    actions.accept(Action.UpdateCategory(true))
  }

}
