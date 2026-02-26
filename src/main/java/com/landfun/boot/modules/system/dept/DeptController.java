package com.landfun.boot.modules.system.dept;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.IdInput;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.infrastructure.web.R;
import com.landfun.boot.modules.system.dept.dto.DeptInput;
import com.landfun.boot.modules.system.dept.dto.DeptSpecification;
import com.landfun.boot.modules.system.dept.dto.DeptView;
import com.landfun.boot.modules.system.dept.dto.DeptTreeView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Department", description = "Department Management APIs")
@RestController
@RequestMapping("/sys/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "Get Department List")
    @GetMapping("/list")
    public R<PageResult<DeptView>> list(DeptSpecification spec, @PageableDefault Pageable pageable) {
        return R.ok(deptService.list(spec, pageable));
    }

    @Operation(summary = "Get Department Tree")
    @GetMapping("/tree")
    public R<List<DeptTreeView>> tree() {
        return R.ok(deptService.tree());
    }

    @Operation(summary = "Create Department")
    @PostMapping("/create")
    @HasPermission("sys:dept:add")
    public R<DeptView> create(@RequestBody @Valid DeptInput input) {
        return R.ok(deptService.save(input));
    }

    @Operation(summary = "Update Department")
    @PostMapping("/update")
    @HasPermission("sys:dept:update")
    public R<DeptView> update(@RequestBody @Valid DeptInput input) {
        return R.ok(deptService.save(input));
    }

    @Operation(summary = "Delete Department")
    @PostMapping("/delete")
    @HasPermission("sys:dept:delete")
    public R<Void> delete(@RequestBody @Valid IdInput input) {
        deptService.delete(input.id());
        return R.ok(null);
    }
}
