package com.landfun.boot.modules.system.dept;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.IdInput;
import com.landfun.boot.infrastructure.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public R<List<Dept>> tree() {
        return R.ok(deptService.tree());
    }

    @PostMapping("/create")
    @HasPermission("sys:dept:add")
    public R<Long> create(@RequestBody @Valid DeptInput input) {
        return R.ok(deptService.save(input));
    }

    @PostMapping("/update")
    @HasPermission("sys:dept:update")
    public R<Long> update(@RequestBody @Valid DeptInput input) {
        return R.ok(deptService.save(input));
    }

    @PostMapping("/delete")
    @HasPermission("sys:dept:delete")
    public R<Void> delete(@RequestBody @Valid IdInput input) {
        deptService.delete(input.id());
        return R.ok(null);
    }
}
