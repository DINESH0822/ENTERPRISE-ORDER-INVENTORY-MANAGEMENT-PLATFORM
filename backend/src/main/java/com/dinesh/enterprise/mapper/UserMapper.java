package com.dinesh.enterprise.mapper;

import com.dinesh.enterprise.dto.auth.UserResponse;
import com.dinesh.enterprise.entity.Role;
import com.dinesh.enterprise.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Maps User entity to UserResponse DTO.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
