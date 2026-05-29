package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;

import edu.jmi.openatom.server.openatomsystem.dto.RequestPublicLoginDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePublicLoginVO;

public interface PublicService {
	Result<ResponsePublicLoginVO> login(RequestPublicLoginDTO requestPublicLoginDTO, String apiKey);
}
