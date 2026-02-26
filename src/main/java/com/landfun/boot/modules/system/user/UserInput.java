package com.landfun.boot.modules.system.user;

import lombok.Data;
import org.babyfish.jimmer.Input;
import java.util.List;

@Data
public class UserInput implements Input<User> {

    private Long id;

    private String username;

    private String email;

    private String password;

    private boolean active;

    private List<Long> roleIds;

    private Long deptId;

    @Override
    public User toEntity() {
        return UserDraft.$.produce(draft -> {
            if (id != null) {
                draft.setId(id);
            }
            if (username != null) {
                draft.setUsername(username);
            }
            if (email != null) {
                draft.setEmail(email);
            }
            if (password != null) {
                draft.setPassword(password);
            }
            draft.setActive(active);
            if (roleIds != null) {
                for (Long roleId : roleIds) {
                    draft.addIntoRoles(role -> role.setId(roleId));
                }
            }
            if (deptId != null) {
                draft.setDeptId(deptId);
            }
        });
    }
}
