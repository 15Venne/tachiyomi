/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.domain.library.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.library.model.Category
import tachiyomi.domain.library.model.CategoryUpdate
import tachiyomi.domain.library.model.CategoryWithCount

interface CategoryRepository {

  fun subscribeAll(): Flow<List<Category>>

  fun subscribeWithCount(): Flow<List<CategoryWithCount>>

  fun subscribeForManga(mangaId: Long): Flow<List<Category>>

  fun findAll(): List<Category>

  fun find(categoryId: Long): Category?

  fun findForManga(mangaId: Long): List<Category>

  fun save(category: Category)

  fun save(categories: Collection<Category>)

  fun savePartial(update: CategoryUpdate)

  fun savePartial(updates: Collection<CategoryUpdate>)

  fun delete(categoryId: Long)

  fun delete(categoryIds: Collection<Long>)

}
