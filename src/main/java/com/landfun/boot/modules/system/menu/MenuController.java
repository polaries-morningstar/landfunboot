package com.landfun.boot.modules.system.menu;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.IdInput;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.infrastructure.web.R;
import com.landfun.boot.modules.system.menu.dto.MenuInput;
import com.landfun.boot.modules.system.menu.dto.MenuSpecification;
import com.landfun.boot.modules.system.menu.dto.MenuView;
import com.landfun.boot.modules.system.menu.dto.MenuTreeView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Menu", description = "Menu Management APIs")
@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "Get Menu List")
    @GetMapping("/list")
    public R<PageResult<MenuView>> list(MenuSpecification spec, @PageableDefault Pageable pageable) {
        return R.ok(menuService.list(spec, pageable));
    }

    @Operation(summary = "Get Menu Tree")
    @GetMapping("/tree")
    public R<List<MenuTreeView>> tree() {
        return R.ok(menuService.tree());
    }

    @Operation(summary = "Create Menu")
    @PostMapping("/create")
    @HasPermission("sys:menu:add")
    public R<MenuView> create(@RequestBody @Valid MenuInput input) {
        return R.ok(menuService.save(input));
    }

    @Operation(summary = "Update Menu")
    @PostMapping("/update")
    @HasPermission("sys:menu:update")
    public R<MenuView> update(@RequestBody @Valid MenuInput input) {
        return R.ok(menuService.save(input));
    }

    @Operation(summary = "Delete Menu")
    @PostMapping("/delete")
    @HasPermission("sys:menu:delete")
    public R<Void> delete(@RequestBody @Valid IdInput input) {
        menuService.delete(input.id());
        return R.ok(null);
    }
}
