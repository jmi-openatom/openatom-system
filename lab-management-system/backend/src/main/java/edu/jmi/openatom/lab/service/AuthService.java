package edu.jmi.openatom.lab.service;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.lab.dto.CMSUserInfo;
import edu.jmi.openatom.lab.entity.LabUser;
import edu.jmi.openatom.lab.repository.LabUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final LabUserRepository labUserRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${lab.cms.user-info-url}")
    private String cmsUserInfoUrl;

    public LabUser loginWithCMSToken(String cmsToken) {
        // 从 CMS 获取用户信息
        CMSUserInfo cmsUser = fetchCMSUserInfo(cmsToken);

        if (cmsUser == null || !Boolean.TRUE.equals(cmsUser.getIsLabMember())) {
            throw new RuntimeException("您当前非实验室正式成员，请联系管理员在社团管理系统开通权限");
        }

        // 查找或创建本地用户
        LabUser labUser = labUserRepository.findByClubUserId(cmsUser.getId())
            .orElseGet(() -> {
                LabUser newUser = new LabUser();
                newUser.setClubUserId(cmsUser.getId());
                return newUser;
            });

        // 更新用户信息
        labUser.setUsername(cmsUser.getUsername());
        labUser.setEmail(cmsUser.getEmail());
        labUser.setAvatar(cmsUser.getAvatar());
        labUserRepository.save(labUser);

        // 生成 LMS Token
        StpUtil.login(labUser.getId());

        return labUser;
    }

    private CMSUserInfo fetchCMSUserInfo(String token) {
        try {
            return restTemplate.getForObject(
                cmsUserInfoUrl + "?token=" + token,
                CMSUserInfo.class
            );
        } catch (Exception e) {
            throw new RuntimeException("无法从CMS获取用户信息: " + e.getMessage());
        }
    }

    public LabUser getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return labUserRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
