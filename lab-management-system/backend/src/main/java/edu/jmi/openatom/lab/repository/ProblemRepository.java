package edu.jmi.openatom.lab.repository;

import edu.jmi.openatom.lab.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findByProblemDate(LocalDate date);
}
