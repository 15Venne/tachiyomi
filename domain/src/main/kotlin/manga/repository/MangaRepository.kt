/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.domain.manga.repository

import io.reactivex.Observable
import tachiyomi.core.stdlib.Optional
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate

interface MangaRepository {

  fun subscribe(mangaId: Long): Observable<Optional<Manga>>

  fun subscribe(key: String, sourceId: Long): Observable<Optional<Manga>>

  fun find(mangaId: Long): Manga?

  fun find(key: String, sourceId: Long): Manga?

  fun save(manga: Manga): Long?

  fun savePartial(update: MangaUpdate)

  fun deleteNonFavorite()

}
