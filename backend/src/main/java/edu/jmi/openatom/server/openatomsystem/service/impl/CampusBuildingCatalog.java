package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Uses the same stable IDs as the frontend GeoJSON, including the lighthouse. */
@Component
public class CampusBuildingCatalog {
  private final Map<String, Building> buildings;

  public CampusBuildingCatalog(ObjectMapper objectMapper) throws IOException {
    try (var input = new ClassPathResource("campus/buildings.json").getInputStream()) {
      List<Building> rows = objectMapper.readValue(input, new TypeReference<>() {});
      buildings = rows.stream().collect(Collectors.toUnmodifiableMap(Building::id, b -> b));
    }
  }

  public String name(String id) {
    Building building = buildings.get(id);
    if (building == null) throw new IllegalArgumentException("校园建筑不存在");
    return building.name();
  }

  public String description(String id) {
    name(id);
    return buildings.get(id).description();
  }

  public record Building(String id, String name, String description) {}
}
