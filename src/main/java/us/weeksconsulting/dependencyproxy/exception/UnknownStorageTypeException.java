package us.weeksconsulting.dependencyproxy.exception;

public class UnknownStorageTypeException extends RuntimeException {

  public UnknownStorageTypeException(String storageType) {
    super("Unknown Storage Type - " + storageType);
  }

}
