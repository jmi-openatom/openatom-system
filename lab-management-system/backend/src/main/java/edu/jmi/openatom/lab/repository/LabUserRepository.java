package edu.jmi.openatom.lab.repository;

import edu.jmi.openatom.lab.entity.LabUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LabUserRepository extends JpaRepository<LabUser, Long> {
    Optional<LabUser> findByClubUserId(Long clubUserId);
}
