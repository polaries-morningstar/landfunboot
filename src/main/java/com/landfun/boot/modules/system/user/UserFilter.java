package com.landfun.boot.modules.system.user;

import java.util.SortedMap;
import java.util.TreeMap;

import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.stereotype.Component;

import com.landfun.boot.infrastructure.service.DataScopeResolver;
import com.landfun.boot.infrastructure.service.DataScopeResolver.ScopeResult;
import com.landfun.boot.infrastructure.service.DataScopeService;
import com.landfun.boot.infrastructure.web.AuthContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserFilter implements CacheableFilter<UserProps> {

    private final DataScopeService dataScopeService;

    @Override
    public SortedMap<String, Object> getParameters() {
        User user = AuthContext.getUser();
        if (user == null) {
            return null;
        }
        SortedMap<String, Object> map = new TreeMap<>();
        map.put("userId", user.id());
        return map;
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return e.getImmutableType().getJavaClass() == User.class;
    }

    @Override
    public void filter(FilterArgs<UserProps> args) {
        User user = AuthContext.getUser();
        if (user == null) {
            log.trace("No user in context, skipping UserFilter");
            return;
        }

        ScopeResult scope = DataScopeResolver.resolve(user, dataScopeService::getSubDeptIds);

        if (scope.unrestricted()) {
            log.debug("User {} has unrestricted scope, skipping filter", user.username());
            return;
        }

        if (scope.selfOnly()) {
            log.debug("Applying SELF filter for user {}", user.id());
            args.where(args.getTable().id().eq(user.id()));
            return;
        }

        log.debug("Applying DEPT filter for deptIds: {}", scope.allowedDeptIds());
        args.where(args.getTable().deptId().in(scope.allowedDeptIds()));
    }
}
