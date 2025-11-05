package com.selimhorri.app.helper;

import java.util.Optional;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;

public interface CategoryMappingHelper {
	
	public static CategoryDto map(final Category category) {
		
		CategoryDto.CategoryDtoBuilder builder = CategoryDto.builder()
				.categoryId(category.getCategoryId())
				.categoryTitle(category.getCategoryTitle())
				.imageUrl(category.getImageUrl());
		
		// Solo mapear parentCategoryDto si parentCategory existe
		if (category.getParentCategory() != null) {
			builder.parentCategoryDto(
					CategoryDto.builder()
						.categoryId(category.getParentCategory().getCategoryId())
						.categoryTitle(category.getParentCategory().getCategoryTitle())
						.imageUrl(category.getParentCategory().getImageUrl())
						.build());
		}
		
		return builder.build();
	}
	
	public static Category map(final CategoryDto categoryDto) {
		
		Category.CategoryBuilder builder = Category.builder()
				.categoryId(categoryDto.getCategoryId())
				.categoryTitle(categoryDto.getCategoryTitle())
				.imageUrl(categoryDto.getImageUrl());
		
		// Solo mapear parentCategory si parentCategoryDto está presente y tiene datos
		if (categoryDto.getParentCategoryDto() != null) {
			final CategoryDto parentCategoryDto = categoryDto.getParentCategoryDto();
			
			// Si solo tiene categoryId (sin categoryTitle), crear Category solo con ID
			// El servicio se encargará de buscar la categoría existente
			if (parentCategoryDto.getCategoryId() != null) {
				builder.parentCategory(
						Category.builder()
							.categoryId(parentCategoryDto.getCategoryId())
							.build());
			}
			// Si tiene categoryTitle, crear Category completa
			else if (parentCategoryDto.getCategoryTitle() != null) {
				builder.parentCategory(
						Category.builder()
							.categoryId(parentCategoryDto.getCategoryId())
							.categoryTitle(parentCategoryDto.getCategoryTitle())
							.imageUrl(parentCategoryDto.getImageUrl())
							.build());
			}
		}
		// Si parentCategoryDto es null, no establecer parentCategory (será null)
		
		return builder.build();
	}
	
	
	
}










