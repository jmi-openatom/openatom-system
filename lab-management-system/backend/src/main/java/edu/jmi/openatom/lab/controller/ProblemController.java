package edu.jmi.openatom.lab.controller;

import edu.jmi.openatom.lab.dto.ApiResponse;
import edu.jmi.openatom.lab.entity.Problem;
import edu.jmi.openatom.lab.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {
    private final ProblemRepository problemRepository;

    @GetMapping("/today")
    public ApiResponse<Problem> getTodayProblem() {
        Problem problem = problemRepository.findByProblemDate(LocalDate.now())
            .orElseThrow(() -> new RuntimeException("今日题目尚未生成"));
        return ApiResponse.success(problem);
    }

    @GetMapping("/{id}")
    public ApiResponse<Problem> getProblem(@PathVariable Long id) {
        Problem problem = problemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("题目不存在"));
        return ApiResponse.success(problem);
    }
}
