package us.weeksconsulting.util;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheUtils.class);

  public static String serializeUrlParams(Map<String, String> urlParams) {
    return String.valueOf(urlParams);
  }

  public static String getObjectFilePath(String urlPath, Map<String, String> urlParams) {

    try {
      MessageDigest digest;
      digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = digest.digest((urlPath + serializeUrlParams(urlParams)).getBytes());
      String sha256Hex = Hex.encodeHexString(encodedHash);

      String level1Dir = sha256Hex.substring(0, 1);
      String level2Dir = sha256Hex.substring(1, 2);

      return level1Dir + File.separator + level2Dir;
    } catch (NoSuchAlgorithmException exception) {
      LOGGER.error("Error generation object file path.", exception);
      throw new RuntimeException(exception);
    }

  }
}
