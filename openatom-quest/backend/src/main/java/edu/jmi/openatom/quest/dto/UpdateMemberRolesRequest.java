package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateMemberRolesRequest(@NotEmpty Set<String> roles) {
}
