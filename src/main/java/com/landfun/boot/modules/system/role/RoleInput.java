package com.landfun.boot.modules.system.role;

import lombok.Data;
import org.babyfish.jimmer.Input;
import java.util.List;

@Data
public class RoleInput implements Input<Role> {

    private Long id;

    private String code;

    private String name;

    private String description;

    private List<Long> menuIds;

    private List<Long> deptIds;

    private DataScope dataScope;

    @Override
    public Role toEntity() {
        return RoleDraft.$.produce(draft -> {
            if (id != null) {
                draft.setId(id);
            }
            if (code != null) {
                draft.setCode(code);
            }
            if (name != null) {
                draft.setName(name);
            }
            if (description != null) {
                draft.setDescription(description);
            }
            if (menuIds != null) {
                for (Long menuId : menuIds) {
                    draft.addIntoMenus(menu -> menu.setId(menuId));
                }
            }
            if (deptIds != null) {
                for (Long deptId : deptIds) {
                    draft.addIntoDepts(dept -> dept.setId(deptId));
                }
            }
            if (dataScope != null) {
                draft.setDataScope(dataScope);
            }
        });
    }
}
