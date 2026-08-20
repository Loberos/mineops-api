package com.mineops.mineopsapi.iam.application.internal.outboundservices.acl;

import com.mineops.mineopsapi.iam.domain.model.aggregates.User;
import com.mineops.mineopsapi.iam.domain.model.queries.GetUserByEmailQuery;
import com.mineops.mineopsapi.iam.domain.model.queries.GetUserByIdQuery;
import com.mineops.mineopsapi.iam.domain.services.UserQueryService;
import com.mineops.mineopsapi.iam.interfaces.acl.UserContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserContextFacadeImpl implements UserContextFacade {

    private final UserQueryService userQueryService;

    public UserContextFacadeImpl(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @Override
    public Optional<Long> fetchUserIdByEmail(String email) {
        return userQueryService.handle(new GetUserByEmailQuery(email)).map(User::getId);
    }

    @Override
    public Optional<String> fetchFullNameByUserId(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId)).map(User::getFullName);
    }

    @Override
    public boolean isAllowedToAuthorizeOverrides(Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(User::canAuthorizeOverrides)
                .orElse(false);
    }
}
