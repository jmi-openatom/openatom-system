package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.service.AvatarStorageService;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publicly readable static assets served by the backend. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {
  private final AvatarStorageService avatarStorageService;

  @GetMapping("/avatars/{fileName:.+}")
  public ResponseEntity<?> avatar(@PathVariable String fileName) throws IOException {
    return avatarStorageService
        .load(fileName)
        .<ResponseEntity<?>>map(
            avatar ->
                ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
                    .contentType(avatar.getMediaType())
                    .body(avatar.getResource()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
