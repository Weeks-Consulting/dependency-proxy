package us.weeksconsulting.dependency_proxy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class CachingException extends RuntimeException {

  public CachingException(Throwable cause) {
    super(cause);
  }

  public CachingException(String message, Throwable cause) {
    super(message, cause);
  }

}
