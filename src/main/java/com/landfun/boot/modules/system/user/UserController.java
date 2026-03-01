package com.landfun.boot.modules.system.user;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.web.AuthContext;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.infrastructure.web.R;
import com.landfun.boot.modules.system.user.dto.ChangePasswordInput;
import com.landfun.boot.modules.system.user.dto.ChangeSelfPasswordInput;
import com.landfun.boot.modules.system.user.dto.CreateUserInput;
import com.landfun.boot.modules.system.user.dto.UpdateUserInput;
import com.landfun.boot.modules.system.user.dto.UserSpecification;
import com.landfun.boot.modules.system.user.dto.UserView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "User Management APIs")
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get User List Page")
    @GetMapping
    @HasPermission("sys:user:list")
    public R<PageResult<UserView>> list(UserSpecification spec, @PageableDefault Pageable pageable) {
        return R.ok(userService.page(spec, pageable));
    }

    @Operation(summary = "Get Current User Profile")
    @GetMapping("/self")
    public R<UserView> self() {
        Long id = AuthContext.getUserId();
        if (id == null) {
            throw new BizException(401, "Not authenticated");
        }
        return R.ok(userService.getById(id));
    }

    @Operation(summary = "Get User by ID")
    @GetMapping("/{id}")
    @HasPermission("sys:user:list")
    public R<UserView> getById(@PathVariable long id) {
        return R.ok(userService.getById(id));
    }

    @Operation(summary = "Create User")
    @PostMapping
    @HasPermission("sys:user:add")
    public R<UserView> create(@RequestBody @Valid CreateUserInput input) {
        return R.ok(userService.create(input));
    }

    @Operation(summary = "Update User")
    @PutMapping
    @HasPermission("sys:user:update")
    public R<UserView> update(@RequestBody @Valid UpdateUserInput input) {
        return R.ok(userService.update(input));
    }

    @Operation(summary = "Change Password")
    @PostMapping("/password")
    @HasPermission("sys:user:update")
    public R<Void> changePassword(@RequestBody @Valid ChangePasswordInput input) {
        userService.changePassword(input);
        return R.ok(null);
    }

    @Operation(summary = "Change My Password")
    @PostMapping("/password/self")
    public R<Void> changeSelfPassword(@RequestBody @Valid ChangeSelfPasswordInput input) {
        userService.changeSelfPassword(input);
        return R.ok(null);
    }

    @Operation(summary = "Delete User")
    @DeleteMapping("/{id}")
    @HasPermission("sys:user:delete")
    public R<Void> delete(@PathVariable long id) {
        userService.delete(id);
        return R.ok(null);
    }

    @Operation(summary = "Get Online Users")
    @GetMapping("/online")
    @HasPermission("sys:user:online")
    public R<List<OnlineUserVO>> onlineUsers() {
        return R.ok(userService.listOnlineUsers());
    }

    @Operation(summary = "Kickout User")
    @DeleteMapping("/online/{userId}")
    @HasPermission("sys:user:kickout")
    public R<Void> kickout(@PathVariable long userId) {
        userService.kickout(userId);
        return R.ok(null);
    }
}
