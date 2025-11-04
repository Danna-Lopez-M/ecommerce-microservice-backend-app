package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.helper.OrderMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
	
	private final OrderRepository orderRepository;
	private final CartRepository cartRepository;
	
	@Override
	public List<OrderDto> findAll() {
		log.info("*** OrderDto List, service; fetch all orders *");
		return this.orderRepository.findAll()
				.stream()
					.map(OrderMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public OrderDto findById(final Integer orderId) {
		log.info("*** OrderDto, service; fetch order by id *");
		return this.orderRepository.findById(orderId)
				.map(OrderMappingHelper::map)
				.orElseThrow(() -> new OrderNotFoundException(String
						.format("Order with id: %d not found", orderId)));
	}
	
	@Override
	public OrderDto save(final OrderDto orderDto) {
		log.info("*** OrderDto, service; save order *");
		Order order = Order.builder()
				.orderDate(orderDto.getOrderDate())
				.orderDesc(orderDto.getOrderDesc())
				.orderFee(orderDto.getOrderFee())
				.build();
		
		if (orderDto.getCartDto() != null && orderDto.getCartDto().getCartId() != null) {
			Cart cart = this.cartRepository.findById(orderDto.getCartDto().getCartId())
					.orElseGet(() -> {
						// Si el cart no existe, crear uno nuevo con cartId y userId
						Cart newCart = Cart.builder()
								.cartId(orderDto.getCartDto().getCartId())
								.userId(orderDto.getCartDto().getUserId())
								.build();
						return this.cartRepository.save(newCart);
					});
			order.setCart(cart);
		}
		
		return OrderMappingHelper.map(this.orderRepository.save(order));
	}
	
	@Override
	public OrderDto update(final OrderDto orderDto) {
		log.info("*** OrderDto, service; update order *");
		Order existingOrder = this.orderRepository.findById(orderDto.getOrderId())
				.orElseThrow(() -> new OrderNotFoundException(String.format("Order with id: %d not found", orderDto.getOrderId())));
		
		existingOrder.setOrderDate(orderDto.getOrderDate());
		existingOrder.setOrderDesc(orderDto.getOrderDesc());
		existingOrder.setOrderFee(orderDto.getOrderFee());
		if (orderDto.getCartDto() != null) {
			existingOrder.setCart(OrderMappingHelper.mapCartDto(orderDto.getCartDto()));
		}
		
		return OrderMappingHelper.map(this.orderRepository.save(existingOrder));
	}
	
	@Override
	public OrderDto update(final Integer orderId, final OrderDto orderDto) {
		log.info("*** OrderDto, service; update order with orderId *");
		Order existingOrder = this.orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(String.format("Order with id: %d not found", orderId)));
		
		existingOrder.setOrderDate(orderDto.getOrderDate());
		existingOrder.setOrderDesc(orderDto.getOrderDesc());
		existingOrder.setOrderFee(orderDto.getOrderFee());
		if (orderDto.getCartDto() != null) {
			existingOrder.setCart(OrderMappingHelper.mapCartDto(orderDto.getCartDto()));
		}
		
		return OrderMappingHelper.map(this.orderRepository.save(existingOrder));
	}
	
	@Override
	public void deleteById(final Integer orderId) {
		log.info("*** Void, service; delete order by id *");
		Order order = this.orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(String.format("Order with id: %d not found", orderId)));
		this.orderRepository.delete(order);
	}
	
	
	
}