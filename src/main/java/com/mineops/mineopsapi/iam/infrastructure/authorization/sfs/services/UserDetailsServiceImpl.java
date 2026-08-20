package com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.services;

import com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.mineops.mineopsapi.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("defaultUserDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmail(username)
                .map(UserDetailsImpl::build)
                .orElseThrow(() -> new UsernameNotFoundException("El usuario %s no fue encontrado".formatted(username)));
    }
}
