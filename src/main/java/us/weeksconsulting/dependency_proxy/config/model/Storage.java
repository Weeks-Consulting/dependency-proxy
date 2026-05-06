package us.weeksconsulting.dependency_proxy.config.model;

public class Storage {
  private final String type;
  private final String path;
  private final String bucket;

  public Storage(String type, String path, String bucket) {
    this.type = type;
    this.path = path;
    this.bucket = bucket;
  }

  public String getType() {
    return type;
  }

  public String getPath() {
    return path;
  }

  public String getBucket() {
    return bucket;
  }

  @Override
  public String toString() {
    return "Storage [type=" + type + ", path=" + path + ", bucket=" + bucket + "]";
  }

}
