/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.withContext
import tachiyomi.core.util.CoroutineDispatchers
import tachiyomi.domain.manga.model.MangasPage
import tachiyomi.source.CatalogSource
import tachiyomi.source.model.Listing
import javax.inject.Inject

class ListMangaPageFromCatalogSource @Inject internal constructor(
  private val getOrAddMangaFromSource: GetOrAddMangaFromSource,
  private val dispatchers: CoroutineDispatchers
) {

  suspend fun await(source: CatalogSource, listing: Listing?, page: Int): MangasPage {
    return withContext(dispatchers.io) {
      val sourcePage = source.fetchMangaList(listing, page)
      val localPage = sourcePage.mangas.map { getOrAddMangaFromSource.await(it, source.id) }

      MangasPage(page, localPage, sourcePage.hasNextPage)
    }
  }

}
