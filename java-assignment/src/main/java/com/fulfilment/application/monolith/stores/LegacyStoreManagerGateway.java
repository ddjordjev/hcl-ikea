package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LegacyStoreManagerGateway {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreManagerGateway.class);

  public void createStoreOnLegacySystem(Store store) {
    writeToFile(store);
  }

  public void updateStoreOnLegacySystem(Store store) {
    writeToFile(store);
  }

  private void writeToFile(Store store) {
    try {
      Path tempFile = Files.createTempFile(store.name, ".txt");
      LOGGER.debugf("Temporary file created at: %s", tempFile);

      String content =
              "Store created. [ name ="
                      + store.name
                      + " ] [ items on stock ="
                      + store.quantityProductsInStock
                      + "]";
      Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
      LOGGER.debug("Data written to temporary file.");

      String readContent = Files.readString(tempFile, StandardCharsets.UTF_8);
      LOGGER.debugf("Data read from temporary file: %s", readContent);

      Files.delete(tempFile);
      LOGGER.debug("Temporary file deleted.");
    } catch (Exception e) {
      LOGGER.error("Failed to write legacy store temp file", e);
    }
  }
}