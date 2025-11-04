package com.selimhorri.app.exception;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.selimhorri.app.exception.payload.ExceptionMsg;
import com.selimhorri.app.exception.wrapper.AddressNotFoundException;
import com.selimhorri.app.exception.wrapper.CredentialNotFoundException;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.exception.wrapper.VerificationTokenNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {
	
	@ExceptionHandler(value = {
		MethodArgumentNotValidException.class,
		HttpMessageNotReadableException.class
	})
	public <T extends BindException> ResponseEntity<ExceptionMsg> handleValidationException(final T e) {
		
		log.info("**ApiExceptionHandler controller, handle validation exception*\n");
		final var badRequest = HttpStatus.BAD_REQUEST;
		
		return new ResponseEntity<>(
				ExceptionMsg.builder()
					.msg("*" + e.getBindingResult().getFieldError().getDefaultMessage() + "!**")
					.httpStatus(badRequest)
					.timestamp(ZonedDateTime
							.now(ZoneId.systemDefault()))
					.build(), badRequest);
	}
	
	@ExceptionHandler(value = UserObjectNotFoundException.class)
	public ResponseEntity<ExceptionMsg> handleUserObjectNotFoundException(final UserObjectNotFoundException e) {
		
		log.info("**ApiExceptionHandler controller, handle user not found exception*\n");
		final var notFound = HttpStatus.NOT_FOUND;
		
		return new ResponseEntity<>(
				ExceptionMsg.builder()
					.msg("#### " + e.getMessage() + "! ####")
					.httpStatus(notFound)
					.timestamp(ZonedDateTime
							.now(ZoneId.systemDefault()))
					.build(), notFound);
	}
	
	@ExceptionHandler(value = {
		CredentialNotFoundException.class,
		VerificationTokenNotFoundException.class,
		AddressNotFoundException.class
	})
	public <T extends RuntimeException> ResponseEntity<ExceptionMsg> handleApiRequestException(final T e) {
		
		log.info("**ApiExceptionHandler controller, handle API request*\n");
		final var badRequest = HttpStatus.BAD_REQUEST;
		
		return new ResponseEntity<>(
				ExceptionMsg.builder()
					.msg("#### " + e.getMessage() + "! ####")
					.httpStatus(badRequest)
					.timestamp(ZonedDateTime
							.now(ZoneId.systemDefault()))
					.build(), badRequest);
	}
	
	@ExceptionHandler(value = DataIntegrityViolationException.class)
	public ResponseEntity<ExceptionMsg> handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
		
		log.info("**ApiExceptionHandler controller, handle data integrity violation exception*\n");
		final var conflict = HttpStatus.CONFLICT;
		
		return new ResponseEntity<>(
				ExceptionMsg.builder()
					.msg("#### Data integrity violation! ####")
					.httpStatus(conflict)
					.timestamp(ZonedDateTime
							.now(ZoneId.systemDefault()))
					.build(), conflict);
	}
	
	@ExceptionHandler(value = Exception.class)
	public ResponseEntity<ExceptionMsg> handleGenericException(final Exception e) {
		
		log.error("**ApiExceptionHandler controller, handle generic exception*\n", e);
		final var internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
		
		return new ResponseEntity<>(
				ExceptionMsg.builder()
					.msg("#### Internal server error: " + e.getMessage() + "! ####")
					.httpStatus(internalServerError)
					.timestamp(ZonedDateTime
							.now(ZoneId.systemDefault()))
					.throwable(e)
					.build(), internalServerError);
	}
	
	
	
}










