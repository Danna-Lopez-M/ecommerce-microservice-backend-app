package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.helper.CartMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
	
	private final CartRepository cartRepository;
	private final RestTemplate restTemplate;
	
	@Override
	public List<CartDto> findAll() {
		log.info("*** CartDto List, service; fetch all carts *");
		return this.cartRepository.findAll()
				.stream()
				.map(CartMappingHelper::map)
				.map(c -> {
					// Obtener User completo del user-service
					if (c.getUserDto() != null && c.getUserDto().getUserId() != null) {
						try {
							final UserDto userDto = this.restTemplate.getForObject(
									AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + c.getUserDto().getUserId(), 
									UserDto.class);
							c.setUserDto(userDto);
						} catch (Exception e) {
							log.warn("Failed to fetch user from user-service for userId {}: {}", 
									c.getUserDto().getUserId(), e.getMessage());
						}
					}
					return c;
				})
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public CartDto findById(final Integer cartId) {
		log.info("*** CartDto, service; fetch cart by id *");
		return this.cartRepository.findById(cartId)
				.map(CartMappingHelper::map)
				.map(c -> {
					// Obtener User completo del user-service
					if (c.getUserDto() != null && c.getUserDto().getUserId() != null) {
						try {
							final UserDto userDto = this.restTemplate.getForObject(
									AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + c.getUserDto().getUserId(), 
									UserDto.class);
							c.setUserDto(userDto);
						} catch (Exception e) {
							log.warn("Failed to fetch user from user-service for userId {}: {}", 
									c.getUserDto().getUserId(), e.getMessage());
						}
					}
					return c;
				})
				.orElseThrow(() -> new CartNotFoundException(String
						.format("Cart with id: %d not found", cartId)));
	}
	
	@Override
	public CartDto save(final CartDto cartDto) {
		log.info("*** CartDto, service; save cart *");
		final CartDto savedCartDto = CartMappingHelper.map(this.cartRepository
				.save(CartMappingHelper.map(cartDto)));
		
		// Obtener User completo del user-service
		if (savedCartDto.getUserDto() != null && savedCartDto.getUserDto().getUserId() != null) {
			try {
				final UserDto userDto = this.restTemplate.getForObject(
						AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + savedCartDto.getUserDto().getUserId(), 
						UserDto.class);
				savedCartDto.setUserDto(userDto);
			} catch (Exception e) {
				log.warn("Failed to fetch user from user-service: {}", e.getMessage());
			}
		}
		
		return savedCartDto;
	}
	
	@Override
	public CartDto update(final CartDto cartDto) {
		log.info("*** CartDto, service; update cart *");
		final CartDto updatedCartDto = CartMappingHelper.map(this.cartRepository
				.save(CartMappingHelper.map(cartDto)));
		
		// Obtener User completo del user-service
		if (updatedCartDto.getUserDto() != null && updatedCartDto.getUserDto().getUserId() != null) {
			try {
				final UserDto userDto = this.restTemplate.getForObject(
						AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + updatedCartDto.getUserDto().getUserId(), 
						UserDto.class);
				updatedCartDto.setUserDto(userDto);
			} catch (Exception e) {
				log.warn("Failed to fetch user from user-service: {}", e.getMessage());
			}
		}
		
		return updatedCartDto;
	}
	
	@Override
	public CartDto update(final Integer cartId, final CartDto cartDto) {
		log.info("*** CartDto, service; update cart with cartId *");
		return CartMappingHelper.map(this.cartRepository
				.save(CartMappingHelper.map(this.findById(cartId))));
	}
	
	@Override
	public void deleteById(final Integer cartId) {
		log.info("*** Void, service; delete cart by id *");
		this.cartRepository.deleteById(cartId);
	}
	
	
	
}