/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.data.library.sql

import android.database.Cursor
import com.pushtorefresh.storio3.sqlite.StorIOSQLite
import com.pushtorefresh.storio3.sqlite.operations.get.DefaultGetResolver
import tachiyomi.data.manga.sql.MangaTable

internal object FavoriteSourceIdsGetResolver : DefaultGetResolver<Long>() {

  const val query = """
    SELECT ${MangaTable.COL_SOURCE}
    FROM ${MangaTable.LIBRARY}
    GROUP BY ${MangaTable.COL_SOURCE}
    ORDER BY COUNT(${MangaTable.COL_SOURCE}) DESC
  """

  override fun mapFromCursor(storIOSQLite: StorIOSQLite, cursor: Cursor): Long {
    return cursor.getLong(0)
  }

}
