package com.shop.module.system.controller;

import com.shop.common.exception.ServerException;
import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.framework.security.LoginUser;
import com.shop.module.system.service.SystemAdministrationService;
import com.shop.module.system.vo.AdminUserPasswordReqVO;
import com.shop.module.system.vo.AdminUserRespVO;
import com.shop.module.system.vo.AdminUserSaveReqVO;
import com.shop.module.system.vo.AdminUserStatusReqVO;
import com.shop.module.system.vo.ChangePasswordReqVO;
import com.shop.module.system.vo.LoginLogRespVO;
import com.shop.module.system.vo.OperationLogRespVO;
import com.shop.module.system.vo.PermissionRespVO;
import com.shop.module.system.vo.RoleRespVO;
import com.shop.module.system.vo.RoleSaveReqVO;
import com.shop.module.system.vo.RoleStatusReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/system")
@RequiredArgsConstructor
public class SystemAdministrationController {

    private final SystemAdministrationService administrationService;

    @GetMapping("/admin-user/page")
    public CommonResult<PageResult<AdminUserRespVO>> userPage(PageParam pageParam,
            @RequestParam(required = false) String username, @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer status) {
        administrationService.requireSuperAdmin(currentAdminId());
        return CommonResult.success(administrationService.getUserPage(pageParam, username, nickname, status));
    }

    @GetMapping("/admin-user/detail")
    public CommonResult<AdminUserRespVO> userDetail(@RequestParam Long id) {
        administrationService.requireSuperAdmin(currentAdminId());
        return CommonResult.success(administrationService.getUser(id));
    }

    @PostMapping("/admin-user/save")
    public CommonResult<Long> saveUser(@RequestBody AdminUserSaveReqVO request) {
        administrationService.requireSuperAdmin(currentAdminId());
        return CommonResult.success(administrationService.saveUser(request));
    }

    @PostMapping("/admin-user/status")
    public CommonResult<Boolean> changeUserStatus(@RequestBody AdminUserStatusReqVO request) {
        Long adminId = currentAdminId(); administrationService.requireSuperAdmin(adminId);
        administrationService.changeUserStatus(adminId, request == null ? null : request.getId(), request == null ? null : request.getStatus());
        return CommonResult.success(true);
    }

    @PostMapping("/admin-user/unlock")
    public CommonResult<Boolean> unlockUser(@RequestBody AdminUserStatusReqVO request) {
        administrationService.requireSuperAdmin(currentAdminId()); administrationService.unlockUser(request == null ? null : request.getId());
        return CommonResult.success(true);
    }

    @PostMapping("/admin-user/reset-password")
    public CommonResult<Boolean> resetPassword(@RequestBody AdminUserPasswordReqVO request) {
        administrationService.requireSuperAdmin(currentAdminId()); administrationService.resetPassword(request);
        return CommonResult.success(true);
    }

    @PostMapping("/admin-user/force-logout")
    public CommonResult<Boolean> forceLogout(@RequestBody AdminUserStatusReqVO request) {
        administrationService.requireSuperAdmin(currentAdminId()); administrationService.forceLogout(request == null ? null : request.getId());
        return CommonResult.success(true);
    }

    @PostMapping("/password/change")
    public CommonResult<Boolean> changeOwnPassword(@RequestBody ChangePasswordReqVO request) {
        Long adminId = currentAdminId();
        if (request == null) throw new ServerException(400, "密码信息不能为空");
        administrationService.changeOwnPassword(adminId, request.getOldPassword(), request.getNewPassword());
        return CommonResult.success(true);
    }

    @GetMapping("/role/list")
    public CommonResult<List<RoleRespVO>> roles() {
        administrationService.requireSuperAdmin(currentAdminId()); return CommonResult.success(administrationService.getRoles());
    }

    @PostMapping("/role/save")
    public CommonResult<Long> saveRole(@RequestBody RoleSaveReqVO request) {
        administrationService.requireSuperAdmin(currentAdminId()); return CommonResult.success(administrationService.saveRole(request));
    }

    @PostMapping("/role/status")
    public CommonResult<Boolean> changeRoleStatus(@RequestBody RoleStatusReqVO request) {
        administrationService.requireSuperAdmin(currentAdminId()); administrationService.changeRoleStatus(request == null ? null : request.getId(), request == null ? null : request.getStatus());
        return CommonResult.success(true);
    }

    @GetMapping("/permission/list")
    public CommonResult<List<PermissionRespVO>> permissions() {
        administrationService.requireSuperAdmin(currentAdminId()); return CommonResult.success(administrationService.getPermissions());
    }

    @GetMapping("/audit/login-page")
    public CommonResult<PageResult<LoginLogRespVO>> loginLogs(PageParam pageParam,
            @RequestParam(required = false) String username, @RequestParam(required = false) Integer success) {
        administrationService.requireSuperAdmin(currentAdminId()); return CommonResult.success(administrationService.getLoginLogs(pageParam, username, success));
    }

    @GetMapping("/audit/operation-page")
    public CommonResult<PageResult<OperationLogRespVO>> operationLogs(PageParam pageParam,
            @RequestParam(required = false) String username, @RequestParam(required = false) String requestUri,
            @RequestParam(required = false) String businessRef, @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Integer highRisk, @RequestParam(required = false) Integer success) {
        administrationService.requireSuperAdmin(currentAdminId()); return CommonResult.success(administrationService.getOperationLogs(pageParam, username, requestUri, businessRef, operationType, highRisk, success));
    }

    private Long currentAdminId() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) throw new ServerException(401, "管理员未登录");
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser user && Integer.valueOf(2).equals(user.getUserType())) return user.getUserId();
        throw new ServerException(401, "管理员未登录");
    }
}
