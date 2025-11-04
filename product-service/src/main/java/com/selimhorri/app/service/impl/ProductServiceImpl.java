package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.helper.ProductMappingHelper;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
	
	private final ProductRepository productRepository;
	
	@Override
	public List<ProductDto> findAll() {
		log.info("*** ProductDto List, service; fetch all products *");
		return this.productRepository.findAll()
				.stream()
					.map(ProductMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public ProductDto findById(final Integer productId) {
		log.info("*** ProductDto, service; fetch product by id *");
		return this.productRepository.findById(productId)
				.map(ProductMappingHelper::map)
				.orElseThrow(() -> new ProductNotFoundException(String.format("Product with id: %d not found", productId)));
	}
	
	@Override
	public ProductDto save(final ProductDto productDto) {
		log.info("*** ProductDto, service; save product *");
		return ProductMappingHelper.map(this.productRepository
				.save(ProductMappingHelper.map(productDto)));
	}
	
	@Override
	public ProductDto update(final ProductDto productDto) {
		log.info("*** ProductDto, service; update product *");
		Product existingProduct = this.productRepository.findById(productDto.getProductId())
				.orElseThrow(() -> new ProductNotFoundException(String.format("Product with id: %d not found", productDto.getProductId())));
		
		existingProduct.setProductTitle(productDto.getProductTitle());
		existingProduct.setImageUrl(productDto.getImageUrl());
		existingProduct.setSku(productDto.getSku());
		existingProduct.setPriceUnit(productDto.getPriceUnit());
		existingProduct.setQuantity(productDto.getQuantity());
		if (productDto.getCategoryDto() != null) {
			existingProduct.setCategory(ProductMappingHelper.mapCategoryDto(productDto.getCategoryDto()));
		}
		
		return ProductMappingHelper.map(this.productRepository.save(existingProduct));
	}
	
	@Override
	public ProductDto update(final Integer productId, final ProductDto productDto) {
		log.info("*** ProductDto, service; update product with productId *");
		Product existingProduct = this.productRepository.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException(String.format("Product with id: %d not found", productId)));
		
		existingProduct.setProductTitle(productDto.getProductTitle());
		existingProduct.setImageUrl(productDto.getImageUrl());
		existingProduct.setSku(productDto.getSku());
		existingProduct.setPriceUnit(productDto.getPriceUnit());
		existingProduct.setQuantity(productDto.getQuantity());
		if (productDto.getCategoryDto() != null) {
			existingProduct.setCategory(ProductMappingHelper.mapCategoryDto(productDto.getCategoryDto()));
		}
		
		return ProductMappingHelper.map(this.productRepository.save(existingProduct));
	}
	
	@Override
	public void deleteById(final Integer productId) {
		log.info("*** Void, service; delete product by id *");
		Product product = this.productRepository.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException(String.format("Product with id: %d not found", productId)));
		this.productRepository.delete(product);
	}

	@Override
	public boolean isValidPrice(final Double price) {
		if (price == null) return false;
		return price >= 0.0;
	}

	@Override
	public boolean isSkuUnique(final String sku) {
		if (sku == null) return false;
		return !this.productRepository.existsBySku(sku);
	}
	
	
	
}









