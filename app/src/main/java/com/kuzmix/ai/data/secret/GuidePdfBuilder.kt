package com.kuzmix.ai.data.secret

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File

/**
 * Renders the bundled study guides (`assets/secret/ssN.json`) into a real
 * PDF document — "Study notes by Kuzmix AI".
 *
 * Format follows the school exam layout: subject headings, SECTION headings,
 * questions numbered 1. 2. 3. with (a)(b)(c)(d) options, diagrams inline.
 * Everything identifying the school (NAME/SUBJECT/CLASS/DATE lines,
 * INSTRUCTION lines, school header) is never rendered.
 */
object GuidePdfBuilder {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 44f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN
    private const val TOP = 52f
    private const val BOTTOM = PAGE_H - 56f

    fun build(context: Context, classKey: String, json: String): File {
        val root = org.json.JSONObject(json)
        val className = root.optString("class", classKey.uppercase())
        val subjects = root.getJSONArray("subjects")

        val subjectPaint = textPaint(14f, bold = true)
        val sectionPaint = textPaint(11.5f, bold = true)
        val qPaint = textPaint(10.5f)
        val optPaint = textPaint(10.5f)
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1.2f
            color = 0xFF333333.toInt()
        }

        val doc = PdfDocument()
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun finishPage() {
            val p = page
            if (p != null) {
                // newer API levels removed Page.finish(); the document API
                // still finalises pages explicitly
                doc.finishPage(p)
            }
            page = null
            canvas = null
        }

        fun newPage() {
            finishPage()
            pageNo += 1
            val p = doc.startPage(
                PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create()
            )
            page = p
            canvas = p.canvas
            y = TOP
            // footer on every page: label + page number
            val foot = textPaint(8f)
            p.canvas.drawText("Study notes by Kuzmix AI", MARGIN, PAGE_H - 30f, foot)
            val num = textPaint(8f)
            val numText = pageNo.toString()
            p.canvas.drawText(numText, PAGE_W - MARGIN - num.measureText(numText), PAGE_H - 30f, num)
        }

        fun ensure(space: Float) {
            if (page == null || y + space > BOTTOM) newPage()
        }

        fun drawStatic(layout: StaticLayout, x: Float = MARGIN, extra: Float = 4f) {
            ensure(layout.height.toFloat())
            val c = canvas
            if (c != null) {
                c.save()
                c.translate(x, y)
                layout.draw(c)
                c.restore()
            }
            y += layout.height + extra
        }

        fun drawText(text: String, paint: TextPaint, extra: Float = 4f) {
            drawStatic(static(text, paint, CONTENT_W.toInt()), extra = extra)
        }

        fun drawIndented(text: String, paint: TextPaint, indent: Float, extra: Float = 3f) {
            drawStatic(static(text, paint, (CONTENT_W - indent).toInt()), MARGIN + indent, extra)
        }

        // ---- cover ----
        newPage()
        y = 210f
        drawStatic(static("Study notes by Kuzmix AI", textPaint(20f, bold = true),
            CONTENT_W.toInt(), center = true), extra = 10f)
        drawStatic(static("$className  ·  1st Term Exam", textPaint(12f),
            CONTENT_W.toInt(), center = true), extra = 8f)
        drawStatic(static("Read-only · screenshots disabled · for Kuzmix AI students",
            textPaint(9f, color = 0xFF666666.toInt()), CONTENT_W.toInt(), center = true))

        // ---- subjects ----
        for (si in 0 until subjects.length()) {
            val subject = subjects.getJSONObject(si)
            val name = subject.optString("name", "Subject").uppercase()
            val questions = subject.getJSONArray("questions")

            newPage()
            drawText(name, subjectPaint, extra = 2f)
            canvas?.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rulePaint)
            y += 14f

            var currentSection = ""
            var numberInSection = 0
            for (qi in 0 until questions.length()) {
                val q = questions.getJSONObject(qi)
                val section = q.optString("section", "")
                if (section != currentSection) {
                    currentSection = section
                    numberInSection = 0
                    ensure(40f)
                    y += 6f
                    drawText(section.uppercase(), sectionPaint, extra = 6f)
                }
                numberInSection += 1

                val stem = cleanStem(q.optString("stem", ""))
                if (stem.isBlank()) continue

                drawStatic(static("$numberInSection. $stem", qPaint, CONTENT_W.toInt()), extra = 3f)

                val options = q.optJSONArray("options")
                if (options != null) {
                    val letters = listOf("a", "b", "c", "d", "e", "f")
                    for (oi in 0 until options.length()) {
                        val opt = options.optString(oi).trim()
                        if (opt.isEmpty()) continue
                        val letter = letters.getOrElse(oi) { oi.toString() }
                        drawIndented("($letter) $opt", optPaint, 16f)
                    }
                    y += 5f
                }

                val image: String? =
                    if (q.has("image") && !q.isNull("image")) q.optString("image") else null
                if (!image.isNullOrBlank()) {
                    val bmp = decodeScaled(context, image)
                    if (bmp != null) {
                        val w = CONTENT_W
                        val h = w * bmp.height / bmp.width
                        ensure(h + 10f)
                        val c = canvas
                        if (c != null) {
                            c.save()
                            c.translate(MARGIN, y)
                            c.scale(w / bmp.width, h / bmp.height)
                            c.drawBitmap(bmp, 0f, 0f, null)
                            c.restore()
                        }
                        y += h + 10f
                    }
                }
            }
        }
        finishPage()

        val dir = File(context.cacheDir, "study_notes").apply { mkdirs() }
        val out = File(dir, "$classKey.pdf")
        doc.writeTo(out.outputStream())
        doc.close()
        return out
    }

    /** Drops leftover "a)" prefixes — numbering is rendered by us. */
    private fun cleanStem(stem: String): String =
        stem.replace(Regex("^\\(?a[).]\\s*"), "").trim()

    private fun static(
        text: String,
        paint: TextPaint,
        width: Int,
        center: Boolean = false
    ): StaticLayout {
        val alignment = if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(alignment)
            .setLineSpacing(2f, 1f)
            .setIncludePad(false)
            .build()
    }

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = 0xFF111111.toInt()): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            isFakeBoldText = bold
        }

    private fun decodeScaled(context: Context, asset: String): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(asset).use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = maxOf(1, bounds.outWidth / 1200)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.assets.open(asset).use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (t: Throwable) {
        null
    }
}
