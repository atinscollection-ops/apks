package com.example.printing

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import java.io.FileOutputStream
import java.io.IOException

data class ShippingLabelData(
    val senderName: String = "Atin's Collection",
    val senderPhone: String = "0899-7884-435",
    val recipientName: String,
    val recipientPhone: String,
    val recipientAddress: String,
    val expedition: String,
    val doNumber: String,
    val soNumber: String,
    val doDate: String = "",
    val itemsSummary: String = "",
    val paperSize: String = "80mm" // 58mm, 80mm, 100mm
)

object ThermalLabelPrinter {

    fun getPaperWidthPoints(paperSize: String): Int {
        return when (paperSize) {
            "58mm" -> 164
            "100mm" -> 283
            else -> 226 // 80mm default
        }
    }

    /**
     * Renders exact Delivery Order label matching user attachment design with generous spacing.
     */
    fun createLabelBitmap(data: ShippingLabelData): Bitmap {
        val width = when (data.paperSize) {
            "58mm" -> 384
            "100mm" -> 800
            else -> 576 // 80mm standard
        }
        val estimatedHeight = (width * 1.8).toInt()

        val bitmap = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val strokeBorderWidth = (width * 0.007f).coerceAtLeast(3f)
        val paintBorder = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = strokeBorderWidth
        }

        val paintDivider = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = (width * 0.005f).coerceAtLeast(2.5f)
        }

        val paintLabelSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = (width * 0.034f).coerceAtLeast(14f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val paintValueBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = (width * 0.048f).coerceAtLeast(18f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val paintLargeTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = (width * 0.058f).coerceAtLeast(22f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val paintLargeValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = (width * 0.052f).coerceAtLeast(20f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val paintFooter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = (width * 0.034f).coerceAtLeast(13f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val outerMargin = width * 0.03f
        val innerPadding = width * 0.05f
        val innerWidth = width - (innerPadding * 2)

        var y = outerMargin + (width * 0.06f)

        // 1. TOP HEADER: "DELIVERY ORDER"
        canvas.drawText("DELIVERY ORDER", width / 2f, y, paintLargeTitle)
        y += width * 0.035f

        // Divider below header
        canvas.drawLine(outerMargin, y, width - outerMargin, y, paintDivider)
        y += width * 0.045f // Spacing lebar

        // 2. SECTION 1: DO ID & EKSPEDISI
        canvas.drawText("DO ID", innerPadding, y, paintLabelSmall)
        y += paintValueBold.textSize * 1.15f
        canvas.drawText(data.doNumber, innerPadding, y, paintLargeValue)
        y += width * 0.050f // Spacing lebar

        canvas.drawText("EKSPEDISI", innerPadding, y, paintLabelSmall)
        y += paintValueBold.textSize * 1.15f
        canvas.drawText(data.expedition, innerPadding, y, paintLargeValue)
        y += width * 0.045f // Spacing lebar

        // Divider below Section 1
        canvas.drawLine(outerMargin, y, width - outerMargin, y, paintDivider)
        y += width * 0.045f // Spacing lebar

        // 3. SECTION 2: PENGIRIM & NO HANDPHONE
        canvas.drawText("PENGIRIM", innerPadding, y, paintLabelSmall)
        y += paintValueBold.textSize * 1.15f
        canvas.drawText(data.senderName, innerPadding, y, paintValueBold)
        y += width * 0.040f // Spacing lebar

        canvas.drawText("NO HANDPHONE", innerPadding, y, paintLabelSmall)
        y += paintValueBold.textSize * 1.15f
        canvas.drawText(data.senderPhone, innerPadding, y, paintValueBold)
        y += width * 0.045f // Spacing lebar

        // Divider below Section 2
        canvas.drawLine(outerMargin, y, width - outerMargin, y, paintDivider)
        y += width * 0.045f // Spacing lebar

        // 4. SECTION 3: PENERIMA, TELEPON, ALAMAT
        canvas.drawText("NAMA PENERIMA", innerPadding, y, paintLabelSmall)
        y += paintValueBold.textSize * 1.15f
        canvas.drawText(data.recipientName, innerPadding, y, paintLargeValue)
        y += width * 0.040f // Spacing lebar

        canvas.drawText("NO TELEPON", innerPadding, y, paintLabelSmall)
        y += paintValueBold.textSize * 1.15f
        canvas.drawText(data.recipientPhone, innerPadding, y, paintValueBold)
        y += width * 0.040f // Spacing lebar

        canvas.drawText("ALAMAT", innerPadding, y, paintLabelSmall)
        y += paintValueBold.textSize * 1.15f

        // Address Word Wrapping
        val words = data.recipientAddress.split(" ")
        val line = StringBuilder()
        for (w in words) {
            val testLine = if (line.isEmpty()) w else "$line $w"
            if (paintValueBold.measureText(testLine) <= innerWidth) {
                line.setLength(0)
                line.append(testLine)
            } else {
                canvas.drawText(line.toString(), innerPadding, y, paintValueBold)
                y += paintValueBold.textSize * 1.25f
                line.setLength(0)
                line.append(w)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), innerPadding, y, paintValueBold)
            y += paintValueBold.textSize * 1.25f
        }
        y += width * 0.040f // Spacing lebar

        // Divider below Section 3
        canvas.drawLine(outerMargin, y, width - outerMargin, y, paintDivider)
        y += width * 0.045f // Spacing lebar

        // 5. FOOTER: SO, Tanggal, Isi
        val dateText = if (data.doDate.isNotBlank()) data.doDate else "2026-10-01"
        val isiText = if (data.itemsSummary.isNotBlank()) " | Isi: ${data.itemsSummary}" else ""
        val footerStr = "SO: ${data.soNumber} | Tgl: $dateText$isiText"
        canvas.drawText(footerStr, innerPadding, y, paintFooter)
        y += width * 0.05f

        // Draw Outer Solid Rectangle Border (matching image.png)
        canvas.drawRect(outerMargin, outerMargin, width - outerMargin, y, paintBorder)
        y += outerMargin

        val finalHeight = y.toInt()
        return Bitmap.createBitmap(bitmap, 0, 0, width, finalHeight)
    }

    /**
     * Starts system PrintManager print job for the shipping label.
     */
    fun printLabel(context: Context, labelData: ShippingLabelData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Shipping_Label_${labelData.doNumber}"

        val adapter = object : PrintDocumentAdapter() {
            private var pdfDocument: PrintedPdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                pdfDocument = PrintedPdfDocument(context, newAttributes)
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val page = pdfDocument?.startPage(0) ?: return
                val canvas = page.canvas

                val bitmap = createLabelBitmap(labelData)
                val destRect = RectF(0f, 0f, canvas.width.toFloat(), (canvas.width * bitmap.height.toFloat() / bitmap.width))
                canvas.drawBitmap(bitmap, null, destRect, null)

                pdfDocument?.finishPage(page)

                try {
                    val out = FileOutputStream(destination?.fileDescriptor)
                    pdfDocument?.writeTo(out)
                    out.close()
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: IOException) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument?.close()
                    pdfDocument = null
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()

        printManager.print(jobName, adapter, printAttributes)
    }
}
