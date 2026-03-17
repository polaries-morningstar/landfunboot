package com.landfun.boot.modules.system.menu;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.IdInput;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.infrastructure.web.R;
import com.landfun.boot.modules.system.menu.dto.CreateMenuInput;
import com.landfun.boot.modules.system.menu.dto.UpdateMenuInput;
import com.landfun.boot.modules.system.menu.dto.MenuSpecification;
import com.landfun.boot.modules.system.menu.dto.MenuTreeView;
import com.landfun.boot.modules.system.menu.dto.MenuView;

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
    @HasPermission("sys:menu:list")
    public R<PageResult<MenuView>> list(MenuSpecification spec, @PageableDefault Pageable pageable) {
        return R.ok(menuService.list(spec, pageable));
    }

    @Operation(summary = "Get Menu Tree")
    @GetMapping("/tree")
    @HasPermission("sys:menu:list")
    public R<List<MenuTreeView>> tree() {
        return R.ok(menuService.tree());
    }

    @Operation(summary = "Create Menu")
    @PostMapping("/create")
    @HasPermission("sys:menu:add")
    public R<MenuView> create(@RequestBody @Valid CreateMenuInput input) {
        return R.ok(menuService.create(input));
    }

    @Operation(summary = "Update Menu")
    @PostMapping("/update")
    @HasPermission("sys:menu:update")
    public R<MenuView> update(@RequestBody @Valid UpdateMenuInput input) {
        return R.ok(menuService.update(input));
    }

    @Operation(summary = "Delete Menu")
    @PostMapping("/delete")
    @HasPermission("sys:menu:delete")
    public R<Void> delete(@RequestBody @Valid IdInput input) {
        menuService.delete(input.id());
        return R.ok(null);
    }
}
