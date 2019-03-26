/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.category

import tachiyomi.domain.category.model.Category

data class ViewState(
  val categories: List<Category> = emptyList(),
  val selectedCategories: Set<Long> = emptySet(),
  val error: Throwable? = null
)
