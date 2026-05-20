package us.weeksconsulting.dependency_proxy.record;

import java.sql.Timestamp;
import java.util.UUID;

@SuppressWarnings("java:S1845")
public record RepositoryCacheLockRecord(
    UUID cacheObjectId,
    UUID lockId,
    String lockType,
    Timestamp lockedAt) {

  public static final String CACHE_OBJECT_ID = "cache_object_id";
  public static final String LOCK_ID = "lock_id";
  public static final String LOCK_TYPE = "lock_type";
  public static final String LOCKED_AT = "locked_at";

  public static final String READ_LOCK = "READ";
  public static final String WRITE_LOCK = "WRITE";
  public static final String DELETE_LOCK = "DELETE";
}
