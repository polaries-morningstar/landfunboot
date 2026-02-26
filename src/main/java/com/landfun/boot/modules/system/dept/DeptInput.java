package com.landfun.boot.modules.system.dept;

import lombok.Data;
import org.babyfish.jimmer.Input;
import org.springframework.lang.Nullable;

@Data
public class DeptInput implements Input<Dept> {

    @Nullable
    private Long id;

    private String name;

    @Nullable
    private Long parentId;

    @Override
    public Dept toEntity() {
        return DeptDraft.$.produce(draft -> {
            if (id != null) {
                draft.setId(id);
            }
            if (name != null) {
                draft.setName(name);
            }
            if (parentId != null) {
                draft.setParentId(parentId);
            }
        });
    }
}
