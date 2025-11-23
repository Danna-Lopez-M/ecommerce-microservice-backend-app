package com.selimhorri.app.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.selimhorri.app.constant.AppConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FavouriteDto implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@NotNull(message = "Field must not be NULL")
	private Integer userId;
	
	@NotNull(message = "Field must not be NULL")
	private Integer productId;
	
	@NotNull(message = "Field must not be NULL")
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", shape = Shape.STRING)
	@DateTimeFormat(pattern = AppConstant.LOCAL_DATE_TIME_FORMAT)
	private LocalDateTime likeDate;
	
	@JsonProperty("user")
	@JsonInclude(Include.NON_NULL)
	private UserDto userDto;
	
	@JsonProperty("product")
	@JsonInclude(Include.NON_NULL)
	private ProductDto productDto;
	
}










