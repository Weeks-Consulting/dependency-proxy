package us.weeksconsulting.dependency_proxy.controller_advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import us.weeksconsulting.dependency_proxy.exception.UnknownRepositoryException;
import us.weeksconsulting.dependency_proxy.exception.UnknownStorageTypeException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UnknownRepositoryException.class)
  public ResponseEntity<Object> handleNotFound(UnknownRepositoryException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(UnknownStorageTypeException.class)
  public ResponseEntity<Object> handleNotFound(UnknownStorageTypeException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
  }
}
