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
		return OrderItemMappingHelper.map(this.orderItemRepository
				.save(OrderItemMappingHelper.map(orderItemDto)));
	}
	
	@Override
	public OrderItemDto update(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; update orderItem *");
		OrderItemId orderItemId = new OrderItemId(orderItemDto.getOrderId(), orderItemDto.getProductId());
		OrderItem existingOrderItem = this.orderItemRepository.findById(orderItemId)
				.orElseThrow(() -> new OrderItemNotFoundException(String.format("OrderItem with id: %s not found", orderItemId)));
		
		existingOrderItem.setOrderedQuantity(orderItemDto.getOrderedQuantity());
		
		return OrderItemMappingHelper.map(this.orderItemRepository.save(existingOrderItem));
	}
	
	@Override
	public void deleteById(final OrderItemId orderItemId) {
		log.info("*** Void, service; delete orderItem by id *");
		OrderItem orderItem = this.orderItemRepository.findById(orderItemId)
				.orElseThrow(() -> new OrderItemNotFoundException(String.format("OrderItem with id: %s not found", orderItemId)));
		this.orderItemRepository.delete(orderItem);
	}
	
	
	
}