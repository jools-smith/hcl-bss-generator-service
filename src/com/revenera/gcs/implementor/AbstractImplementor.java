package com.revenera.gcs.implementor;

import com.flexnet.external.type.*;
import com.flexnet.external.webservice.keygenerator.LicGeneratorException;
import com.revenera.gcs.utils.Log;
import com.revenera.gcs.Application;
import com.flexnet.external.webservice.keygenerator.LicenseGeneratorServiceInterface;
import com.revenera.gcs.utils.Utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

  void sign() {
    logger.in();
    try {

      final Path exepath = Application.singleton().getResourcePath("executable", "Test.exe");
      logger.array(Log.Level.info, "executable path", exepath.toString());

      final Path filepath = Application.singleton().getResourcePath("licenses", UUID.randomUUID() + ".lic");
      logger.array(Log.Level.info, "file path", filepath.toString());

      final Instant now = Instant.now();


      final String exec = String.format("\"%s\" \"%s\" %s", exepath.toString(), filepath.toString(), now.toString());

      logger.array(Log.Level.info, "exec", exec);
      final Process process = Runtime.getRuntime().exec(exec);

      logger.array(Log.Level.info, "process", "waiting", process.isAlive(),
                   Duration.between(now, Instant.now()),toString());
      process.waitFor();

      logger.array(Log.Level.info, "process", "finished", process.exitValue(),
                   Duration.between(now, Instant.now()),toString());


      try (InputStream fileStream = Files.newInputStream(filepath, StandardOpenOption.DELETE_ON_CLOSE)) {
        logger.log(Log.Level.info, "opened license file");
      }
    }
    catch (final Throwable t) {
      logger.exception(t);
    }
    finally {
      logger.out();
    }
  }

  @Override
  public PingResponse ping(final PingRequest request) {
    logger.in();

    sign();

    return new PingResponse() {
      {
        this.info = Utils.safeSerializeYaml(PingInfo.create());

        this.str = String.format("%s | %s | %s | %s",
                                 logger.type().getSimpleName(),
                                 Application.getInstance().getVersionDate(),
                                 Application.getInstance().getBuildSequence(),
                                 technologyId());

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

  @Override
  public ConsolidatedLicense consolidateFulfillments(final FulfillmentRecordSet fulfillmentRecordset) throws LicGeneratorException {
    final String license = fulfillmentRecordset.getFulfillments().stream().flatMap(fulfilment -> fulfilment.getLicenseFiles().stream()).filter(lfd -> String.class.isAssignableFrom(lfd.getValue().getClass())).map(lfd -> lfd.getValue().toString()).collect(Collectors.joining("\n"));

    return new ConsolidatedLicense() {
      {
        this.fulfillments = fulfillmentRecordset.getFulfillments();

        fulfillmentRecordset.getFulfillments().stream().findAny().ifPresent(fid -> {
          this.licFiles = makeLicenseFiles(fid.getLicenseTechnology().getLicenseFileDefinitions(), license, null);
        });
      }
    };
  }

  private <T> T except(final Class<T> type, final String message) {
    throw new RuntimeException(message + " | " + type.getName());
  }

  @Override
  public LicenseFileDefinitionMap generateLicenseFilenames(final GeneratorRequest fileRec) throws LicGeneratorException {

    return except(LicenseFileDefinitionMap.class, "generateLicenseFilenames not implemented");
  }

  @Override
  public LicenseFileDefinitionMap generateConsolidatedLicenseFilenames(final ConsolidatedLicenseResquest clRec) throws LicGeneratorException {
    return except(LicenseFileDefinitionMap.class, "generateConsolidatedLicenseFilenames not implemented");
  }

  @Override
  public String generateCustomHostIdentifier(final HostIdRequest hostIdReq) throws LicGeneratorException {
    return except(String.class, "generateCustomHostIdentifier not implemented");
  }

  public abstract String technologyName();

  public abstract String technologyId();
}
