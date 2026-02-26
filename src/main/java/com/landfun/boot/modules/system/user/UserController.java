package com.landfun.boot.modules.system.user;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.BasePageQuery;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.infrastructure.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @HasPermission("sys:user:list")
    public R<PageResult<User>> list(BasePageQuery query) {
        return R.ok(userService.page(query));
    }

    @PostMapping
    @HasPermission("sys:user:add")
    public R<Long> create(@RequestBody @Valid UserInput input) {
        return R.ok(userService.save(input));
    }

    @PutMapping
    @HasPermission("sys:user:update")
    public R<Long> update(@RequestBody @Valid UserInput input) {
        return R.ok(userService.save(input));
    }

    @DeleteMapping("/{id}")
    @HasPermission("sys:user:delete")
    public R<Void> delete(@PathVariable long id) {
        userService.delete(id);
        return R.ok(null);
    }
}
