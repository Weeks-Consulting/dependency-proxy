package us.weeksconsulting.dependency_proxy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class UnknownRepositoryException extends RuntimeException {

  public UnknownRepositoryException(String repoName) {
    super("Unknown Repository - " + repoName);
  }

}
