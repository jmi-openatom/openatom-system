package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateUnifiedGroupDTO;
import edu.jmi.openatom.server.openatomsystem.service.CheckInService;
import edu.jmi.openatom.server.openatomsystem.service.DepartmentService;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class UnifiedGroupServiceImplTest {
  @Test
  void createsCheckInGroupInRequestedClub() {
    AtomicReference<Object[]> invocation = new AtomicReference<>();
    CheckInService checkInService = proxy(
        CheckInService.class,
        "createGroup",
        arguments -> {
          invocation.set(arguments);
          return Result.success(42);
        });
    UnifiedGroupServiceImpl service = service(checkInService, null);
    RequestCreateUnifiedGroupDTO request = request("checkin");
    request.setUserIds(List.of(2, 3));

    Result<String> result = service.create(request);

    assertEquals(Result.SUCCESS_CODE, result.getCode());
    assertNotNull(invocation.get());
    assertEquals(8, invocation.get()[0]);
    assertEquals(List.of(2, 3), ((RequestCheckInGroupDTO) invocation.get()[1]).getUserIds());
  }

  @Test
  void delegatesDepartmentCreationToCompatibilityAdapter() {
    AtomicReference<Object[]> invocation = new AtomicReference<>();
    DepartmentService departmentService = proxy(
        DepartmentService.class,
        "createDepartment",
        arguments -> {
          invocation.set(arguments);
          return Result.success("部门创建成功");
        });
    UnifiedGroupServiceImpl service = service(null, departmentService);

    Result<String> result = service.create(request("department"));

    assertEquals(Result.SUCCESS_CODE, result.getCode());
    assertNotNull(invocation.get());
    assertEquals(8, invocation.get()[0]);
  }

  @Test
  void rejectsUnsupportedGroupType() {
    UnifiedGroupServiceImpl service = service(null, null);

    Result<String> result = service.create(request("unknown"));

    assertEquals(400, result.getCode());
  }

  private UnifiedGroupServiceImpl service(
      CheckInService checkInService, DepartmentService departmentService) {
    return new UnifiedGroupServiceImpl(
        null, null, departmentService, checkInService, null);
  }

  private RequestCreateUnifiedGroupDTO request(String type) {
    RequestCreateUnifiedGroupDTO request = new RequestCreateUnifiedGroupDTO();
    request.setClubId(8);
    request.setType(type);
    request.setName("测试分组");
    request.setDescription("统一分组测试");
    return request;
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(
      Class<T> type, String supportedMethod, Invocation invocation) {
    return (T) Proxy.newProxyInstance(
        type.getClassLoader(),
        new Class<?>[] {type},
        (instance, method, arguments) -> {
          if (method.getName().equals(supportedMethod)) return invocation.invoke(arguments);
          if (method.getName().equals("toString")) return type.getSimpleName() + "TestDouble";
          throw new UnsupportedOperationException(method.getName());
        });
  }

  @FunctionalInterface
  private interface Invocation {
    Object invoke(Object[] arguments);
  }
}
