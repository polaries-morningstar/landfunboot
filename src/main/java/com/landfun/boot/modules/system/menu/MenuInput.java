package com.landfun.boot.modules.system.menu;

import lombok.Data;
import org.babyfish.jimmer.Input;
import org.springframework.lang.Nullable;

@Data
public class MenuInput implements Input<Menu> {

    @Nullable
    private Long id;

    private long parentId;

    private String name;

    private String path;

    private String permission;

    private String component;

    private String icon;

    private int sort;

    private Menu.Type type;

    @Override
    public Menu toEntity() {
        return MenuDraft.$.produce(draft -> {
            if (id != null) {
                draft.setId(id);
            }
            draft.setParentId(parentId);
            if (name != null) {
                draft.setName(name);
            }
            if (path != null) {
                draft.setPath(path);
            }
            if (permission != null) {
                draft.setPermission(permission);
            }
            if (component != null) {
                draft.setComponent(component);
            }
            if (icon != null) {
                draft.setIcon(icon);
            }
            draft.setSortOrder(sort);
            if (type != null) {
                draft.setType(type);
            }
        });
    }
}
