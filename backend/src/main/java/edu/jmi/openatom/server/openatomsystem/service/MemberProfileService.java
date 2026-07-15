package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveMemberProfileDTO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseImageUploadVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberCardVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberFilterVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface MemberProfileService {
  Result<PageDataVO<ResponseMemberCardVO>> members(
      String keyword, Integer departmentId, String skill, Long page, Long pageSize);

  Result<ResponseMemberFilterVO> filters();

  Result<ResponseMemberProfileVO> detail(String slug);

  Result<ResponseMemberProfileVO> mine();

  Result<ResponseMemberProfileVO> save(RequestSaveMemberProfileDTO request);

  Result<ResponseMemberProfileVO> publish();

  Result<ResponseMemberProfileVO> unpublish();

  Result<ResponseImageUploadVO> upload(String kind, MultipartFile file, String baseUrl);
}
