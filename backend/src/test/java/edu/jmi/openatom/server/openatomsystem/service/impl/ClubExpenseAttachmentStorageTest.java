package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ClubExpenseAttachmentStorageTest {
  @TempDir Path directory;

  @Test
  void storesValidReceiptPrivatelyAndRejectsForgedType() throws Exception {
    ClubExpenseAttachmentStorage storage = new ClubExpenseAttachmentStorage();
    ReflectionTestUtils.setField(storage, "storageDir", directory.toString());
    var pdf = new MockMultipartFile("file", "发票.pdf", "application/pdf", "%PDF-1.7\n".getBytes());

    var saved = storage.store(pdf);

    assertEquals("application/pdf", saved.mimeType());
    assertTrue(storage.load(saved.storageName()).exists());
    assertThrows(IllegalArgumentException.class,
        () -> storage.store(new MockMultipartFile("file", "假票据.pdf", "application/pdf", "not-a-pdf".getBytes())));
    assertThrows(Exception.class, () -> storage.load("../other.pdf"));
  }
}
