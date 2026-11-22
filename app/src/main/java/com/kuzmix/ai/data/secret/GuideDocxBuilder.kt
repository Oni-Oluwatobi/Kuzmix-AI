package com.kuzmix.ai.data.secret

import android.content.Context
import com.kuzmix.ai.domain.ai.SecretDocs
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

/**
 * Builds "Study notes by Kuzmix AI — SS X.docx" from the bundled guide
 * JSON — a real Word document with no external dependencies.
 *
 * Format: subject headings, SECTION headings, questions numbered 1. 2. 3.
 * with (a)(b)(c)(d) options and diagrams inline. Everything identifying
 * the school (NAME/SUBJECT/CLASS/DATE lines, INSTRUCTION lines, school
 * header) is never written.
 */
object GuideDocxBuilder {

    fun build(context: Context, classKey: String, json: String): File {
        val root = org.json.JSONObject(json)
        val className = root.optString("class", classKey.uppercase())
        val subjects = root.getJSONArray("subjects")

        val body = StringBuilder()
        val mediaRels = StringBuilder()
        val mediaFiles = mutableListOf<Pair<String, ByteArray>>()
        var drawingId = 0

        fun para(runs: String, center: Boolean = false, indent: Int = 0, pageBreak: Boolean = false) {
            body.append("<w:p><w:pPr>")
            if (pageBreak) body.append("<w:pageBreakBefore/>")
            if (center) body.append("<w:jc w:val=\"center\"/>")
            if (indent > 0) body.append("<w:ind w:left=\"$indent\"/>")
            body.append("<w:spacing w:after=\"120\"/></w:pPr>$runs</w:p>")
        }

        fun run(text: String, bold: Boolean = false, sz: Int? = null, italic: Boolean = false): String {
            val props = StringBuilder("<w:rPr>")
            if (bold) props.append("<w:b/>")
            if (italic) props.append("<w:i/>")
            sz?.let { props.append("<w:sz w:val=\"$it\"/><w:szCs w:val=\"$it\"/>") }
            props.append("</w:rPr>")
            // split on newlines -> breaks; Word rejects raw control chars
            val lines = text.split('\n').map { esc(it.replace('\t', ' ')) }
            val content = lines.joinToString("<w:br/>") { l ->
                "<w:t xml:space=\"preserve\">$l</w:t>"
            }
            return "<w:r>$props$content</w:r>"
        }

        fun imagePara(asset: String): String? = try {
            val bytes = context.assets.open(asset).use { it.readBytes() }
            val ext = asset.substringAfterLast('.').lowercase()
            drawingId += 1
            val relId = "rIdImg$drawingId"
            val name = "image$drawingId.$ext"
            mediaFiles.add(name to bytes)
            mediaRels.append(
                "<Relationship Id=\"$relId\" " +
                    "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" " +
                    "Target=\"media/$name\"/>"
            )
            // ~6.2in wide in EMU (1px @96dpi = 9525 EMU)
            val cx = 5700000L
            val probe = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeStream(
                context.assets.open(asset), null, probe
            )
            val w = if (probe.outWidth > 0) probe.outWidth else 1000
            val h = if (probe.outHeight > 0) probe.outHeight else 700
            val cy = cx * h / w
            body.append(
                "<w:p><w:r><w:drawing>" +
                    "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
                    "<wp:extent cx=\"$cx\" cy=\"$cy\"/>" +
                    "<wp:docPr id=\"$drawingId\" name=\"$name\"/>" +
                    "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
                    "<pic:pic><pic:nvPicPr><pic:cNvPr id=\"$drawingId\" name=\"$name\"/><pic:cNvPicPr/></pic:nvPicPr>" +
                    "<pic:blipFill><a:blip r:embed=\"$relId\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>" +
                    "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"$cx\" cy=\"$cy\"/></a:xfrm>" +
                    "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr>" +
                    "</pic:pic></a:graphicData></a:graphic></wp:inline>" +
                    "</w:drawing></w:r></w:p>"
            )
            null
        } catch (t: Throwable) {
            null
        }

        // ---- cover ----
        para(run("Study notes by Kuzmix AI", bold = true, sz = 32), center = true)
        para(run("$className  ·  1st Term Exam", sz = 24), center = true)
        para(run("Read-only study notes for Kuzmix AI students", sz = 18), center = true)

        // ---- subjects ----
        for (si in 0 until subjects.length()) {
            val subject = subjects.getJSONObject(si)
            val name = subject.optString("name", "Subject").uppercase()
            val questions = subject.getJSONArray("questions")

            para(run(name, bold = true, sz = 26), pageBreak = true)

            var currentSection = ""
            var numberInSection = 0
            for (qi in 0 until questions.length()) {
                val q = questions.getJSONObject(qi)
                val section = q.optString("section", "")
                val isPassage = q.optString("kind", "question") == "passage"
                if (section != currentSection) {
                    currentSection = section
                    numberInSection = 0
                    para(run(section.uppercase(), bold = true, sz = 22))
                }

                val stem = cleanStem(q.optString("stem", ""))
                if (stem.isBlank()) continue

                if (isPassage) {
                    // shared reading passage — unnumbered, italic
                    para(run(stem, sz = 21, italic = true))
                    continue
                }
                numberInSection += 1

                para(run("$numberInSection. $stem", sz = 21))

                val options = q.optJSONArray("options")
                if (options != null) {
                    val letters = listOf("a", "b", "c", "d", "e", "f")
                    for (oi in 0 until options.length()) {
                        val opt = options.optString(oi).trim()
                        if (opt.isEmpty()) continue
                        val letter = letters.getOrElse(oi) { oi.toString() }
                        para(run("($letter) $opt", sz = 21), indent = 720)
                    }
                }

                val image: String? =
                    if (q.has("image") && !q.isNull("image")) q.optString("image") else null
                if (!image.isNullOrBlank()) {
                    imagePara(image)
                }
            }
        }

        val sectPr =
            "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>" +
                "<w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\"/></w:sectPr>"

        val document =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<w:document " +
                "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" " +
                "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" " +
                "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
                "xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
                "<w:body>$body$sectPr</w:body></w:document>"

        val contentTypes =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Default Extension=\"png\" ContentType=\"image/png\"/>" +
                "<Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/>" +
                "<Default Extension=\"jpg\" ContentType=\"image/jpeg\"/>" +
                "<Override PartName=\"/word/document.xml\" " +
                "ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                "</Types>"

        val rootRels =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" " +
                "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" " +
                "Target=\"word/document.xml\"/></Relationships>"

        val docRels =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                mediaRels.toString() +
                "</Relationships>"

        val dir = File(context.filesDir, "study_notes").apply { mkdirs() }
        val out = File(dir, "Study notes by Kuzmix AI - ${SecretDocs.labelFor(classKey)}.docx")
        ZipOutputStream(FileOutputStream(out)).use { zip ->
            zip.putNextEntry(zipEntry("[Content_Types].xml")); zip.write(contentTypes.toByteArray())
            zip.putNextEntry(zipEntry("_rels/.rels")); zip.write(rootRels.toByteArray())
            zip.putNextEntry(zipEntry("word/document.xml")); zip.write(document.toByteArray())
            zip.putNextEntry(zipEntry("word/_rels/document.xml.rels")); zip.write(docRels.toByteArray())
            for ((name, bytes) in mediaFiles) {
                zip.putNextEntry(zipEntry("word/media/$name")); zip.write(bytes)
            }
            zip.closeEntry()
        }
        return out
    }

    private fun zipEntry(name: String) = java.util.zip.ZipEntry(name)

    /** Drops leftover "a)" prefixes — numbering is rendered by us. */
    private fun cleanStem(stem: String): String =
        stem.replace(Regex("^\\(?a[).]\\s*"), "").trim()

    private fun esc(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), " ")
}
