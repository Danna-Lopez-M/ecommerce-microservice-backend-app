package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;

public interface ProductMappingHelper {
	
	public static ProductDto map(final Product product) {
		ProductDto.ProductDtoBuilder builder = ProductDto.builder()
				.productId(product.getProductId())
				.productTitle(product.getProductTitle())
				.imageUrl(product.getImageUrl())
				.sku(product.getSku())
				.priceUnit(product.getPriceUnit())
				.quantity(product.getQuantity());
		
		if (product.getCategory() != null) {
			builder.categoryDto(
					CategoryDto.builder()
						.categoryId(product.getCategory().getCategoryId())
						.categoryTitle(product.getCategory().getCategoryTitle())
						.imageUrl(product.getCategory().getImageUrl())
						.build());
		}
		
		return builder.build();
	}
	
	public static Product map(final ProductDto productDto) {
		Product.ProductBuilder builder = Product.builder()
				.productId(productDto.getProductId())
				.productTitle(productDto.getProductTitle())
				.imageUrl(productDto.getImageUrl())
				.sku(productDto.getSku())
				.priceUnit(productDto.getPriceUnit())
				.quantity(productDto.getQuantity());
		
		if (productDto.getCategoryDto() != null) {
			builder.category(mapCategoryDto(productDto.getCategoryDto()));
		}
		
		return builder.build();
	}
	
	public static Category mapCategoryDto(final CategoryDto categoryDto) {
		if (categoryDto == null) {
			return null;
		}
		return Category.builder()
				.categoryId(categoryDto.getCategoryId())
				.categoryTitle(categoryDto.getCategoryTitle())
				.imageUrl(categoryDto.getImageUrl())
				.build();
	}
	
	
	
}










