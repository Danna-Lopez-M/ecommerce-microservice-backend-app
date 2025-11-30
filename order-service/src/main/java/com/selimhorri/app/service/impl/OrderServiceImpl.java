package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
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
	private final MeterRegistry meterRegistry;

	private Counter ordersCreatedCounter;
	private Counter ordersUpdatedCounter;
	private Counter ordersDeletedCounter;
	private DistributionSummary orderValueSummary;

	@PostConstruct
	void initMetrics() {
		this.ordersCreatedCounter = Counter.builder("orders_created")
				.description("Total de ordenes creadas")
				.tag("application", "order-service")
				.register(this.meterRegistry);

		this.ordersUpdatedCounter = Counter.builder("orders_updated")
				.description("Total de ordenes actualizadas")
				.tag("application", "order-service")
				.register(this.meterRegistry);

		this.ordersDeletedCounter = Counter.builder("orders_deleted")
				.description("Total de ordenes eliminadas")
				.tag("application", "order-service")
				.register(this.meterRegistry);

		this.orderValueSummary = DistributionSummary.builder("order_value_amount")
				.description("Distribucion de valores de orden (USD)")
				.baseUnit("USD")
				.publishPercentileHistogram(true)
				.serviceLevelObjectives(50.0, 100.0, 250.0, 500.0)
				.tag("application", "order-service")
				.register(this.meterRegistry);

		// Gauge en vivo del total de ordenes persistidas
		this.meterRegistry.gauge("orders_total_active", 
				io.micrometer.core.instrument.Tags.of("application", "order-service"),
				this.orderRepository,
				OrderRepository::count);
	}
	
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
		
		Order saved = this.orderRepository.save(order);
		this.ordersCreatedCounter.increment();
		this.recordOrderValue(saved.getOrderFee());
		return OrderMappingHelper.map(saved);
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
		
		Order saved = this.orderRepository.save(existingOrder);
		this.ordersUpdatedCounter.increment();
		this.recordOrderValue(saved.getOrderFee());
		return OrderMappingHelper.map(saved);
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
		
		Order saved = this.orderRepository.save(existingOrder);
		this.ordersUpdatedCounter.increment();
		this.recordOrderValue(saved.getOrderFee());
		return OrderMappingHelper.map(saved);
	}
	
	@Override
	public void deleteById(final Integer orderId) {
		log.info("*** Void, service; delete order by id *");
		Order order = this.orderRepository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(String.format("Order with id: %d not found", orderId)));
		this.orderRepository.delete(order);
		this.ordersDeletedCounter.increment();
	}

	private void recordOrderValue(@Nullable final Double orderFee) {
		if (orderFee != null && orderFee > 0) {
			this.orderValueSummary.record(orderFee);
		}
	}
	
	
	
}
