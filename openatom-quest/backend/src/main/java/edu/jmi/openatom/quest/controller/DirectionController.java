package edu.jmi.openatom.quest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.entity.TechnicalDirection;
import edu.jmi.openatom.quest.mapper.TechnicalDirectionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/directions")
@RequiredArgsConstructor
public class DirectionController {
    private final TechnicalDirectionMapper directionMapper;

    @GetMapping
    public ApiResponse<List<TechnicalDirection>> list() {
        return ApiResponse.ok(directionMapper.selectList(
            new LambdaQueryWrapper<TechnicalDirection>()
                .eq(TechnicalDirection::getStatus, "ACTIVE")
                .orderByAsc(TechnicalDirection::getSortOrder)
        ));
    }
}
