/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.domain.library.interactor

import io.reactivex.Observable
import tachiyomi.domain.library.model.Category
import tachiyomi.domain.library.repository.CategoryRepository
import javax.inject.Inject

class SubscribeCategories @Inject constructor(
  private val categoryRepository: CategoryRepository
) {

  fun interact(): Observable<List<Category>> {
    return categoryRepository.subscribe()
  }
}
