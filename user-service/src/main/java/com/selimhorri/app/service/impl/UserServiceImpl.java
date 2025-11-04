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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	
	private final UserRepository userRepository;
	
	@Override
	public List<UserDto> findAll() {
		log.info("*** UserDto List, service; fetch all users *");
		return this.userRepository.findAll()
				.stream()
					.map(UserMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public UserDto findById(final Integer userId) {
		log.info("*** UserDto, service; fetch user by id *");
		return this.userRepository.findById(userId)
				.map(UserMappingHelper::map)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with id: %d not found", userId)));
	}
	
	@Override
	public UserDto save(final UserDto userDto) {
		log.info("*** UserDto, service; save user *");
		return UserMappingHelper.map(this.userRepository.save(UserMappingHelper.map(userDto)));
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
	public void deleteById(final Integer userId) {
		log.info("*** Void, service; delete user by id *");
		final User user = this.userRepository.findById(userId)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with id: %d not found", userId)));
		this.userRepository.delete(user);
	}
	
	@Override
	public UserDto findByUsername(final String username) {
		log.info("*** UserDto, service; fetch user with username *");
		return UserMappingHelper.map(this.userRepository.findByCredentialUsername(username)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with username: %s not found", username))));
	}

	@Override
	public boolean isValidEmail(final String email) {
		if (email == null) return false;
		return email.contains("@") && email.contains(".");
	}
	
	
	
}










