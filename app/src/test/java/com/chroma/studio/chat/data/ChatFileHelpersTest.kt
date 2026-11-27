package com.chroma.studio.chat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatFileHelpersTest {

    @Test
    fun getFileBadgeVisual_mapsDocumentAndArchiveExtensions() {
        val pdf = getFileBadgeVisual("Contract_Final.pdf")
        assertEquals("PDF", pdf.extensionLabel)
        assertEquals(0xFFFF3B30, pdf.accentColorHex)
        assertEquals("PDF Document", pdf.typeDescription)

        val zip = getFileBadgeVisual("zip")
        assertEquals("ZIP", zip.extensionLabel)
        assertEquals("Archives", zip.category)

        val xlsx = getFileBadgeVisual("budget.xlsx")
        assertEquals("XLSX", xlsx.extensionLabel)
        assertEquals("Sheets", xlsx.category)
    }

    @Test
    fun resolveMimeTypeForFileName_mapsCommonDocumentFormats() {
        assertEquals("application/pdf", resolveMimeTypeForFileName("report.PDF"))
        assertEquals("application/zip", resolveMimeTypeForFileName("bundle.zip"))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            resolveMimeTypeForFileName("notes.docx")
        )
        assertEquals("application/vnd.android.package-archive", resolveMimeTypeForFileName("build.apk"))
        assertEquals("text/plain", resolveMimeTypeForFileName(null, fallbackExt = "txt"))
        assertEquals("*/*", resolveMimeTypeForFileName("unknown_blob"))
    }

    @Test
    fun formatReadableFileSize_formatsBytesOrEmptyWhenUnknown() {
        assertEquals("", formatReadableFileSize(null))
        assertEquals("", formatReadableFileSize(-1L))
        assertEquals("0 B", formatReadableFileSize(0L))
        assertEquals("512 B", formatReadableFileSize(512L))
        assertEquals("42 KB", formatReadableFileSize(42 * 1024L))
        assertEquals("2.5 MB", formatReadableFileSize((2.5 * 1024 * 1024).toLong()))
        assertEquals("1.5 GB", formatReadableFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun sanitizeStorageFileName_stripsUnsafeCharsPreservingExtension() {
        assertEquals("Q4_Report_Final.pdf", sanitizeStorageFileName("Q4 Report (Final).pdf", "bin"))
        assertEquals("document.zip", sanitizeStorageFileName("   ", "zip"))
    }

    @Test
    fun resolveFileMessageNameAndCaption_supportsStructuredAndLegacyPayloads() {
        val (structuredName, structuredCaption) = resolveFileMessageNameAndCaption(
            storedFileName = "DesignSpec.pdf",
            storedText = "Please review pages 2-4"
        )
        assertEquals("DesignSpec.pdf", structuredName)
        assertEquals("Please review pages 2-4", structuredCaption)

        val (sameTextName, sameTextCaption) = resolveFileMessageNameAndCaption(
            storedFileName = "DesignSpec.pdf",
            storedText = "DesignSpec.pdf"
        )
        assertEquals("DesignSpec.pdf", sameTextName)
        assertNull(sameTextCaption)

        val (legacyName, legacyCaption) = resolveFileMessageNameAndCaption(
            storedFileName = null,
            storedText = "Invoice_2026.pdf"
        )
        assertEquals("Invoice_2026.pdf", legacyName)
        assertNull(legacyCaption)
        assertEquals("📄 Invoice_2026.pdf", formatFileRoomPreview(null, "Invoice_2026.pdf"))
    }
}
