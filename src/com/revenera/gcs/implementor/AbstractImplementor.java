package com.revenera.gcs.implementor;

import com.flexnet.external.type.LicenseFileDefinition;
import com.flexnet.external.type.LicenseFileMapItem;
import com.flexnet.external.type.PingRequest;
import com.flexnet.external.type.PingResponse;
import com.revenera.gcs.utils.Log;
import com.revenera.gcs.Application;
import com.flexnet.external.webservice.keygenerator.LicenseGeneratorServiceInterface;
import com.revenera.gcs.utils.Utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractImplementor implements LicenseGeneratorServiceInterface {

  protected final Log logger = Log.create(this.getClass());

  protected List<LicenseFileMapItem> makeLicenseFiles(final List<LicenseFileDefinition> files, final String text, final byte[] bytes) {
    return new ArrayList<LicenseFileMapItem>() {
      {
        files.forEach(lfd -> {
          switch (lfd.getLicenseStorageType()) {
            case TEXT:
              Optional.ofNullable(text).ifPresent(license -> {
                this.add(new LicenseFileMapItem() {
                  {
                    this.name = lfd.getName();
                    this.value = license;
                  }
                });
              });
              break;
            case BINARY:
              Optional.ofNullable(bytes).ifPresent(license -> {
                this.add(new LicenseFileMapItem() {
                  {
                    this.name = lfd.getName();
                    this.value = license;
                  }
                });
              });
              break;
            default:
              throw new RuntimeException("invalid license file type");
          }
        });
      }
    };
  }

  @Override
  public PingResponse ping(final PingRequest request) {
    logger.in();

    return new PingResponse() {
      {
        this.info = Utils.safeSerializeYaml(PingInfo.create());

        this.str = String.format("%s | %s | %s | %s",
                                 logger.type().getSimpleName(),
                                 Application.getInstance().getVersionDate(),
                                 Application.getInstance().getBuildSequence(),
                                 technologyName());

        this.processedTime = Instant.now().toString();
      }
    };
  }

  public abstract String technologyName();
}
