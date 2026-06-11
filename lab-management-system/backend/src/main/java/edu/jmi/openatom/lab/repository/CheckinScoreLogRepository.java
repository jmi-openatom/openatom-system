package edu.jmi.openatom.lab.repository;

import edu.jmi.openatom.lab.entity.CheckinScoreLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface CheckinScoreLogRepository extends JpaRepository<CheckinScoreLog, Long> {
    Optional<CheckinScoreLog> findByUserIdAndCheckinDate(Long userId, LocalDate date);
    List<CheckinScoreLog> findByClubSyncStatus(Integer status);
}
