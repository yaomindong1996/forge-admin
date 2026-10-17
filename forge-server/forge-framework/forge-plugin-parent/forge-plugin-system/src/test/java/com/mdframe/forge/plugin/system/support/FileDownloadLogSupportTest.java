package com.mdframe.forge.plugin.system.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDownloadLogSupportTest {

    @Test
    void previewDownloadIsNotRecordedUntilExplicitIntent() {
        String uri = "/api/file/download/abc";
        String disposition = "attachment;filename=a.png";
        assertFalse(FileDownloadLogSupport.shouldRecord(uri, disposition, null, 200));
        assertTrue(FileDownloadLogSupport.shouldRecord(uri, disposition, "1", 200));
    }

    @Test
    void exportAttachmentIsRecordedWithoutIntentHeader() {
        assertTrue(FileDownloadLogSupport.shouldRecord(
                "/system/user/export", "attachment;filename*=utf-8''users.xlsx", null, 200));
        assertFalse(FileDownloadLogSupport.shouldRecord(
                "/system/user/export", "attachment;filename=users.xlsx", null, 500));
        assertTrue(FileDownloadLogSupport.shouldRecord(
                "/system/user/export", "attachment;filename=users.xlsx", null, 0));
    }

    @Test
    void adminsSeeAllWhileOthersStayOnThemselves() {
        assertNull(FileDownloadLogSupport.restrictedUserId(true, 8L));
        assertEquals(8L, FileDownloadLogSupport.restrictedUserId(false, 8L));
    }

    @Test
    void parsesEncodedFileNameAndFileId() {
        assertEquals("报表.xlsx", FileDownloadLogSupport.fileNameOf(
                "attachment;filename*=utf-8''%E6%8A%A5%E8%A1%A8.xlsx"));
        assertEquals("file-1", FileDownloadLogSupport.fileIdOf("/api/file/download/file-1"));
        assertEquals(FileDownloadLogSupport.SOURCE_FILE, FileDownloadLogSupport.sourceOf("/api/file/download/file-1"));
        assertEquals(FileDownloadLogSupport.SOURCE_EXPORT, FileDownloadLogSupport.sourceOf("/system/user/export"));
    }
}
