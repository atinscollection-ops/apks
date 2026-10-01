package com.example.excel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun columnLetter(colIndex: Int): String {
        var n = colIndex
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, ('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.toString()
    }

    /**
     * Generates a valid ECMA-376 OpenXML (.xlsx) file.
     */
    fun createXlsxFile(
        context: Context,
        fileName: String,
        sheetTitle: String,
        headers: List<String>,
        rows: List<List<Any>>
    ): File {
        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val finalFile = File(exportDir, fileName)
        if (finalFile.exists()) finalFile.delete()

        ZipOutputStream(FileOutputStream(finalFile)).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".trimIndent()
            zip.write(contentTypes.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()
            zip.write(rootRels.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            val wbRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()
            zip.write(wbRels.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 4. xl/styles.xml
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="2">
        <font>
            <sz val="11"/>
            <name val="Calibri"/>
        </font>
        <font>
            <b/>
            <sz val="11"/>
            <color rgb="FFFFFFFF"/>
            <name val="Calibri"/>
        </font>
    </fonts>
    <fills count="3">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        <fill>
            <patternFill patternType="solid">
                <fgColor rgb="FFC75B7A"/>
            </patternFill>
        </fill>
    </fills>
    <borders count="1">
        <border><left/><right/><top/><bottom/><diagonal/></border>
    </borders>
    <cellStyleXfs count="1">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
    </cellStyleXfs>
    <cellXfs count="2">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
    </cellXfs>
</styleSheet>""".trimIndent()
            zip.write(styles.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 5. xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="${escapeXml(sheetTitle)}" sheetId="1" r:id="rId1"/>
    </sheets>
</workbook>""".trimIndent()
            zip.write(workbook.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 6. xl/worksheets/sheet1.xml
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheetSb = java.lang.StringBuilder()
            sheetSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sheetSb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            sheetSb.append("""<sheetData>""")

            var rowIndex = 1

            // Header row with style 1 (Dusty Pink header background + white bold text)
            sheetSb.append("""<row r="$rowIndex">""")
            headers.forEachIndexed { colIdx, headerText ->
                val cellRef = "${columnLetter(colIdx)}$rowIndex"
                sheetSb.append("""<c r="$cellRef" s="1" t="inlineStr"><is><t>${escapeXml(headerText)}</t></is></c>""")
            }
            sheetSb.append("""</row>""")
            rowIndex++

            // Data rows
            for (row in rows) {
                sheetSb.append("""<row r="$rowIndex">""")
                row.forEachIndexed { colIdx, cellValue ->
                    val cellRef = "${columnLetter(colIdx)}$rowIndex"
                    when (cellValue) {
                        is Number -> {
                            sheetSb.append("""<c r="$cellRef"><v>$cellValue</v></c>""")
                        }
                        is Boolean -> {
                            sheetSb.append("""<c r="$cellRef" t="b"><v>${if (cellValue) 1 else 0}</v></c>""")
                        }
                        else -> {
                            sheetSb.append("""<c r="$cellRef" t="inlineStr"><is><t>${escapeXml(cellValue.toString())}</t></is></c>""")
                        }
                    }
                }
                sheetSb.append("""</row>""")
                rowIndex++
            }

            sheetSb.append("""</sheetData>""")
            sheetSb.append("""</worksheet>""")

            zip.write(sheetSb.toString().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }

        return finalFile
    }

    /**
     * Shares or opens the exported file.
     */
    fun shareFile(context: Context, file: File, title: String = "Share Report Excel") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
