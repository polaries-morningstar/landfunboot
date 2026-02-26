package com.landfun.boot.modules.system.menu;

import lombok.RequiredArgsConstructor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final JSqlClient sqlClient;

    public List<Menu> tree() {
        return sqlClient.createQuery(MenuTable.$)
                .select(MenuTable.$)
                .execute();
    }

    @Transactional
    public long save(MenuInput input) {
        SimpleSaveResult<Menu> result = sqlClient.save(input);
        return result.getModifiedEntity().id();
    }

    @Transactional
    public void delete(long id) {
        sqlClient.deleteById(Menu.class, id);
    }
}
