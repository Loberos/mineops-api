package com.mineops.mineopsapi.iam.application.internal.queryservices;

import com.mineops.mineopsapi.iam.domain.model.aggregates.User;
import com.mineops.mineopsapi.iam.domain.model.queries.GetAllUsersQuery;
import com.mineops.mineopsapi.iam.domain.model.queries.GetUserByEmailQuery;
import com.mineops.mineopsapi.iam.domain.model.queries.GetUserByIdQuery;
import com.mineops.mineopsapi.iam.domain.services.UserQueryService;
import com.mineops.mineopsapi.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;
import com.mineops.mineopsapi.shared.infrastructure.persistence.jpa.PageCriteriaTranslator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PagedResult<User> handle(GetAllUsersQuery query, PageCriteria criteria) {
        // `findAll` no trae orden propio, así que se le fija uno: sin él, dos páginas consecutivas
        // pueden repetir un usuario y saltarse otro según cómo devuelva las filas la base.
        var page = userRepository.findAll(PageCriteriaTranslator.toPageable(criteria, Sort.by("email")));
        return PageCriteriaTranslator.toPagedResult(page, criteria);
    }

    @Override
    public List<User> handle(GetAllUsersQuery query) {
        return handle(query, PageCriteria.unpaged()).content();
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }

    @Override
    public Optional<User> handle(GetUserByEmailQuery query) {
        return userRepository.findByEmail(query.email());
    }
}
