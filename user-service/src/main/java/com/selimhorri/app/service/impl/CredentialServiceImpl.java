package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.exception.wrapper.CredentialNotFoundException;
import com.selimhorri.app.exception.wrapper.MissingUserDtoException;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.helper.CredentialMappingHelper;
import com.selimhorri.app.repository.CredentialRepository;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.CredentialService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CredentialServiceImpl implements CredentialService {
	
	private final CredentialRepository credentialRepository;
	private final UserRepository userRepository;
	
	@Override
	public List<CredentialDto> findAll() {
		log.info("*** CredentialDto List, service; fetch all credentials *");
		return this.credentialRepository.findAll()
				.stream()
					.map(CredentialMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public CredentialDto findById(final Integer credentialId) {
		log.info("*** CredentialDto, service; fetch credential by ids *");
		return this.credentialRepository.findById(credentialId)
				.map(CredentialMappingHelper::map)
				.orElseThrow(() -> new CredentialNotFoundException(String.format("#### Credential with id: %d not found! ####", credentialId)));
	}
	
	@Override
	public CredentialDto save(final CredentialDto credentialDto) {
		log.info("*** CredentialDto, service; save credential *");
		
		// Si no viene UserDto, crear un User nuevo automáticamente
		if (credentialDto.getUserDto() == null) {
			// Crear un User nuevo con datos mínimos
			final User newUser = User.builder()
					.firstName("New User")
					.lastName("")
					.email(credentialDto.getUsername() + "@example.com")
					.build();
			
			final User savedUser = this.userRepository.save(newUser);
			
			// Crear UserDto con el User recién creado
			credentialDto.setUserDto(
					com.selimhorri.app.dto.UserDto.builder()
						.userId(savedUser.getUserId())
						.firstName(savedUser.getFirstName())
						.lastName(savedUser.getLastName())
						.imageUrl(savedUser.getImageUrl())
						.email(savedUser.getEmail())
						.phone(savedUser.getPhone())
						.build());
		}
		// Si solo se proporciona userId, buscar el User existente
		else if (credentialDto.getUserDto().getUserId() != null && 
			credentialDto.getUserDto().getFirstName() == null) {
			final User user = this.userRepository.findById(credentialDto.getUserDto().getUserId())
					.orElseThrow(() -> new UserObjectNotFoundException(
							String.format("#### User with id: %d not found! ####", 
									credentialDto.getUserDto().getUserId())));
			
			// Crear UserDto con el User encontrado
			credentialDto.setUserDto(
					com.selimhorri.app.dto.UserDto.builder()
						.userId(user.getUserId())
						.firstName(user.getFirstName())
						.lastName(user.getLastName())
						.imageUrl(user.getImageUrl())
						.email(user.getEmail())
						.phone(user.getPhone())
						.build());
		}
		
		return CredentialMappingHelper.map(this.credentialRepository.save(CredentialMappingHelper.map(credentialDto)));
	}
	
	@Override
	public CredentialDto update(final CredentialDto credentialDto) {
		log.info("*** CredentialDto, service; update credential *");
		return CredentialMappingHelper.map(this.credentialRepository.save(CredentialMappingHelper.map(credentialDto)));
	}
	
	@Override
	public CredentialDto update(final Integer credentialId, final CredentialDto credentialDto) {
		log.info("*** CredentialDto, service; update credential with credentialId *");
		return CredentialMappingHelper.map(this.credentialRepository.save(
				CredentialMappingHelper.map(this.findById(credentialId))));
	}
	
	@Override
	public void deleteById(final Integer credentialId) {
		log.info("*** Void, service; delete credential by id *");
		this.credentialRepository.deleteById(credentialId);
	}
	
	@Override
	public CredentialDto findByUsername(final String username) {
		return CredentialMappingHelper.map(this.credentialRepository.findByUsername(username)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("#### Credential with username: %s not found! ####", username))));
	}
	
	
	
}










