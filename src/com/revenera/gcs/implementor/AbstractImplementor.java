package com.revenera.gcs.implementor;

import com.flexnet.external.type.*;
import com.flexnet.external.webservice.keygenerator.LicGeneratorException;
import com.flexnet.external.webservice.keygenerator.LicenseGeneratorServiceInterface;
import com.revenera.gcs.Application;
import com.revenera.gcs.utils.Log;
import com.revenera.gcs.utils.Utils;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class AbstractImplementor implements LicenseGeneratorServiceInterface {

  protected final Log logger = Log.create(this.getClass());

  @SuppressWarnings("unused")
  static protected <T> T raiseLicGeneratorException(final Throwable t) throws LicGeneratorException {
    throw new LicGeneratorException("unexpected exception", new SvcException() {
      {
        this.message = t.getMessage();
        this.name = t.getClass().getSimpleName();
      }
    });
  }

  @Override
  public PingResponse ping(final PingRequest request) {
    return new PingResponse() {
      {
        final PingInfo pingInfo = PingInfo.create();

        this.info = Utils.safeSerializeYaml(pingInfo);

        this.str = String.join(" | ", Arrays.asList(
            logger.type().getSimpleName(),
            Application.getInstance().getVersionString(),
            technologyId(),
            // system
            pingInfo.system.name,
            pingInfo.system.version,
            pingInfo.system.architecture,
            // program
            pingInfo.props.javaVersion,
            pingInfo.props.javaVendor,
            pingInfo.props.javaName,
            pingInfo.props.hostName,
            pingInfo.props.userName,
            Application.getInstance().getResourcePath().toString()
        ));

        this.processedTime = Instant.now().toString();
      }
    };
  }

  @Override
  public Status validateProduct(final ProductRequest product) throws LicGeneratorException {

    return new Status() {
      {
        this.message = "product is validated | " + product.getName() + " | " + product.getVersion();
        this.code = 0;
      }
    };
  }

  @Override
  public Status validateLicenseModel(final LicenseModelRequest model) throws LicGeneratorException {
    return new Status() {
      {
        this.message = "license model is validated | " + model.getName();
        this.code = 0;
      }
    };
  }

  private <T> T except(final Class<T> type, final String message) {
    throw new RuntimeException(message + " | " + type.getName());
  }

  @Override
  public LicenseFileDefinitionMap generateLicenseFilenames(final GeneratorRequest payload) throws LicGeneratorException {

    return new LicenseFileDefinitionMap() {
      {
        this.item = payload.getLicenseFileDefinitions().stream().map(x -> new LicenseFileDefinitionMapItem() {
          {
            this.name = x.getName();
          }
        }).collect(Collectors.toList());
      }
    };
  }

  @Override
  public LicenseFileDefinitionMap generateConsolidatedLicenseFilenames(final ConsolidatedLicenseResquest payload) throws LicGeneratorException {

    return new LicenseFileDefinitionMap() {
      {
        this.item = payload.getLicenseFileNames().stream().map(x -> new LicenseFileDefinitionMapItem() {
          {
            this.name = x.getName();
          }
        }).collect(Collectors.toList());
      }
    };
  }

  @Override
  public String generateCustomHostIdentifier(final HostIdRequest hostIdReq) throws LicGeneratorException {
    return except(String.class, "generateCustomHostIdentifier not implemented");
  }

  @SuppressWarnings("unused")
  public abstract String technologyName();

  public abstract String technologyId();

}
