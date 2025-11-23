package com.selimhorri.app.domain.id;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.selimhorri.app.constant.AppConstant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class FavouriteId implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@NotNull
	private Integer userId;
	
	@NotNull
	private Integer productId;
	
	@NotNull
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", shape = Shape.STRING)
	@DateTimeFormat(pattern = AppConstant.LOCAL_DATE_TIME_FORMAT)
	private LocalDateTime likeDate;
	
}










