package us.weeksconsulting.dependency_proxy.exception;

public class UnknownStorageTypeException extends RuntimeException {

  public UnknownStorageTypeException(String storageType) {
    super("Unknown Storage Type - " + storageType);
  }

}
