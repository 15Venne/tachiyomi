/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.domain.library.interactor

import io.reactivex.Single
import tachiyomi.domain.library.model.Category
import tachiyomi.domain.library.prefs.LibraryPreferences
import tachiyomi.domain.library.repository.CategoryRepository
import tachiyomi.domain.library.updater.LibraryUpdater
import javax.inject.Inject

class DeleteCategories @Inject constructor(
  private val categoryRepository: CategoryRepository,
  private val libraryPreferences: LibraryPreferences,
  private val libraryUpdater: LibraryUpdater
) {

  fun interact(categoryId: Long): Single<Result> {
    if (categoryId <= 0) {
      return Single.just(Result.SystemCategoryUndeletableError)
    }

    return categoryRepository.delete(categoryId)
      .toSingle<Result> { Result.Success }
      .doOnSuccess {
        if (libraryPreferences.defaultCategory().get() == categoryId) {
          libraryPreferences.defaultCategory().delete()
        }
      }
      .onErrorReturn(Result::InternalError)
  }

  fun interact(categoryIds: Collection<Long>): Single<Result> {
    val safeCategoryIds = categoryIds.filter { it > 0 }
    if (safeCategoryIds.isEmpty()) {
      return Single.just(Result.Success)
    }

    return categoryRepository.delete(safeCategoryIds)
      .toSingle<Result> { Result.Success }
      .doOnSuccess {
        if (libraryPreferences.defaultCategory().get() in safeCategoryIds) {
          libraryPreferences.defaultCategory().delete()
        }
        for (id in safeCategoryIds) {
          libraryUpdater.unscheduleAll(id)
        }
      }
      .onErrorReturn(Result::InternalError)
  }

  fun interact(category: Category): Single<Result> {
    return interact(category.id)
  }

  sealed class Result {
    object Success : Result()
    object SystemCategoryUndeletableError : Result()
    data class InternalError(val error: Throwable) : Result()
  }

}
