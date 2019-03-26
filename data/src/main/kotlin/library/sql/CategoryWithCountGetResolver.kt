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
import tachiyomi.domain.library.model.Category
import tachiyomi.domain.library.model.CategoryWithCount

internal object CategoryWithCountGetResolver : DefaultGetResolver<CategoryWithCount>(),
  CategoryCursorMapper {

  private const val mangaCount = "manga_count"

  const val query = """
    SELECT ${CategoryTable.TABLE}.*, COUNT(${MangaCategoryTable.COL_MANGA_ID}) as $mangaCount
    FROM ${CategoryTable.TABLE}
    LEFT JOIN ${MangaCategoryTable.TABLE}
    ON ${CategoryTable.COL_ID} = ${MangaCategoryTable.COL_CATEGORY_ID}
    WHERE ${CategoryTable.COL_ID} > 0
    GROUP BY ${CategoryTable.COL_ID}
    UNION ALL
    SELECT *, (
      SELECT COUNT()
      FROM ${MangaTable.LIBRARY}
    )
    FROM ${CategoryTable.TABLE}
    WHERE ${CategoryTable.COL_ID} = ${Category.ALL_ID}
    UNION ALL
    SELECT *, (
      SELECT COUNT(${MangaTable.COL_ID})
      FROM ${MangaTable.LIBRARY}
      WHERE NOT EXISTS (
        SELECT ${MangaCategoryTable.COL_MANGA_ID}
        FROM ${MangaCategoryTable.TABLE}
        WHERE ${MangaTable.COL_ID} = ${MangaCategoryTable.COL_MANGA_ID}
      )
    )
    FROM ${CategoryTable.TABLE}
    WHERE ${CategoryTable.COL_ID} = ${Category.UNCATEGORIZED_ID}
    ORDER BY ${CategoryTable.COL_ORDER}
  """

  override fun mapFromCursor(storIOSQLite: StorIOSQLite, cursor: Cursor): CategoryWithCount {
    val category = mapCategory(cursor)
    val count = cursor.getInt(cursor.getColumnIndex(mangaCount))
    return CategoryWithCount(category, count)
  }

}
