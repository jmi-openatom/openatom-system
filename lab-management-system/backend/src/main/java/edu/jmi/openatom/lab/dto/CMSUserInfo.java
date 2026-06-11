package edu.jmi.openatom.lab.dto;

import lombok.Data;

@Data
public class CMSUserInfo {
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private Boolean isLabMember;
}
