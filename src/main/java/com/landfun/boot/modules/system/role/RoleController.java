package com.landfun.boot.modules.system.role;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.BasePageQuery;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.infrastructure.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @HasPermission("sys:role:list")
    public R<PageResult<Role>> list(BasePageQuery query) {
        return R.ok(roleService.page(query));
    }

    @GetMapping("/{id}")
    @HasPermission("sys:role:query")
    public R<Role> get(@PathVariable long id) {
        return R.ok(roleService.findById(id));
    }

    @GetMapping("/all")
    public R<java.util.List<Role>> listAll() {
        return R.ok(roleService.listAll());
    }

    @PostMapping
    @HasPermission("sys:role:add")
    public R<Long> create(@RequestBody @Valid RoleInput input) {
        return R.ok(roleService.save(input));
    }

    @PutMapping
    @HasPermission("sys:role:update")
    public R<Long> update(@RequestBody @Valid RoleInput input) {
        return R.ok(roleService.save(input));
    }

    @DeleteMapping("/{id}")
    @HasPermission("sys:role:delete")
    public R<Void> delete(@PathVariable long id) {
        roleService.delete(id);
        return R.ok(null);
    }
}
