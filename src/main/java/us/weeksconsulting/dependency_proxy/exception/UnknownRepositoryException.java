package us.weeksconsulting.dependency_proxy.exception;

public class UnknownRepositoryException extends RuntimeException {

  public UnknownRepositoryException(String repoName) {
    super("Unknown Repository - " + repoName);
  }

}
