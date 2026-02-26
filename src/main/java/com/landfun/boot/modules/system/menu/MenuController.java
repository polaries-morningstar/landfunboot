package com.landfun.boot.modules.system.menu;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.IdInput;
import com.landfun.boot.infrastructure.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public R<List<Menu>> tree() {
        return R.ok(menuService.tree());
    }

    @PostMapping("/create")
    @HasPermission("sys:menu:add")
    public R<Long> create(@RequestBody @Valid MenuInput input) {
        return R.ok(menuService.save(input));
    }

    @PostMapping("/update")
    @HasPermission("sys:menu:update")
    public R<Long> update(@RequestBody @Valid MenuInput input) {
        return R.ok(menuService.save(input));
    }

    @PostMapping("/delete")
    @HasPermission("sys:menu:delete")
    public R<Void> delete(@RequestBody @Valid IdInput input) {
        menuService.delete(input.id());
        return R.ok(null);
    }
}
