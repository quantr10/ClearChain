package com.clearchain.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.clearchain.app.R
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.PickupRequest
import java.io.File

/**
 * The pickup receipt shared by [com.clearchain.app.presentation.ngo.myrequests.MyRequestsViewModel]
 * and [com.clearchain.app.presentation.shared.requestdetail.RequestDetailScreen] — previously
 * duplicated in both, which is how they diverged (one copy had a mojibake-corrupted "…").
 *
 * A pickup can hold several listings, so the receipt lists every requested item and starts a new
 * page when they do not fit on one.
 */
object PickupReceiptPdf {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842 // A4
    private const val MARGIN = 40f
    private const val RIGHT_EDGE = PAGE_WIDTH - MARGIN
    private const val BOTTOM_LIMIT = PAGE_HEIGHT - 60f
    private const val VALUE_COLUMN = 220f

    fun build(context: Context, request: PickupRequest): Uri {
        val doc = PdfDocument()

        val titlePaint = Paint().apply {
            textSize = 24f
            color = Color.BLACK
            isFakeBoldText = true
        }
        val headingPaint = Paint().apply {
            textSize = 15f
            color = Color.BLACK
            isFakeBoldText = true
        }
        val labelPaint = Paint().apply {
            textSize = 14f
            color = Color.GRAY
        }
        val valuePaint = Paint().apply {
            textSize = 14f
            color = Color.BLACK
        }
        val smallPaint = Paint().apply {
            textSize = 11f
            color = Color.GRAY
        }
        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val pages = PageWriter(doc)
        try {
            pages.newPage()
            pages.canvas.drawText(context.getString(R.string.label_pickup_receipt), MARGIN, pages.y, titlePaint)
            pages.y += 8f
            pages.canvas.drawLine(MARGIN, pages.y, RIGHT_EDGE, pages.y, dividerPaint)
            pages.y += 30f

            fun row(label: String, value: String) {
                pages.ensureRoom(24f)
                pages.canvas.drawText(label, MARGIN, pages.y, labelPaint)
                pages.canvas.drawText(fit(value, valuePaint, RIGHT_EDGE - VALUE_COLUMN), VALUE_COLUMN, pages.y, valuePaint)
                pages.y += 24f
            }

            row(context.getString(R.string.label_reference_id), request.id.take(16) + "…")
            row(context.getString(R.string.label_from), request.groceryName)
            row(context.getString(R.string.label_pickup_date), request.pickupDate)
            row(context.getString(R.string.label_pickup_time), request.pickupTime)
            row(context.getString(R.string.label_status), context.getString(request.status.labelResId))
            request.notes?.takeIf { it.isNotBlank() }?.let { row(context.getString(R.string.label_notes), it) }

            pages.y += 12f
            pages.ensureRoom(40f)
            pages.canvas.drawText(context.getString(R.string.cart_requested_items), MARGIN, pages.y, headingPaint)
            pages.y += 10f
            pages.canvas.drawLine(MARGIN, pages.y, RIGHT_EDGE, pages.y, dividerPaint)
            pages.y += 24f

            fun item(title: String, quantity: String, category: String, expiryDate: String?) {
                if (title.isBlank()) return
                val detail = listOfNotNull(
                    categoryLabel(context, category).takeIf { it.isNotBlank() },
                    expiryDate?.takeIf { it.isNotBlank() }?.let {
                        context.getString(R.string.label_expires_date, DateTimeUtils.formatDate(it))
                    }
                ).joinToString(" · ")

                pages.ensureRoom(if (detail.isBlank()) 30f else 48f)
                val quantityWidth = valuePaint.measureText(quantity)
                pages.canvas.drawText(fit(title, valuePaint, RIGHT_EDGE - MARGIN - quantityWidth - 16f), MARGIN, pages.y, valuePaint)
                pages.canvas.drawText(quantity, RIGHT_EDGE - quantityWidth, pages.y, valuePaint)
                pages.y += 17f
                if (detail.isNotBlank()) {
                    pages.canvas.drawText(fit(detail, smallPaint, RIGHT_EDGE - MARGIN), MARGIN, pages.y, smallPaint)
                    pages.y += 17f
                }
                pages.y += 8f
            }

            if (request.items.isNotEmpty()) {
                request.items.forEach {
                    item(it.listingTitle, "${it.requestedQuantity} ${it.listingUnit}".trim(), it.listingCategory, it.listingExpiryDate)
                }
            } else {
                // Pickups made before items were tracked separately keep their listing on the request itself.
                item(
                    request.listingTitle,
                    "${request.requestedQuantity} ${request.listingUnit}".trim(),
                    request.listingCategory,
                    request.listingExpiryDate
                )
            }

            pages.y += 4f
            pages.ensureRoom(40f)
            pages.canvas.drawLine(MARGIN, pages.y, RIGHT_EDGE, pages.y, dividerPaint)
            pages.y += 20f
            pages.canvas.drawText(context.getString(R.string.pdf_generated_by), MARGIN, pages.y, smallPaint)

            pages.finish()

            // Under receipts/ because that is the cache subfolder the FileProvider is set up to share
            // (res/xml/file_paths.xml); a file directly in the cache folder cannot be turned into a uri.
            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(dir, "receipt_${request.id.take(8)}.pdf")
            file.outputStream().use { doc.writeTo(it) }

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } finally {
            doc.close()
        }
    }

    /** Categories are stored as keys ("BAKERY"); older rows may hold free text, shown as-is. */
    private fun categoryLabel(context: Context, raw: String): String =
        runCatching { FoodCategory.valueOf(raw.uppercase()) }.getOrNull()
            ?.let { context.getString(it.labelResId) } ?: raw

    /** Cuts [text] with an ellipsis so it fits [maxWidth] instead of running off the page. */
    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 1 && paint.measureText(text, 0, end) + paint.measureText(ellipsis) > maxWidth) end--
        return text.substring(0, end) + ellipsis
    }

    /** Hands out the current page and moves on to a new one when the next block would not fit. */
    private class PageWriter(private val doc: PdfDocument) {
        private var number = 0
        private var page: PdfDocument.Page? = null
        lateinit var canvas: Canvas
            private set
        var y = 0f

        fun newPage() {
            page?.let { doc.finishPage(it) }
            number++
            val next = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create())
            page = next
            canvas = next.canvas
            y = 60f
        }

        fun ensureRoom(height: Float) {
            if (y + height > BOTTOM_LIMIT) newPage()
        }

        fun finish() {
            page?.let { doc.finishPage(it) }
            page = null
        }
    }
}
