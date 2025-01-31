package com.revenera.gcs.implementor;

import com.flexnet.external.type.LicenseFileDefinition;
import com.flexnet.external.type.LicenseFileMapItem;
import com.flexnet.external.type.PingRequest;
import com.flexnet.external.type.PingResponse;
import com.revenera.gcs.utils.Log;
import com.revenera.gcs.Application;
import com.flexnet.external.webservice.keygenerator.LicenseGeneratorServiceInterface;
import com.revenera.gcs.utils.Utils;

import javax.servlet.ServletContext;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                                 technologyName());

        this.processedTime = Instant.now().toString();
      }
    };
  }

  public abstract String technologyName();
}
