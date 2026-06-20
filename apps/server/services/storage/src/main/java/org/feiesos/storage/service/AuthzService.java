package org.feiesos.storage.service;

import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.mapper.PermissionMapper;
import org.springframework.stereotype.Service;

@Service
public class AuthzService {

    private final PermissionMapper permissionMapper;

    public AuthzService(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public void checkPermission(Long userId, String permissionCode) {
        if (userId == null) {
            throw new BusinessException(401, "未认证");
        }
        int count = permissionMapper.countPermission(userId, permissionCode);
        if (count == 0) {
            throw new BusinessException(403, "无权限执行该操作: " + permissionCode);
        }
    }
}
