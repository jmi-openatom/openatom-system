package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestDataOpenApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPublicLoginDTO;
import edu.jmi.openatom.server.openatomsystem.service.DataOpenApplicationService;
import edu.jmi.openatom.server.openatomsystem.service.PublicService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseDataOpenApplicationVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePublicLoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

	private final PublicService publicService;
	private final DataOpenApplicationService dataOpenApplicationService;

	@PostMapping("/login")
	public Result<ResponsePublicLoginVO> login(
			@RequestBody(required = false) RequestPublicLoginDTO requestPublicLoginDTO,
			HttpServletRequest request){
		return publicService.login(requestPublicLoginDTO, resolveApiKey(request));
	}

	@PostMapping("/data-open/applications")
	public Result<ResponseDataOpenApplicationVO> submitDataOpenApplication(
			@Valid @RequestBody RequestDataOpenApplicationDTO request) {
		return dataOpenApplicationService.submit(request);
	}

	@GetMapping("/data-open/applications/{applicationId}")
	public Result<ResponseDataOpenApplicationVO> dataOpenApplicationDetail(
			@PathVariable Integer applicationId,
			@RequestParam String applicantContact) {
		return dataOpenApplicationService.publicDetail(applicationId, applicantContact);
	}

	private String resolveApiKey(HttpServletRequest request) {
		String value = trimToNull(request.getHeader("X-Openatom-Data-Key"));
		if (value != null) return value;
		value = trimToNull(request.getHeader("X-Data-Open-Key"));
		if (value != null) return value;
		return trimToNull(request.getHeader("openatom-data-key"));
	}

	private String trimToNull(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
