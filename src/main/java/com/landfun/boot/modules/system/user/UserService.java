package com.landfun.boot.modules.system.user;

import com.landfun.boot.infrastructure.web.BasePageQuery;
import com.landfun.boot.infrastructure.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final JSqlClient sqlClient;

    public PageResult<User> page(BasePageQuery query) {
        Page<User> page = sqlClient.createQuery(UserTable.$)
                .select(
                        UserTable.$.fetch(
                                UserFetcher.$.allScalarFields().roles(true).dept(true)))
                .fetchPage(query.pageIndex(), query.pageSize());

        return PageResult.of(page);
    }

    @Transactional
    public long save(UserInput input) {
        if (input.getPassword() != null && !input.getPassword().isEmpty()) {
            input.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw(input.getPassword()));
        }
        SimpleSaveResult<User> result = sqlClient.save(input);
        return result.getModifiedEntity().id();
    }

    @Transactional
    public void delete(long id) {
        sqlClient.deleteById(User.class, id);
    }
}
