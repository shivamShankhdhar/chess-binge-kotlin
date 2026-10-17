package bizora.app.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import bizora.app.data.model.AttendanceStatus
import bizora.app.data.model.RosterItem
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ReportFormat(val extension: String, val mimeType: String, val displayName: String) {
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel Spreadsheet"),
    PDF("pdf", "application/pdf", "PDF Document"),
    CSV("csv", "text/csv", "CSV Spreadsheet")
}

data class ExportedReport(
    val file: File,
    val uri: Uri,
    val format: ReportFormat,
    val filename: String,
    val fileSizeFormatted: String,
    val recordCount: Int,
    val workplaceName: String,
    val generatedDate: String
)

object ReportExporter {

    private fun getReportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun calculateWorkDuration(checkIn: String?, checkOut: String?): String {
        if (checkIn.isNullOrBlank() || checkIn == "-") return "-"
        if (checkOut.isNullOrBlank() || checkOut == "-") return "In Progress"
        return try {
            val inMinutes = parseTimeToMinutes(checkIn)
            val outMinutes = parseTimeToMinutes(checkOut)
            if (inMinutes != null && outMinutes != null && outMinutes >= inMinutes) {
                val diff = outMinutes - inMinutes
                val hrs = diff / 60
                val mins = diff % 60
                "${hrs}h ${mins}m"
            } else {
                "In Progress"
            }
        } catch (e: Exception) {
            "-"
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        val clean = timeStr.trim().uppercase()
        val regex12 = Regex("""^(\d{1,2}):(\d{2})\s*(AM|PM)$""")
        val match12 = regex12.find(clean)
        if (match12 != null) {
            var hour = match12.groupValues[1].toInt()
            val min = match12.groupValues[2].toInt()
            val ampm = match12.groupValues[3]
            if (ampm == "PM" && hour < 12) hour += 12
            if (ampm == "AM" && hour == 12) hour = 0
            return hour * 60 + min
        }
        val regex24 = Regex("""^(\d{1,2}):(\d{2})(?::\d{2})?$""")
        val match24 = regex24.find(clean)
        if (match24 != null) {
            val hour = match24.groupValues[1].toInt()
            val min = match24.groupValues[2].toInt()
            return hour * 60 + min
        }
        return null
    }

    private fun sanitizeName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    /**
     * Builds a genuine OpenXML Excel (.xlsx) file and returns the ExportedReport
     */
    fun exportToExcel(
        context: Context,
        workplaceName: String,
        roster: List<RosterItem>,
        date: LocalDate = LocalDate.now()
    ): ExportedReport {
        val dateStamp = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val filename = "Bizora_Report_$dateStamp.xlsx"
        val outputFile = File(getReportsDir(context), filename)

        val total = roster.size
        val present = roster.count { it.status == AttendanceStatus.PRESENT }
        val absent = roster.count { it.status == AttendanceStatus.ABSENT }
        val pending = roster.count { it.status == AttendanceStatus.PENDING }
        val rate = if (total > 0) (present * 100) / total else 0
        val dateFormatted = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        val nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // 2. _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // 3. xl/_rels/workbook.xml.rels
                zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // 4. xl/workbook.xml
                zos.putNextEntry(ZipEntry("xl/workbook.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Attendance Report" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // 5. xl/styles.xml
                zos.putNextEntry(ZipEntry("xl/styles.xml"))
                zos.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="16"/><color rgb="FF48562A"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF222222"/><name val="Calibri"/></font>
  </fonts>
  <fills count="5">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF48562A"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF4F7EE"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE8EEDF"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD5DEC9"/></left>
      <right style="thin"><color rgb="FFD5DEC9"/></right>
      <top style="thin"><color rgb="FFD5DEC9"/></top>
      <bottom style="thin"><color rgb="FFD5DEC9"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="7">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="3" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/>
  </cellXfs>
</styleSheet>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                )
                zos.closeEntry()

                // 6. xl/worksheets/sheet1.xml
                zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                val sheetXml = buildString {
                    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
                    append("""<cols>""")
                    append("""<col min="1" max="1" width="18" customWidth="1"/>""")
                    append("""<col min="2" max="2" width="28" customWidth="1"/>""")
                    append("""<col min="3" max="3" width="16" customWidth="1"/>""")
                    append("""<col min="4" max="4" width="18" customWidth="1"/>""")
                    append("""<col min="5" max="5" width="20" customWidth="1"/>""")
                    append("""<col min="6" max="6" width="20" customWidth="1"/>""")
                    append("""</cols>""")
                    append("""<sheetData>""")

                    // Row 1: Title
                    append("""<row r="1" ht="30" customHeight="1">""")
                    append("""<c r="A1" s="1" t="inlineStr"><is><t>BIZORA ATTENDANCE REPORT</t></is></c>""")
                    append("""</row>""")

                    // Row 2: Workplace & Meta
                    append("""<row r="2" ht="20" customHeight="1">""")
                    append("""<c r="A2" t="inlineStr"><is><t>Workplace: ${escapeXml(workplaceName)}</t></is></c>""")
                    append("""<c r="C2" t="inlineStr"><is><t>Report Date: $dateFormatted</t></is></c>""")
                    append("""<c r="E2" t="inlineStr"><is><t>Generated: $nowFormatted</t></is></c>""")
                    append("""</row>""")

                    // Row 3: Blank
                    append("""<row r="3" ht="10" customHeight="1"/>""")

                    // Row 4: Summary Statistics
                    append("""<row r="4" ht="24" customHeight="1">""")
                    append("""<c r="A4" s="5" t="inlineStr"><is><t>Total Roster: $total</t></is></c>""")
                    append("""<c r="B4" s="5" t="inlineStr"><is><t>Present: $present ($rate%)</t></is></c>""")
                    append("""<c r="C4" s="5" t="inlineStr"><is><t>Absent: $absent</t></is></c>""")
                    append("""<c r="D4" s="5" t="inlineStr"><is><t>Pending: $pending</t></is></c>""")
                    append("""<c r="E4" s="5" t="inlineStr"><is><t>Attendance Rate: $rate%</t></is></c>""")
                    append("""</row>""")

                    // Row 5: Blank
                    append("""<row r="5" ht="12" customHeight="1"/>""")

                    // Row 6: Table Headers
                    append("""<row r="6" ht="26" customHeight="1">""")
                    append("""<c r="A6" s="2" t="inlineStr"><is><t>Employee ID</t></is></c>""")
                    append("""<c r="B6" s="2" t="inlineStr"><is><t>Employee Name</t></is></c>""")
                    append("""<c r="C6" s="2" t="inlineStr"><is><t>Date</t></is></c>""")
                    append("""<c r="D6" s="2" t="inlineStr"><is><t>Status</t></is></c>""")
                    append("""<c r="E6" s="2" t="inlineStr"><is><t>Check In Time</t></is></c>""")
                    append("""<c r="F6" s="2" t="inlineStr"><is><t>Check Out Time</t></is></c>""")
                    append("""<c r="G6" s="2" t="inlineStr"><is><t>Total Work Time</t></is></c>""")
                    append("""</row>""")

                    // Rows 7+: Data rows
                    roster.forEachIndexed { index, item ->
                        val rowIndex = 7 + index
                        val rowStyle = if (index % 2 == 1) "4" else "3"
                        val statusText = when (item.status) {
                            AttendanceStatus.PRESENT -> "PRESENT"
                            AttendanceStatus.ABSENT -> "ABSENT"
                            AttendanceStatus.HALF_DAY -> "HALF DAY"
                            AttendanceStatus.LEAVE -> "ON LEAVE"
                            AttendanceStatus.PENDING -> "PENDING"
                            AttendanceStatus.NOT_MARKED -> "NOT MARKED"
                        }
                        val workDuration = calculateWorkDuration(item.checkInTime, item.checkOutTime)
                        append("""<row r="$rowIndex" ht="22" customHeight="1">""")
                        append("""<c r="A$rowIndex" s="$rowStyle" t="inlineStr"><is><t>${escapeXml(item.employeeCode)}</t></is></c>""")
                        append("""<c r="B$rowIndex" s="$rowStyle" t="inlineStr"><is><t>${escapeXml(item.name)}</t></is></c>""")
                        append("""<c r="C$rowIndex" s="$rowStyle" t="inlineStr"><is><t>$dateFormatted</t></is></c>""")
                        append("""<c r="D$rowIndex" s="$rowStyle" t="inlineStr"><is><t>$statusText</t></is></c>""")
                        append("""<c r="E$rowIndex" s="$rowStyle" t="inlineStr"><is><t>${escapeXml(item.checkInTime ?: "-")}</t></is></c>""")
                        append("""<c r="F$rowIndex" s="$rowStyle" t="inlineStr"><is><t>${escapeXml(item.checkOutTime ?: "-")}</t></is></c>""")
                        append("""<c r="G$rowIndex" s="$rowStyle" t="inlineStr"><is><t>${escapeXml(workDuration)}</t></is></c>""")
                        append("""</row>""")
                    }

                    // Blank Row
                    val summaryStart = 7 + roster.size + 1
                    append("""<row r="$summaryStart" ht="12" customHeight="1"/>""")

                    // Report Totals Section
                    val tRow1 = summaryStart + 1
                    append("""<row r="$tRow1" ht="24" customHeight="1">""")
                    append("""<c r="A$tRow1" s="6" t="inlineStr"><is><t>REPORT AUDIT SUMMARY</t></is></c>""")
                    append("""</row>""")

                    val tRow2 = tRow1 + 1
                    append("""<row r="$tRow2" ht="20" customHeight="1">""")
                    append("""<c r="A$tRow2" s="3" t="inlineStr"><is><t>Total Workforce Records</t></is></c>""")
                    append("""<c r="B$tRow2" s="6" t="inlineStr"><is><t>$total</t></is></c>""")
                    append("""</row>""")

                    val tRow3 = tRow2 + 1
                    append("""<row r="$tRow3" ht="20" customHeight="1">""")
                    append("""<c r="A$tRow3" s="3" t="inlineStr"><is><t>Present Members</t></is></c>""")
                    append("""<c r="B$tRow3" s="6" t="inlineStr"><is><t>$present ($rate%)</t></is></c>""")
                    append("""</row>""")

                    val tRow4 = tRow3 + 1
                    append("""<row r="$tRow4" ht="20" customHeight="1">""")
                    append("""<c r="A$tRow4" s="3" t="inlineStr"><is><t>Absent Members</t></is></c>""")
                    append("""<c r="B$tRow4" s="6" t="inlineStr"><is><t>$absent</t></is></c>""")
                    append("""</row>""")

                    append("""</sheetData>""")
                    append("""<mergeCells count="2">""")
                    append("""<mergeCell ref="A1:G1"/>""")
                    append("""<mergeCell ref="A$tRow1:G$tRow1"/>""")
                    append("""</mergeCells>""")
                    append("""</worksheet>""")
                }

                zos.write(sheetXml.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
        return ExportedReport(
            file = outputFile,
            uri = uri,
            format = ReportFormat.EXCEL,
            filename = filename,
            fileSizeFormatted = formatFileSize(outputFile.length()),
            recordCount = roster.size,
            workplaceName = workplaceName,
            generatedDate = nowFormatted
        )
    }

    /**
     * Builds a printable, styled PDF document and returns the ExportedReport
     */
    fun exportToPdf(
        context: Context,
        workplaceName: String,
        roster: List<RosterItem>,
        date: LocalDate = LocalDate.now()
    ): ExportedReport {
        val dateStamp = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val filename = "Bizora_Report_$dateStamp.pdf"
        val outputFile = File(getReportsDir(context), filename)

        val total = roster.size
        val present = roster.count { it.status == AttendanceStatus.PRESENT }
        val absent = roster.count { it.status == AttendanceStatus.ABSENT }
        val pending = roster.count { it.status == AttendanceStatus.PENDING }
        val rate = if (total > 0) (present * 100) / total else 0
        val dateFormatted = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        val nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))

        val pdfDoc = PdfDocument()

        // Page settings: Landscape A4 (842 x 595 points)
        val pageWidth = 842
        val pageHeight = 595
        val margin = 40f
        val contentWidth = pageWidth - (margin * 2)

        // Paints
        val brandColor = Color.parseColor("#48562A")
        val brandLightColor = Color.parseColor("#F4F7EE")
        val brandBorderColor = Color.parseColor("#D5DEC9")
        val altRowColor = Color.parseColor("#F9FAF6")
        val textDark = Color.parseColor("#1F2937")
        val textMuted = Color.parseColor("#6B7280")

        val paintBrand = Paint().apply {
            color = brandColor
            isAntiAlias = true
        }

        val paintText = Paint().apply {
            color = textDark
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintBold = Paint().apply {
            color = textDark
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = brandBorderColor
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val itemsPerPage = 11
        val pagesCount = maxOf(1, (roster.size + itemsPerPage - 1) / itemsPerPage)

        for (pageIndex in 0 until pagesCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // 1. Brand Header
            // Circular Logo
            val logoRadius = 16f
            val logoCx = margin + logoRadius
            val logoCy = margin + logoRadius
            canvas.drawCircle(logoCx, logoCy, logoRadius, paintBrand)

            val paintLogoText = Paint().apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("B", logoCx, logoCy + 6f, paintLogoText)

            // Title & Subtitle
            val paintTitle = Paint().apply {
                color = brandColor
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("BIZORA", margin + 42f, margin + 14f, paintTitle)

            val paintSubtitle = Paint().apply {
                color = textMuted
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("ATTENDANCE AUDIT REPORT", margin + 42f, margin + 28f, paintSubtitle)

            // Right Header Info
            val paintMetaRight = Paint().apply {
                color = textDark
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            canvas.drawText(workplaceName, pageWidth - margin, margin + 12f, paintMetaRight)

            val paintMetaSub = Paint().apply {
                color = textMuted
                textSize = 9f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            canvas.drawText("Date: $dateFormatted  |  Generated: $nowFormatted", pageWidth - margin, margin + 26f, paintMetaSub)

            // Header line
            canvas.drawLine(margin, margin + 40f, pageWidth - margin, margin + 40f, paintLine)

            // 2. Summary KPI Metrics (Only on Page 1)
            var currentY = margin + 52f
            if (pageIndex == 0) {
                val cardWidth = (contentWidth - 30f) / 4f
                val cardHeight = 44f
                val kpis = listOf(
                    Triple("Total Workforce", "$total", textDark),
                    Triple("Present (${rate}%)", "$present", Color.parseColor("#2E7D32")),
                    Triple("Absent", "$absent", Color.parseColor("#D32F2F")),
                    Triple("Pending Approvals", "$pending", Color.parseColor("#ED6C02"))
                )

                val paintCardBg = Paint().apply {
                    color = brandLightColor
                    isAntiAlias = true
                }

                kpis.forEachIndexed { i, kpi ->
                    val cardX = margin + i * (cardWidth + 10f)
                    val rect = RectF(cardX, currentY, cardX + cardWidth, currentY + cardHeight)
                    canvas.drawRoundRect(rect, 8f, 8f, paintCardBg)
                    canvas.drawRoundRect(rect, 8f, 8f, paintLine)

                    val paintKpiLabel = Paint().apply {
                        color = textMuted
                        textSize = 8.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        isAntiAlias = true
                    }
                    canvas.drawText(kpi.first, cardX + 10f, currentY + 16f, paintKpiLabel)

                    val paintKpiValue = Paint().apply {
                        color = kpi.third
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    canvas.drawText(kpi.second, cardX + 10f, currentY + 35f, paintKpiValue)
                }

                currentY += cardHeight + 14f
            }

            // 3. Table Header
            val tableY = currentY
            val headerHeight = 26f
            val rectHeader = RectF(margin, tableY, pageWidth - margin, tableY + headerHeight)
            canvas.drawRoundRect(rectHeader, 6f, 6f, paintBrand)

            val cols = listOf(
                Pair("EMPLOYEE ID", 90f),
                Pair("NAME", 160f),
                Pair("DATE", 80f),
                Pair("STATUS", 90f),
                Pair("CHECK IN", 85f),
                Pair("CHECK OUT", 85f),
                Pair("TOTAL TIME", contentWidth - 590f)
            )

            val paintHeaderCol = Paint().apply {
                color = Color.WHITE
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            var colX = margin + 10f
            cols.forEach { col ->
                canvas.drawText(col.first, colX, tableY + 17f, paintHeaderCol)
                colX += col.second
            }

            // 4. Data Rows
            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, roster.size)
            val rowHeight = 26f
            var rowY = tableY + headerHeight

            val paintRowBg = Paint().apply {
                color = altRowColor
                isAntiAlias = true
            }

            for (i in startIndex until endIndex) {
                val item = roster[i]
                if ((i - startIndex) % 2 == 1) {
                    canvas.drawRect(margin, rowY, pageWidth - margin, rowY + rowHeight, paintRowBg)
                }

                // Col 1: ID
                var cellX = margin + 10f
                canvas.drawText(item.employeeCode, cellX, rowY + 17f, paintText)

                // Col 2: Name
                cellX += cols[0].second
                canvas.drawText(item.name, cellX, rowY + 17f, paintBold)

                // Col 3: Date
                cellX += cols[1].second
                canvas.drawText(dateFormatted, cellX, rowY + 17f, paintText)

                // Col 4: Status (Cute pill)
                cellX += cols[2].second
                val (statusBg, statusFg, statusLabel) = when (item.status) {
                    AttendanceStatus.PRESENT -> Triple("#E8F5E9", "#2E7D32", "PRESENT")
                    AttendanceStatus.ABSENT -> Triple("#FFEBEE", "#C62828", "ABSENT")
                    AttendanceStatus.HALF_DAY -> Triple("#FFF8E1", "#F57F17", "HALF DAY")
                    AttendanceStatus.LEAVE -> Triple("#E1F5FE", "#0277BD", "ON LEAVE")
                    AttendanceStatus.PENDING -> Triple("#FFF3E0", "#EF6C00", "PENDING")
                    AttendanceStatus.NOT_MARKED -> Triple("#F5F5F5", "#757575", "NOT MARKED")
                }
                val paintPillBg = Paint().apply {
                    color = Color.parseColor(statusBg)
                    isAntiAlias = true
                }
                val pillRect = RectF(cellX, rowY + 5f, cellX + 76f, rowY + rowHeight - 5f)
                canvas.drawRoundRect(pillRect, 10f, 10f, paintPillBg)

                val paintPillFg = Paint().apply {
                    color = Color.parseColor(statusFg)
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(statusLabel, cellX + 38f, rowY + 16.5f, paintPillFg)

                // Col 5: Check In
                cellX += cols[3].second
                canvas.drawText(item.checkInTime ?: "--", cellX, rowY + 17f, paintText)

                // Col 6: Check Out
                cellX += cols[4].second
                canvas.drawText(item.checkOutTime ?: "--", cellX, rowY + 17f, paintText)

                // Col 7: Total Time
                cellX += cols[5].second
                val totalTime = calculateWorkDuration(item.checkInTime, item.checkOutTime)
                canvas.drawText(totalTime, cellX, rowY + 17f, paintBold)

                // Divider line below row
                canvas.drawLine(margin, rowY + rowHeight, pageWidth - margin, rowY + rowHeight, paintLine)
                rowY += rowHeight
            }

            // 5. Footer
            val footerY = pageHeight - 30f
            canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, paintLine)

            val paintFooter = Paint().apply {
                color = textMuted
                textSize = 8.5f
                isAntiAlias = true
            }
            canvas.drawText("Bizora Attendance Tracker  |  Verified Audit Record  |  Confidential", margin, footerY + 4f, paintFooter)

            val paintPageNum = Paint().apply {
                color = textMuted
                textSize = 8.5f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            canvas.drawText("Page ${pageIndex + 1} of $pagesCount", pageWidth - margin, footerY + 4f, paintPageNum)

            pdfDoc.finishPage(page)
        }

        FileOutputStream(outputFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
        return ExportedReport(
            file = outputFile,
            uri = uri,
            format = ReportFormat.PDF,
            filename = filename,
            fileSizeFormatted = formatFileSize(outputFile.length()),
            recordCount = roster.size,
            workplaceName = workplaceName,
            generatedDate = nowFormatted
        )
    }

    /**
     * Builds a genuine CSV (.csv) file with UTF-8 BOM and returns the ExportedReport
     */
    fun exportToCsv(
        context: Context,
        workplaceName: String,
        roster: List<RosterItem>,
        date: LocalDate = LocalDate.now()
    ): ExportedReport {
        val dateStamp = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val filename = "Bizora_Report_$dateStamp.csv"
        val outputFile = File(getReportsDir(context), filename)
        val nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
        val dateFormatted = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

        val total = roster.size
        val present = roster.count { it.status == AttendanceStatus.PRESENT }
        val absent = roster.count { it.status == AttendanceStatus.ABSENT }
        val pending = roster.count { it.status == AttendanceStatus.PENDING }
        val rate = if (total > 0) (present * 100) / total else 0

        outputFile.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            // UTF-8 BOM for Excel and spreadsheet viewers
            writer.write("\uFEFF")
            writer.write("Bizora Attendance Report\n")
            writer.write("Workplace,\"${escapeCsv(workplaceName)}\"\n")
            writer.write("Date,\"$dateFormatted\"\n")
            writer.write("Generated,\"$nowFormatted\"\n")
            writer.write("Summary,\"Total: $total | Present: $present | Absent: $absent | Pending: $pending | Rate: $rate%\"\n")
            writer.write("\n")
            writer.write("Employee Code,Employee Name,Date,Status,Check-In Time,Check-Out Time,Total Work Time,Approved At\n")
            for (item in roster) {
                val statusText = when (item.status) {
                    AttendanceStatus.PRESENT -> "PRESENT"
                    AttendanceStatus.ABSENT -> "ABSENT"
                    AttendanceStatus.HALF_DAY -> "HALF DAY"
                    AttendanceStatus.LEAVE -> "ON LEAVE"
                    AttendanceStatus.PENDING -> "PENDING"
                    AttendanceStatus.NOT_MARKED -> "NOT MARKED"
                }
                val totalTime = calculateWorkDuration(item.checkInTime, item.checkOutTime)
                writer.write(
                    "\"${escapeCsv(item.employeeCode)}\"," +
                    "\"${escapeCsv(item.name)}\"," +
                    "\"$dateFormatted\"," +
                    "\"$statusText\"," +
                    "\"${escapeCsv(item.checkInTime ?: "-")}\"," +
                    "\"${escapeCsv(item.checkOutTime ?: "-")}\"," +
                    "\"$totalTime\"," +
                    "\"${escapeCsv(item.approvedAt ?: "-")}\"\n"
                )
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
        return ExportedReport(
            file = outputFile,
            uri = uri,
            format = ReportFormat.CSV,
            filename = filename,
            fileSizeFormatted = formatFileSize(outputFile.length()),
            recordCount = roster.size,
            workplaceName = workplaceName,
            generatedDate = nowFormatted
        )
    }

    /**
     * Builds an individual employee audit report with daily work times and grand total hours worked.
     */
    /**
     * Builds an individual employee audit report in the chosen format (Excel .xlsx, PDF .pdf, or CSV .csv).
     */
    fun exportIndividualEmployeeReport(
        context: Context,
        workplaceName: String,
        employeeName: String,
        employeeCode: String,
        rosterHistory: List<RosterItem>,
        date: LocalDate = LocalDate.now(),
        format: ReportFormat = ReportFormat.EXCEL
    ): ExportedReport {
        val dateStamp = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val safeEmp = sanitizeName(employeeCode.ifBlank { "EMP" })
        val filename = "Bizora_${safeEmp}_$dateStamp.${format.extension}"
        val outputFile = File(getReportsDir(context), filename)
        val nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))

        val total = rosterHistory.size
        val present = rosterHistory.count { it.status == AttendanceStatus.PRESENT }

        var totalMinutesWorked = 0
        for (item in rosterHistory) {
            val inMin = parseTimeToMinutes(item.checkInTime ?: "")
            val outMin = parseTimeToMinutes(item.checkOutTime ?: "")
            if (inMin != null && outMin != null && outMin >= inMin) {
                totalMinutesWorked += (outMin - inMin)
            }
        }
        val grandTotalHours = "${totalMinutesWorked / 60}h ${totalMinutesWorked % 60}m"

        when (format) {
            ReportFormat.CSV -> {
                outputFile.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                    writer.write("\uFEFF")
                    writer.write("Bizora Individual Attendance Report\n")
                    writer.write("Workplace,\"${escapeCsv(workplaceName)}\"\n")
                    writer.write("Employee Name,\"${escapeCsv(employeeName)}\"\n")
                    writer.write("Employee Code,\"${escapeCsv(employeeCode)}\"\n")
                    writer.write("Generated,\"$nowFormatted\"\n")
                    writer.write("Total Days Present,\"$present / $total\"\n")
                    writer.write("Grand Total Work Time,\"$grandTotalHours\"\n")
                    writer.write("\n")
                    writer.write("Date,Status,Check-In Time,Check-Out Time,Total Work Time,Approved At\n")
                    for ((idx, item) in rosterHistory.withIndex()) {
                        val statusText = when (item.status) {
                            AttendanceStatus.PRESENT -> "PRESENT"
                            AttendanceStatus.ABSENT -> "ABSENT"
                            AttendanceStatus.HALF_DAY -> "HALF DAY"
                            AttendanceStatus.LEAVE -> "ON LEAVE"
                            AttendanceStatus.PENDING -> "PENDING"
                            AttendanceStatus.NOT_MARKED -> "NOT MARKED"
                        }
                        val rowDate = date.minusDays(idx.toLong()).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        val duration = calculateWorkDuration(item.checkInTime, item.checkOutTime)
                        writer.write(
                            "\"$rowDate\"," +
                            "\"$statusText\"," +
                            "\"${escapeCsv(item.checkInTime ?: "-")}\"," +
                            "\"${escapeCsv(item.checkOutTime ?: "-")}\"," +
                            "\"$duration\"," +
                            "\"${escapeCsv(item.approvedAt ?: "-")}\"\n"
                        )
                    }
                }
            }

            ReportFormat.PDF -> {
                val pdfDocument = PdfDocument()
                val pageWidth = 595
                val pageHeight = 842
                val margin = 36f
                val contentWidth = pageWidth - 2 * margin
                val rowHeight = 24f

                val paintBrand = Paint().apply {
                    color = Color.parseColor("#5B692D")
                    isAntiAlias = true
                }
                val paintBrandLight = Paint().apply {
                    color = Color.parseColor("#F4F7EE")
                    isAntiAlias = true
                }
                val paintDark = Paint().apply {
                    color = Color.parseColor("#17202A")
                    isAntiAlias = true
                }
                val paintMuted = Paint().apply {
                    color = Color.parseColor("#64748B")
                    isAntiAlias = true
                }
                val paintLine = Paint().apply {
                    color = Color.parseColor("#E2E8F0")
                    strokeWidth = 1f
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Header
                canvas.drawRoundRect(RectF(margin, margin, margin + 32f, margin + 32f), 8f, 8f, paintBrand)
                val paintIcon = Paint().apply {
                    color = Color.WHITE
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("B", margin + 16f, margin + 22f, paintIcon)

                val paintTitle = Paint().apply {
                    color = paintDark.color
                    textSize = 15f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("BIZORA EMPLOYEE AUDIT REPORT", margin + 40f, margin + 18f, paintTitle)

                val paintSub = Paint().apply {
                    color = paintMuted.color
                    textSize = 9.5f
                    isAntiAlias = true
                }
                canvas.drawText("$workplaceName  |  Generated $nowFormatted", margin + 40f, margin + 30f, paintSub)
                canvas.drawLine(margin, margin + 40f, pageWidth - margin, margin + 40f, paintLine)

                // Employee summary card
                var curY = margin + 50f
                canvas.drawRoundRect(RectF(margin, curY, pageWidth - margin, curY + 46f), 8f, 8f, paintBrandLight)
                canvas.drawRoundRect(RectF(margin, curY, pageWidth - margin, curY + 46f), 8f, 8f, paintLine)

                val paintEmpName = Paint().apply {
                    color = paintDark.color
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText(employeeName, margin + 12f, curY + 18f, paintEmpName)

                val paintEmpMeta = Paint().apply {
                    color = paintMuted.color
                    textSize = 9f
                    isAntiAlias = true
                }
                canvas.drawText("Employee Code: $employeeCode   |   Total Days Logged: $total", margin + 12f, curY + 34f, paintEmpMeta)

                val paintGrandTotal = Paint().apply {
                    color = paintBrand.color
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.drawText("Total Worked: $grandTotalHours", pageWidth - margin - 12f, curY + 20f, paintGrandTotal)
                val paintPresentCount = Paint().apply {
                    color = Color.parseColor("#2E7D32")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.drawText("Present: $present / $total days", pageWidth - margin - 12f, curY + 34f, paintPresentCount)

                curY += 58f

                // Table Header
                canvas.drawRect(RectF(margin, curY, pageWidth - margin, curY + rowHeight), paintBrand)
                val paintTh = Paint().apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("DATE", margin + 8f, curY + 16f, paintTh)
                canvas.drawText("STATUS", margin + 110f, curY + 16f, paintTh)
                canvas.drawText("CHECK-IN", margin + 220f, curY + 16f, paintTh)
                canvas.drawText("CHECK-OUT", margin + 310f, curY + 16f, paintTh)
                canvas.drawText("WORK DURATION", margin + 410f, curY + 16f, paintTh)

                curY += rowHeight

                // Table rows
                val paintTd = Paint().apply {
                    color = paintDark.color
                    textSize = 9f
                    isAntiAlias = true
                }
                val paintStriped = Paint().apply {
                    color = Color.parseColor("#F8FAFC")
                }

                for ((idx, item) in rosterHistory.take(24).withIndex()) {
                    if (idx % 2 == 1) {
                        canvas.drawRect(RectF(margin, curY, pageWidth - margin, curY + rowHeight), paintStriped)
                    }
                    canvas.drawLine(margin, curY + rowHeight, pageWidth - margin, curY + rowHeight, paintLine)

                    val rowDate = date.minusDays(idx.toLong()).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    val statusText = when (item.status) {
                        AttendanceStatus.PRESENT -> "PRESENT"
                        AttendanceStatus.ABSENT -> "ABSENT"
                        AttendanceStatus.HALF_DAY -> "HALF DAY"
                        AttendanceStatus.LEAVE -> "LEAVE"
                        AttendanceStatus.PENDING -> "PENDING"
                        AttendanceStatus.NOT_MARKED -> "NOT MARKED"
                    }
                    val duration = calculateWorkDuration(item.checkInTime, item.checkOutTime)

                    canvas.drawText(rowDate, margin + 8f, curY + 16f, paintTd)
                    canvas.drawText(statusText, margin + 110f, curY + 16f, paintTd)
                    canvas.drawText(item.checkInTime ?: "-", margin + 220f, curY + 16f, paintTd)
                    canvas.drawText(item.checkOutTime ?: "-", margin + 310f, curY + 16f, paintTd)
                    canvas.drawText(duration, margin + 410f, curY + 16f, paintTd)

                    curY += rowHeight
                }

                // Footer
                val paintFooter = Paint().apply {
                    color = paintMuted.color
                    textSize = 8.5f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("Generated securely via Bizora Attendance Tracker  •  Confidential & Proprietary", (pageWidth / 2).toFloat(), pageHeight - 20f, paintFooter)

                pdfDocument.finishPage(page)
                FileOutputStream(outputFile).use { fos ->
                    pdfDocument.writeTo(fos)
                }
                pdfDocument.close()
            }

            ReportFormat.EXCEL -> {
                FileOutputStream(outputFile).use { fos ->
                    ZipOutputStream(fos).use { zos ->
                        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                        zos.write(
                            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                        )
                        zos.closeEntry()

                        zos.putNextEntry(ZipEntry("_rels/.rels"))
                        zos.write(
                            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                        )
                        zos.closeEntry()

                        zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
                        zos.write(
                            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                        )
                        zos.closeEntry()

                        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
                        zos.write(
                            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Attendance" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                        )
                        zos.closeEntry()

                        zos.putNextEntry(ZipEntry("xl/styles.xml"))
                        zos.write(
                            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="15"/><color rgb="FF48562A"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF222222"/><name val="Calibri"/></font>
  </fonts>
  <fills count="5">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF48562A"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF4F7EE"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE8EEDF"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD5DEC9"/></left>
      <right style="thin"><color rgb="FFD5DEC9"/></right>
      <top style="thin"><color rgb="FFD5DEC9"/></top>
      <bottom style="thin"><color rgb="FFD5DEC9"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="7">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="3" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/>
  </cellXfs>
</styleSheet>""".trimIndent().toByteArray(StandardCharsets.UTF_8)
                        )
                        zos.closeEntry()

                        // xl/worksheets/sheet1.xml
                        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                        val sb = StringBuilder()
                        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
                        sb.append("""<cols>""")
                        sb.append("""<col min="1" max="1" width="18" customWidth="1"/>""")
                        sb.append("""<col min="2" max="2" width="16" customWidth="1"/>""")
                        sb.append("""<col min="3" max="3" width="16" customWidth="1"/>""")
                        sb.append("""<col min="4" max="4" width="16" customWidth="1"/>""")
                        sb.append("""<col min="5" max="5" width="20" customWidth="1"/>""")
                        sb.append("""<col min="6" max="6" width="22" customWidth="1"/>""")
                        sb.append("""</cols>""")
                        sb.append("""<sheetData>""")

                        // Row 1: Title
                        sb.append("""<row r="1">""")
                        sb.append("""<c r="A1" t="inlineStr" s="1"><is><t>Bizora - Employee Attendance Audit</t></is></c>""")
                        sb.append("""</row>""")

                        // Row 2: Workplace & Employee
                        sb.append("""<row r="2">""")
                        sb.append("""<c r="A2" t="inlineStr" s="5"><is><t>Workplace: ${escapeXml(workplaceName)}</t></is></c>""")
                        sb.append("""<c r="C2" t="inlineStr" s="5"><is><t>Employee: ${escapeXml(employeeName)} (${escapeXml(employeeCode)})</t></is></c>""")
                        sb.append("""</row>""")

                        // Row 3: Totals
                        sb.append("""<row r="3">""")
                        sb.append("""<c r="A3" t="inlineStr" s="3"><is><t>Generated: $nowFormatted</t></is></c>""")
                        sb.append("""<c r="C3" t="inlineStr" s="3"><is><t>Present: $present / $total days</t></is></c>""")
                        sb.append("""<c r="E3" t="inlineStr" s="3"><is><t>Grand Total: $grandTotalHours</t></is></c>""")
                        sb.append("""</row>""")

                        // Row 5: Table Headers
                        sb.append("""<row r="5">""")
                        sb.append("""<c r="A5" t="inlineStr" s="2"><is><t>Date</t></is></c>""")
                        sb.append("""<c r="B5" t="inlineStr" s="2"><is><t>Status</t></is></c>""")
                        sb.append("""<c r="C5" t="inlineStr" s="2"><is><t>Check-In</t></is></c>""")
                        sb.append("""<c r="D5" t="inlineStr" s="2"><is><t>Check-Out</t></is></c>""")
                        sb.append("""<c r="E5" t="inlineStr" s="2"><is><t>Total Work Duration</t></is></c>""")
                        sb.append("""<c r="F5" t="inlineStr" s="2"><is><t>Approved At</t></is></c>""")
                        sb.append("""</row>""")

                        var rowNum = 6
                        for ((idx, item) in rosterHistory.withIndex()) {
                            val rowDate = date.minusDays(idx.toLong()).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                            val statusText = when (item.status) {
                                AttendanceStatus.PRESENT -> "PRESENT"
                                AttendanceStatus.ABSENT -> "ABSENT"
                                AttendanceStatus.HALF_DAY -> "HALF DAY"
                                AttendanceStatus.LEAVE -> "ON LEAVE"
                                AttendanceStatus.PENDING -> "PENDING"
                                AttendanceStatus.NOT_MARKED -> "NOT MARKED"
                            }
                            val duration = calculateWorkDuration(item.checkInTime, item.checkOutTime)
                            val styleIdx = if (rowNum % 2 == 0) "4" else "3"

                            sb.append("""<row r="$rowNum">""")
                            sb.append("""<c r="A$rowNum" t="inlineStr" s="$styleIdx"><is><t>${escapeXml(rowDate)}</t></is></c>""")
                            sb.append("""<c r="B$rowNum" t="inlineStr" s="$styleIdx"><is><t>${escapeXml(statusText)}</t></is></c>""")
                            sb.append("""<c r="C$rowNum" t="inlineStr" s="$styleIdx"><is><t>${escapeXml(item.checkInTime ?: "-")}</t></is></c>""")
                            sb.append("""<c r="D$rowNum" t="inlineStr" s="$styleIdx"><is><t>${escapeXml(item.checkOutTime ?: "-")}</t></is></c>""")
                            sb.append("""<c r="E$rowNum" t="inlineStr" s="$styleIdx"><is><t>${escapeXml(duration)}</t></is></c>""")
                            sb.append("""<c r="F$rowNum" t="inlineStr" s="$styleIdx"><is><t>${escapeXml(item.approvedAt ?: "-")}</t></is></c>""")
                            sb.append("""</row>""")
                            rowNum++
                        }

                        sb.append("""</sheetData>""")
                        sb.append("""</worksheet>""")
                        zos.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
                        zos.closeEntry()
                    }
                }
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
        return ExportedReport(
            file = outputFile,
            uri = uri,
            format = format,
            filename = filename,
            fileSizeFormatted = formatFileSize(outputFile.length()),
            recordCount = rosterHistory.size,
            workplaceName = workplaceName,
            generatedDate = nowFormatted
        )
    }

    /**
     * Saves the exported report directly into the device's public Downloads directory.
     * Compatible with Android 10+ Scoped Storage (API 29+) and legacy storage.
     */
    fun saveToDevice(context: Context, report: ExportedReport): Result<String> {
        return try {
            val filename = report.filename
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, report.format.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Bizora")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(Exception("Could not create download entry"))

                resolver.openOutputStream(uri)?.use { outputStream ->
                    report.file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    outputStream.flush()
                } ?: return Result.failure(Exception("Could not open storage stream"))

                Result.success("Downloads/Bizora/$filename")
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val bizoraDir = File(downloads, "Bizora").apply { if (!exists()) mkdirs() }
                val targetFile = File(bizoraDir, filename)
                report.file.copyTo(targetFile, overwrite = true)
                Result.success("Downloads/Bizora/$filename")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Copies report data to a user-selected URI (from Storage Access Framework CreateDocument).
     */
    fun saveToUri(context: Context, report: ExportedReport, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                report.file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shares the exported file using the system share chooser
     */
    fun shareReport(context: Context, report: ExportedReport) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = report.format.mimeType
            putExtra(Intent.EXTRA_STREAM, report.uri)
            putExtra(Intent.EXTRA_SUBJECT, "${report.workplaceName} Attendance Report (${report.format.extension.uppercase()})")
            putExtra(
                Intent.EXTRA_TEXT,
                "Bizora Attendance Report for ${report.workplaceName}\nGenerated: ${report.generatedDate}\nRecords: ${report.recordCount} entries"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share ${report.format.displayName}"))
    }

    /**
     * Opens the exported file with the user's default app (e.g. Excel or PDF viewer)
     */
    fun openReport(context: Context, report: ExportedReport) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(report.uri, report.format.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(viewIntent, "Open with"))
    }

    private fun escapeCsv(text: String): String {
        return text.replace("\"", "\"\"")
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
