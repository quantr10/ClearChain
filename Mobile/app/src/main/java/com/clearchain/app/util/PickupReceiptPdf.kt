package com.clearchain.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.clearchain.app.R
import com.clearchain.app.domain.model.PickupRequest
import java.io.File

/**
 * The one-page pickup receipt shared by [com.clearchain.app.presentation.ngo.myrequests.MyRequestsViewModel]
 * and [com.clearchain.app.presentation.shared.requestdetail.RequestDetailScreen] — previously
 * duplicated in both, which is how they diverged (one copy had a mojibake-corrupted "…").
 */
object PickupReceiptPdf {
    fun build(context: Context, request: PickupRequest): Uri {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 24f
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
        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 60f
        canvas.drawText(context.getString(R.string.label_pickup_receipt), 40f, y, titlePaint)
        y += 8f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 30f

        fun row(label: String, value: String) {
            canvas.drawText(label, 40f, y, labelPaint)
            canvas.drawText(value, 220f, y, valuePaint)
            y += 24f
        }

        row(context.getString(R.string.label_reference_id), request.id.take(16) + "…")
        row(context.getString(R.string.label_food_item), request.listingTitle)
        row(context.getString(R.string.label_category), request.listingCategory)
        row(context.getString(R.string.listing_quantity), "${request.requestedQuantity}")
        row(context.getString(R.string.label_from), request.groceryName)
        row(context.getString(R.string.label_pickup_date), request.pickupDate)
        row(context.getString(R.string.label_pickup_time), request.pickupTime)
        row(context.getString(R.string.label_status), request.status.name)
        request.notes?.takeIf { it.isNotBlank() }?.let { row(context.getString(R.string.label_notes), it.take(60)) }

        y += 12f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 20f
        canvas.drawText(
            context.getString(R.string.pdf_generated_by),
            40f,
            y,
            labelPaint.apply { textSize = 10f }
        )

        doc.finishPage(page)

        val file = File(context.cacheDir, "receipt_${request.id.take(8)}.pdf")
        doc.writeTo(file.outputStream())
        doc.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
