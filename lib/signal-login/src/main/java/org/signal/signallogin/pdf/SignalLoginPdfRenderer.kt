/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.signallogin.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.signal.core.util.Result
import org.signal.core.util.logging.Log
import org.signal.signallogin.R
import org.signal.signallogin.fonts.MonoTypeface
import org.signal.signallogin.viewdetails.SignalLoginViewDetailsState
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.signal.core.ui.R as CoreUiR

/**
 * Renders the keys that make up a Signal Login into a single-page PDF, styled to match
 * [org.signal.signallogin.viewdetails.SignalLoginViewDetailsScreen]: the Signal logo up top, then each key
 * in a rounded block, with the recovery key broken into character groups.
 *
 * All dimensions are in PostScript points (1/72 inch), on an A4 page.
 */
object SignalLoginPdfRenderer {

  private val TAG = Log.tag(SignalLoginPdfRenderer::class)

  /** The file name to prefill in the system save dialog. */
  fun suggestedFileName(context: Context): String {
    return context.getString(R.string.SignalLoginViewDetailsScreen__signal_login_pdf)
  }

  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 56f

  private const val LOGO_WIDTH = 140f
  private const val LOGO_BOTTOM_SPACING = 28f

  private const val HEADER_TEXT_SIZE = 12f
  private const val HEADER_TOP_PADDING = 16f
  private const val HEADER_BOTTOM_PADDING = 12f

  private const val KEY_TEXT_SIZE = 13f
  private const val KEY_LINE_HEIGHT = 21f
  private const val KEY_LETTER_SPACING_EM = 0.08f

  private const val BLOCK_CORNER_RADIUS = 18f
  private const val BLOCK_HORIZONTAL_PADDING = 28f
  private const val BLOCK_VERTICAL_PADDING = 20f

  private const val GROUPS_PER_ROW = 4

  /** Light-theme colorSurface2 and onSurface, respectively. The PDF is always rendered as if in light theme. */
  private const val BLOCK_COLOR = 0xFFEDF0F6.toInt()
  private const val TEXT_COLOR = 0xFF1B1B1D.toInt()

  /**
   * Renders the credentials in [state] to a PDF and writes it to [uri].
   */
  suspend fun renderTo(context: Context, uri: Uri, state: SignalLoginViewDetailsState): Result<Unit, SignalLoginPdfError> {
    return withContext(Dispatchers.IO) {
      try {
        val bytes = render(context, state)
        val stream = context.contentResolver.openOutputStream(uri)
        if (stream == null) {
          Log.w(TAG, "Could not open an output stream for the chosen location.")
          Result.failure(SignalLoginPdfError.UnableToOpenDocument)
        } else {
          stream.use { it.write(bytes) }
          Result.success(Unit)
        }
      } catch (e: IOException) {
        Log.w(TAG, "Failed to write the Signal Login PDF.", e)
        Result.failure(SignalLoginPdfError.WriteFailed)
      } catch (e: SecurityException) {
        Log.w(TAG, "Not allowed to write the Signal Login PDF to the chosen location.", e)
        Result.failure(SignalLoginPdfError.NotAllowed)
      }
    }
  }

  fun render(context: Context, state: SignalLoginViewDetailsState): ByteArray {
    val document = PdfDocument()
    try {
      val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
      drawPage(context, page.canvas, state)
      document.finishPage(page)

      return ByteArrayOutputStream().use { stream ->
        document.writeTo(stream)
        stream.toByteArray()
      }
    } finally {
      document.close()
    }
  }

  private fun drawPage(context: Context, canvas: Canvas, state: SignalLoginViewDetailsState) {
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
      textSize = HEADER_TEXT_SIZE
      color = TEXT_COLOR
    }

    val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = MonoTypeface.typeface(context)
      textSize = KEY_TEXT_SIZE
      letterSpacing = KEY_LETTER_SPACING_EM
      color = TEXT_COLOR
    }

    var y = drawLogo(context, canvas, top = MARGIN) + LOGO_BOTTOM_SPACING

    y = drawSectionHeader(canvas, headerPaint, context.getString(R.string.SignalLoginViewDetailsScreen__account_key), y)
    y = drawKeyBlock(canvas, keyPaint, rows = listOf(listOf(state.accountKey)), top = y)

    y = drawSectionHeader(canvas, headerPaint, context.getString(R.string.SignalLoginViewDetailsScreen__recovery_key), y)
    drawKeyBlock(canvas, keyPaint, rows = state.recoveryKeyGroups.chunked(GROUPS_PER_ROW), top = y)
  }

  /** Draws the Signal logo centered at the top of the page, returning the y position of its bottom edge. */
  private fun drawLogo(context: Context, canvas: Canvas, top: Float): Float {
    val logo = requireNotNull(ContextCompat.getDrawable(context, CoreUiR.drawable.image_signal_logo_wordmark_light))
    val height = LOGO_WIDTH * logo.intrinsicHeight / logo.intrinsicWidth
    val left = (PAGE_WIDTH - LOGO_WIDTH) / 2f

    logo.setBounds(left.toInt(), top.toInt(), (left + LOGO_WIDTH).toInt(), (top + height).toInt())
    logo.draw(canvas)

    return top + height
  }

  /** Draws a section header above a key block, returning the y position content below it should start at. */
  private fun drawSectionHeader(canvas: Canvas, paint: Paint, text: String, top: Float): Float {
    val textTop = top + HEADER_TOP_PADDING
    canvas.drawText(text, MARGIN, textTop - paint.fontMetrics.ascent, paint)
    return textTop + paint.fontMetrics.let { it.descent - it.ascent } + HEADER_BOTTOM_PADDING
  }

  /**
   * Draws a rounded block containing rows of key text, returning the y position of the block's bottom edge.
   * Rows with a single entry are drawn left-aligned; rows with multiple groups are spaced evenly across the
   * block, mirroring how the screen lays out recovery key groups.
   */
  private fun drawKeyBlock(canvas: Canvas, paint: Paint, rows: List<List<String>>, top: Float): Float {
    val blockWidth = PAGE_WIDTH - 2 * MARGIN
    val blockHeight = 2 * BLOCK_VERTICAL_PADDING + rows.size * KEY_LINE_HEIGHT

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BLOCK_COLOR }
    canvas.drawRoundRect(RectF(MARGIN, top, MARGIN + blockWidth, top + blockHeight), BLOCK_CORNER_RADIUS, BLOCK_CORNER_RADIUS, backgroundPaint)

    val innerLeft = MARGIN + BLOCK_HORIZONTAL_PADDING
    val innerWidth = blockWidth - 2 * BLOCK_HORIZONTAL_PADDING
    val groupWidth = rows.flatten().maxOfOrNull { paint.measureText(it) } ?: 0f

    var lineTop = top + BLOCK_VERTICAL_PADDING
    for (row in rows) {
      val spacing = if (row.size > 1) (innerWidth - GROUPS_PER_ROW * groupWidth) / (GROUPS_PER_ROW - 1) else 0f
      val baseline = lineTop + (KEY_LINE_HEIGHT - (paint.fontMetrics.descent - paint.fontMetrics.ascent)) / 2f - paint.fontMetrics.ascent

      row.forEachIndexed { index, group ->
        canvas.drawText(group, innerLeft + index * (groupWidth + spacing), baseline, paint)
      }

      lineTop += KEY_LINE_HEIGHT
    }

    return top + blockHeight
  }
}

/**
 * The ways saving the login PDF can fail, each carrying the message to show the user.
 */
sealed class SignalLoginPdfError(@StringRes val userMessageRes: Int) {
  /** The system could not provide an output stream for the chosen location. */
  data object UnableToOpenDocument : SignalLoginPdfError(R.string.SignalLoginViewDetailsScreen__unable_to_save_pdf)

  /** Writing the rendered bytes failed. */
  data object WriteFailed : SignalLoginPdfError(R.string.SignalLoginViewDetailsScreen__unable_to_save_pdf)

  /** The document provider rejected the write, e.g. a permission grant that has since been revoked. */
  data object NotAllowed : SignalLoginPdfError(R.string.SignalLoginViewDetailsScreen__cant_save_pdf_to_the_selected_location)
}
