package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.service.impl.AvatarStorageServiceImpl;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface IAvatarStorageService {
	AvatarStorageServiceImpl.StoredAvatar store(MultipartFile file) throws IOException;

	Optional<AvatarStorageServiceImpl.AvatarResource> load(String fileName) throws IOException;

	boolean isManagedAvatarUrl(String avatarUrl);

	boolean existsByAvatarUrl(String avatarUrl);

	void deleteByAvatarUrl(String avatarUrl);
}
