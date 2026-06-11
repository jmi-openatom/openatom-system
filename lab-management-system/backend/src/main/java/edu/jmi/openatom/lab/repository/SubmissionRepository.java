package edu.jmi.openatom.lab.repository;

import edu.jmi.openatom.lab.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdAndProblemIdOrderByCreatedAtDesc(Long userId, Long problemId);
    List<Submission> findByUserIdOrderByCreatedAtDesc(Long userId);
}
