package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.helper.UserMappingHelper;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.UserService;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	
	private final UserRepository userRepository;
	
	@Override
	@Bulkhead(name = "userServiceNonCritical", fallbackMethod = "findAllFallback")
	public List<UserDto> findAll() {
		log.info("*** UserDto List, service; fetch all users *");
		return this.userRepository.findAll()
				.stream()
					.map(UserMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	public List<UserDto> findAllFallback(Exception e) {
		log.warn("*** Bulkhead fallback: findAll - returning empty list due to: {}", e.getMessage());
		return List.of();
	}
	
	@Override
	@Bulkhead(name = "userServiceCritical", fallbackMethod = "findByIdFallback")
	public UserDto findById(final Integer userId) {
		log.info("*** UserDto, service; fetch user by id *");
		return this.userRepository.findById(userId)
				.map(UserMappingHelper::map)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with id: %d not found", userId)));
	}
	
	public UserDto findByIdFallback(final Integer userId, Exception e) {
		log.error("*** Bulkhead fallback: findById - service overloaded for userId: {}", userId, e);
		throw new UserObjectNotFoundException(String.format("Service temporarily unavailable for user id: %d", userId));
	}
	
	@Override
	@Bulkhead(name = "userServiceCritical", fallbackMethod = "saveFallback")
	public UserDto save(final UserDto userDto) {
		log.info("*** UserDto, service; save user *");
		return UserMappingHelper.map(this.userRepository.save(UserMappingHelper.map(userDto)));
	}
	
	public UserDto saveFallback(final UserDto userDto, Exception e) {
		log.error("*** Bulkhead fallback: save - service overloaded", e);
		throw new RuntimeException("Service temporarily unavailable. Please try again later.");
	}
	
	@Override
	public UserDto update(final UserDto userDto) {
		log.info("*** UserDto, service; update user *");
		if (userDto.getUserId() == null) {
			throw new UserObjectNotFoundException("User ID is required for update");
		}
		
		final User existingUser = this.userRepository.findById(userDto.getUserId())
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with id: %d not found", userDto.getUserId())));
		
		// Update user fields
		if (userDto.getFirstName() != null) existingUser.setFirstName(userDto.getFirstName());
		if (userDto.getLastName() != null) existingUser.setLastName(userDto.getLastName());
		if (userDto.getImageUrl() != null) existingUser.setImageUrl(userDto.getImageUrl());
		if (userDto.getEmail() != null) existingUser.setEmail(userDto.getEmail());
		if (userDto.getPhone() != null) existingUser.setPhone(userDto.getPhone());
		
		// Update credential if provided
		if (userDto.getCredentialDto() != null && existingUser.getCredential() != null) {
			final Credential credential = existingUser.getCredential();
			if (userDto.getCredentialDto().getUsername() != null) credential.setUsername(userDto.getCredentialDto().getUsername());
			if (userDto.getCredentialDto().getPassword() != null) credential.setPassword(userDto.getCredentialDto().getPassword());
			if (userDto.getCredentialDto().getRoleBasedAuthority() != null) credential.setRoleBasedAuthority(userDto.getCredentialDto().getRoleBasedAuthority());
			if (userDto.getCredentialDto().getIsEnabled() != null) credential.setIsEnabled(userDto.getCredentialDto().getIsEnabled());
			if (userDto.getCredentialDto().getIsAccountNonExpired() != null) credential.setIsAccountNonExpired(userDto.getCredentialDto().getIsAccountNonExpired());
			if (userDto.getCredentialDto().getIsAccountNonLocked() != null) credential.setIsAccountNonLocked(userDto.getCredentialDto().getIsAccountNonLocked());
			if (userDto.getCredentialDto().getIsCredentialsNonExpired() != null) credential.setIsCredentialsNonExpired(userDto.getCredentialDto().getIsCredentialsNonExpired());
		}
		
		return UserMappingHelper.map(this.userRepository.save(existingUser));
	}
	
	@Override
	public UserDto update(final Integer userId, final UserDto userDto) {
		log.info("*** UserDto, service; update user with userId *");
		final User existingUser = this.userRepository.findById(userId)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with id: %d not found", userId)));
		
		// Update user fields
		if (userDto.getFirstName() != null) existingUser.setFirstName(userDto.getFirstName());
		if (userDto.getLastName() != null) existingUser.setLastName(userDto.getLastName());
		if (userDto.getImageUrl() != null) existingUser.setImageUrl(userDto.getImageUrl());
		if (userDto.getEmail() != null) existingUser.setEmail(userDto.getEmail());
		if (userDto.getPhone() != null) existingUser.setPhone(userDto.getPhone());
		
		// Update credential if provided
		if (userDto.getCredentialDto() != null && existingUser.getCredential() != null) {
			final Credential credential = existingUser.getCredential();
			if (userDto.getCredentialDto().getUsername() != null) credential.setUsername(userDto.getCredentialDto().getUsername());
			if (userDto.getCredentialDto().getPassword() != null) credential.setPassword(userDto.getCredentialDto().getPassword());
			if (userDto.getCredentialDto().getRoleBasedAuthority() != null) credential.setRoleBasedAuthority(userDto.getCredentialDto().getRoleBasedAuthority());
			if (userDto.getCredentialDto().getIsEnabled() != null) credential.setIsEnabled(userDto.getCredentialDto().getIsEnabled());
			if (userDto.getCredentialDto().getIsAccountNonExpired() != null) credential.setIsAccountNonExpired(userDto.getCredentialDto().getIsAccountNonExpired());
			if (userDto.getCredentialDto().getIsAccountNonLocked() != null) credential.setIsAccountNonLocked(userDto.getCredentialDto().getIsAccountNonLocked());
			if (userDto.getCredentialDto().getIsCredentialsNonExpired() != null) credential.setIsCredentialsNonExpired(userDto.getCredentialDto().getIsCredentialsNonExpired());
		}
		
		return UserMappingHelper.map(this.userRepository.save(existingUser));
	}
	
	@Override
	@Bulkhead(name = "userServiceCritical", fallbackMethod = "deleteByIdFallback")
	public void deleteById(final Integer userId) {
		log.info("*** Void, service; delete user by id *");
		final User user = this.userRepository.findById(userId)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with id: %d not found", userId)));
		this.userRepository.delete(user);
	}
	
	public void deleteByIdFallback(final Integer userId, Exception e) {
		log.error("*** Bulkhead fallback: deleteById - service overloaded for userId: {}", userId, e);
		throw new RuntimeException("Service temporarily unavailable. Please try again later.");
	}
	
	@Override
	@Bulkhead(name = "userServiceNonCritical", fallbackMethod = "findByUsernameFallback")
	public UserDto findByUsername(final String username) {
		log.info("*** UserDto, service; fetch user with username *");
		return UserMappingHelper.map(this.userRepository.findByCredentialUsername(username)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with username: %s not found", username))));
	}
	
	public UserDto findByUsernameFallback(final String username, Exception e) {
		log.warn("*** Bulkhead fallback: findByUsername - service overloaded for username: {}", username, e);
		throw new UserObjectNotFoundException(String.format("Service temporarily unavailable for username: %s", username));
	}

	@Override
	public boolean isValidEmail(final String email) {
		if (email == null) return false;
		return email.contains("@") && email.contains(".");
	}
	
	
	
}










