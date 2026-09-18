package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.auth.UserResponse;
import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.entity.User;
import com.dinesh.enterprise.enums.UserStatus;
import com.dinesh.enterprise.exception.ResourceNotFoundException;
import com.dinesh.enterprise.mapper.UserMapper;
import com.dinesh.enterprise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user management operations (admin-level and self-profile).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Returns a paginated list of all users (ADMIN).
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable)
                .map(userMapper::toResponse);
        return PageResponse.of(page);
    }

    /**
     * Returns a single user by ID (ADMIN).
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findUserById(id);
        return userMapper.toResponse(user);
    }

    /**
     * Updates a user's account status (ADMIN).
     */
    @Transactional
    public UserResponse updateUserStatus(Long id, UserStatus newStatus) {
        User user = findUserById(id);
        user.setStatus(newStatus);
        User saved = userRepository.save(user);
        log.info("Updated status of user {} to {}", id, newStatus);
        return userMapper.toResponse(saved);
    }

    /**
     * Returns the currently authenticated user's profile.
     */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return userMapper.toResponse(user);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
