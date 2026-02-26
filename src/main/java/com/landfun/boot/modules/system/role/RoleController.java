package com.landfun.boot.modules.system.role;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.infrastructure.web.R;
import com.landfun.boot.modules.system.role.dto.RoleInput;
import com.landfun.boot.modules.system.role.dto.RoleSpecification;
import com.landfun.boot.modules.system.role.dto.RoleView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Role", description = "Role Management APIs")
@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Get Role List Page")
    @GetMapping
    @HasPermission("sys:role:list")
    public R<PageResult<RoleView>> list(RoleSpecification spec, @PageableDefault Pageable pageable) {
        return R.ok(roleService.page(spec, pageable));
    }

    @Operation(summary = "Get Role by ID")
    @GetMapping("/{id}")
    @HasPermission("sys:role:query")
    public R<RoleView> get(@PathVariable long id) {
        return R.ok(roleService.findById(id));
    }

    @Operation(summary = "Get All Roles")
    @GetMapping("/all")
    public R<java.util.List<RoleView>> listAll(RoleSpecification spec, @PageableDefault Pageable pageable) {
        return R.ok(roleService.listAll(spec, pageable));
    }

    @Operation(summary = "Create Role")
    @PostMapping
    @HasPermission("sys:role:add")
    public R<RoleView> create(@RequestBody @Valid RoleInput input) {
        return R.ok(roleService.save(input));
    }

    @Operation(summary = "Update Role")
    @PutMapping
    @HasPermission("sys:role:update")
    public R<RoleView> update(@RequestBody @Valid RoleInput input) {
        return R.ok(roleService.save(input));
    }

    @Operation(summary = "Delete Role")
    @DeleteMapping("/{id}")
    @HasPermission("sys:role:delete")
    public R<Void> delete(@PathVariable long id) {
        roleService.delete(id);
        return R.ok(null);
    }
}
