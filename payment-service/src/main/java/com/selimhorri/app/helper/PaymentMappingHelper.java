package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;

public interface PaymentMappingHelper {
	
	public static PaymentDto map(final Payment payment) {
		PaymentDto.PaymentDtoBuilder builder = PaymentDto.builder()
				.paymentId(payment.getPaymentId())
				.isPayed(payment.getIsPayed())
				.paymentStatus(payment.getPaymentStatus());
		
		if (payment.getOrderId() != null) {
			builder.orderDto(
					OrderDto.builder()
						.orderId(payment.getOrderId())
						.build());
		}
		
		return builder.build();
	}
	
	public static Payment map(final PaymentDto paymentDto) {
		Payment.PaymentBuilder builder = Payment.builder()
				.paymentId(paymentDto.getPaymentId())
				.isPayed(paymentDto.getIsPayed())
				.paymentStatus(paymentDto.getPaymentStatus());
		
		if (paymentDto.getOrderDto() != null && paymentDto.getOrderDto().getOrderId() != null) {
			builder.orderId(paymentDto.getOrderDto().getOrderId());
		}
		
		return builder.build();
	}
	
	
	
}










