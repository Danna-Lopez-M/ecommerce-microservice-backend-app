package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
import com.selimhorri.app.helper.OrderItemMappingHelper;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
	
	private final OrderItemRepository orderItemRepository;
	private final RestTemplate restTemplate;
	
	@Override
	public List<OrderItemDto> findAll() {
		log.info("*** OrderItemDto List, service; fetch all orderItems *");
		return this.orderItemRepository.findAll()
				.stream()
				.map(OrderItemMappingHelper::map)
				.map(o -> {
					if (o.getProductDto() != null && o.getProductDto().getProductId() != null) {
						try {
							o.setProductDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
									.PRODUCT_SERVICE_API_URL + "/" + o.getProductDto().getProductId(), ProductDto.class));
						} catch (Exception e) {
							log.warn("Failed to fetch product details for productId: {}", o.getProductDto().getProductId(), e);
						}
					}
					if (o.getOrderDto() != null && o.getOrderDto().getOrderId() != null) {
						try {
							o.setOrderDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
									.ORDER_SERVICE_API_URL + "/" + o.getOrderDto().getOrderId(), OrderDto.class));
						} catch (Exception e) {
							log.warn("Failed to fetch order details for orderId: {}", o.getOrderDto().getOrderId(), e);
						}
					}
					return o;
				})
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public OrderItemDto findById(final OrderItemId orderItemId) {
		log.info("*** OrderItemDto, service; fetch orderItem by id *");
		return this.orderItemRepository.findById(orderItemId)
				.map(OrderItemMappingHelper::map)
				.map(o -> {
					if (o.getProductDto() != null && o.getProductDto().getProductId() != null) {
						try {
							o.setProductDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
									.PRODUCT_SERVICE_API_URL + "/" + o.getProductDto().getProductId(), ProductDto.class));
						} catch (Exception e) {
							log.warn("Failed to fetch product details for productId: {}", o.getProductDto().getProductId(), e);
						}
					}
					if (o.getOrderDto() != null && o.getOrderDto().getOrderId() != null) {
						try {
							o.setOrderDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
									.ORDER_SERVICE_API_URL + "/" + o.getOrderDto().getOrderId(), OrderDto.class));
						} catch (Exception e) {
							log.warn("Failed to fetch order details for orderId: {}", o.getOrderDto().getOrderId(), e);
						}
					}
					return o;
				})
				.orElseThrow(() -> new OrderItemNotFoundException(String.format("OrderItem with id: %s not found", orderItemId)));
	}
	
	@Override
	public OrderItemDto save(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; save orderItem *");
		final OrderItemDto savedOrderItemDto = OrderItemMappingHelper.map(this.orderItemRepository
				.save(OrderItemMappingHelper.map(orderItemDto)));
		
		// Obtener Product completo del product-service
		if (savedOrderItemDto.getProductDto() != null && savedOrderItemDto.getProductDto().getProductId() != null) {
			final Integer productId = savedOrderItemDto.getProductDto().getProductId();
			final String productUrl = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId;
			log.info("Fetching product from URL: {}", productUrl);
			try {
				final ProductDto productDto = this.restTemplate.getForObject(productUrl, ProductDto.class);
				if (productDto != null) {
					log.info("Successfully fetched product: {}", productDto);
					savedOrderItemDto.setProductDto(productDto);
				} else {
					log.warn("ProductDto is null for productId: {}", productId);
				}
			} catch (Exception e) {
				log.error("Failed to fetch product from product-service for productId {} from URL {}: {}", 
						productId, productUrl, e.getMessage(), e);
			}
		} else {
			log.warn("ProductDto or ProductId is null in savedOrderItemDto");
		}
		
		// Obtener Order completo del order-service
		if (savedOrderItemDto.getOrderDto() != null && savedOrderItemDto.getOrderDto().getOrderId() != null) {
			final Integer orderId = savedOrderItemDto.getOrderDto().getOrderId();
			final String orderUrl = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId;
			log.info("Fetching order from URL: {}", orderUrl);
			try {
				final OrderDto orderDto = this.restTemplate.getForObject(orderUrl, OrderDto.class);
				if (orderDto != null) {
					log.info("Successfully fetched order: {}", orderDto);
					savedOrderItemDto.setOrderDto(orderDto);
				} else {
					log.warn("OrderDto is null for orderId: {}", orderId);
				}
			} catch (Exception e) {
				log.error("Failed to fetch order from order-service for orderId {} from URL {}: {}", 
						orderId, orderUrl, e.getMessage(), e);
			}
		} else {
			log.warn("OrderDto or OrderId is null in savedOrderItemDto");
		}
		
		return savedOrderItemDto;
	}
	
	@Override
	public OrderItemDto update(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; update orderItem *");
		OrderItemId orderItemId = new OrderItemId(orderItemDto.getOrderId(), orderItemDto.getProductId());
		OrderItem existingOrderItem = this.orderItemRepository.findById(orderItemId)
				.orElseThrow(() -> new OrderItemNotFoundException(String.format("OrderItem with id: %s not found", orderItemId)));
		
		existingOrderItem.setOrderedQuantity(orderItemDto.getOrderedQuantity());
		
		final OrderItemDto updatedOrderItemDto = OrderItemMappingHelper.map(this.orderItemRepository.save(existingOrderItem));
		
		// Obtener Product completo del product-service
		if (updatedOrderItemDto.getProductDto() != null && updatedOrderItemDto.getProductDto().getProductId() != null) {
			final Integer productId = updatedOrderItemDto.getProductDto().getProductId();
			final String productUrl = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId;
			log.info("Fetching product from URL: {}", productUrl);
			try {
				final ProductDto productDto = this.restTemplate.getForObject(productUrl, ProductDto.class);
				if (productDto != null) {
					log.info("Successfully fetched product: {}", productDto);
					updatedOrderItemDto.setProductDto(productDto);
				} else {
					log.warn("ProductDto is null for productId: {}", productId);
				}
			} catch (Exception e) {
				log.error("Failed to fetch product from product-service for productId {} from URL {}: {}", 
						productId, productUrl, e.getMessage(), e);
			}
		} else {
			log.warn("ProductDto or ProductId is null in updatedOrderItemDto");
		}
		
		// Obtener Order completo del order-service
		if (updatedOrderItemDto.getOrderDto() != null && updatedOrderItemDto.getOrderDto().getOrderId() != null) {
			final Integer orderId = updatedOrderItemDto.getOrderDto().getOrderId();
			final String orderUrl = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId;
			log.info("Fetching order from URL: {}", orderUrl);
			try {
				final OrderDto orderDto = this.restTemplate.getForObject(orderUrl, OrderDto.class);
				if (orderDto != null) {
					log.info("Successfully fetched order: {}", orderDto);
					updatedOrderItemDto.setOrderDto(orderDto);
				} else {
					log.warn("OrderDto is null for orderId: {}", orderId);
				}
			} catch (Exception e) {
				log.error("Failed to fetch order from order-service for orderId {} from URL {}: {}", 
						orderId, orderUrl, e.getMessage(), e);
			}
		} else {
			log.warn("OrderDto or OrderId is null in updatedOrderItemDto");
		}
		
		return updatedOrderItemDto;
	}
	
	@Override
	public void deleteById(final OrderItemId orderItemId) {
		log.info("*** Void, service; delete orderItem by id *");
		OrderItem orderItem = this.orderItemRepository.findById(orderItemId)
				.orElseThrow(() -> new OrderItemNotFoundException(String.format("OrderItem with id: %s not found", orderItemId)));
		this.orderItemRepository.delete(orderItem);
	}
	
	
	
}