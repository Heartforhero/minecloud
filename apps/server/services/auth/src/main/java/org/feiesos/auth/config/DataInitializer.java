package org.feiesos.auth.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.feiesos.auth.entity.RolePermission;
import org.feiesos.auth.entity.SysPermission;
import org.feiesos.auth.entity.SysRole;
import org.feiesos.auth.mapper.RolePermissionMapper;
import org.feiesos.auth.mapper.SysPermissionMapper;
import org.feiesos.auth.mapper.SysRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public DataInitializer(SysPermissionMapper permissionMapper,
                           SysRoleMapper roleMapper,
                           RolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPermissions();
        seedRoles();
        seedRolePermissions();
    }

    private void seedPermissions() {
        seedPermission("file:read", "文件读取");
        seedPermission("file:write", "文件写入");
        seedPermission("file:delete", "文件删除");
    }

    private void seedPermission(String code, String name) {
        if (permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getCode, code)) == 0) {
            SysPermission p = new SysPermission();
            p.setCode(code);
            p.setName(name);
            permissionMapper.insert(p);
            log.info("已创建权限: {}", code);
        }
    }

    private void seedRoles() {
        if (roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "user")) == 0) {
            SysRole role = new SysRole();
            role.setCode("user");
            role.setName("普通用户");
            roleMapper.insert(role);
            log.info("已创建角色: user");
        }
        if (roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "admin")) == 0) {
            SysRole role = new SysRole();
            role.setCode("admin");
            role.setName("管理员");
            roleMapper.insert(role);
            log.info("已创建角色: admin");
        }
    }

    private void seedRolePermissions() {
        SysRole userRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "user"));
        SysRole adminRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "admin"));

        List<SysPermission> allPermissions = permissionMapper.selectList(new LambdaQueryWrapper<>());

        for (SysPermission perm : allPermissions) {
            if (userRole != null && rolePermissionMapper.selectCount(
                    new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, userRole.getId())
                            .eq(RolePermission::getPermissionId, perm.getId())) == 0) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(userRole.getId());
                rp.setPermissionId(perm.getId());
                rolePermissionMapper.insert(rp);
            }
            if (adminRole != null && rolePermissionMapper.selectCount(
                    new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, adminRole.getId())
                            .eq(RolePermission::getPermissionId, perm.getId())) == 0) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(adminRole.getId());
                rp.setPermissionId(perm.getId());
                rolePermissionMapper.insert(rp);
            }
        }
        log.info("角色-权限关联已初始化");
    }
}
