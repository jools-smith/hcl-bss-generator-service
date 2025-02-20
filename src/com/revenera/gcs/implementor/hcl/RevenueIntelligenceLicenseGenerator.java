package com.revenera.gcs.implementor.hcl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.flexnet.external.type.*;
import com.flexnet.external.webservice.keygenerator.LicGeneratorException;
import com.revenera.gcs.Application;
import com.revenera.gcs.implementor.AbstractImplementor;
import com.revenera.gcs.utils.GeneratorImplementor;
import com.revenera.gcs.utils.Log;
import com.revenera.gcs.utils.Utils;
import org.apache.commons.io.FileUtils;

import javax.xml.datatype.XMLGregorianCalendar;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class Resources {
  final String filename;
  final Path directory;
  final Path input;
  final Path executable;

  Resources() {
    this.filename = "raw." + Instant.now().toString().replace(":", ".").replace("-", ".");

    // working directory
    this.directory = Application.getInstance().getResourcePath("licenses");

    // input file for signer
    this.input = Application.getInstance().getResourcePath("licenses", filename);

    // path to signer
    this.executable = Application.getInstance().getResourcePath("executable", "Test.exe");

  }
}

class FeatureLine {
  @JsonIgnore
  public String key() {
    return String.format("%s|%s|%d", this.featureName, this.featureVersion, this.expirationDate);
  }

  static long toLong(final XMLGregorianCalendar date) {
    return date == null ? 0L : date.toGregorianCalendar().getTime().getTime();
  }

  public String featureName;
  public String featureVersion;
  public long featureCount;
  public long expirationDate;

  public FeatureLine() {
  }

  public static FeatureLine create(final com.flexnet.external.type.Feature feature, final XMLGregorianCalendar startDate, final XMLGregorianCalendar expiration) {
    return new FeatureLine() {
      {
        this.featureName = feature.getName();
        this.featureVersion = feature.getVersion();
        this.featureCount = feature.getCount();
        this.expirationDate = toLong(expiration);
      }
    };
  }

  public void add(final FeatureLine value) {
    if (key().equals(value.key())) {
      this.featureCount += value.featureCount;
    }
  }

  static final TypeReference<List<FeatureLine>> featureLineListType = new TypeReference<List<FeatureLine>>() {
  };

  public static String serailizeList(final List<FeatureLine> features) {
    try {
      return Utils.yaml_mapper.writeValueAsString(features);
    }
    catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  static List<FeatureLine>deserializeList(final String payload) {
    try {
      return Utils.yaml_mapper.readValue(payload, featureLineListType);
    }
    catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }
}

@GeneratorImplementor(technology = "RI")
public class RevenueIntelligenceLicenseGenerator extends AbstractImplementor {

  @Override
  public String technologyName() {
    return "Revenue Intelligence License Technology";
  }

  @Override
  public String technologyId() {
    return "RI";
  }

  private static final String licenseFileName = "License";
  private static final String signatureFileName = "Signature";

  private String signLicense(final List<String> lines) {
    logger.in();
    try {
      final Resources res = new Resources();

      logger.yaml(Log.Level.debug, res);

      FileUtils.writeLines(res.input.toFile(), StandardCharsets.UTF_8.name(), lines);

      final ProcessBuilder pb = new ProcessBuilder(res.executable.toAbsolutePath().toString(), res.filename);
      logger.log(Log.Level.debug, "created process");

      pb.directory(res.directory.toFile());

      final Process proc = pb.start();
      logger.log(Log.Level.debug, "started process");

      final boolean status = proc.waitFor(30, TimeUnit.SECONDS);

      if (status) {
        logger.log(Log.Level.debug, "completed process");

        return String.join("\n", lines);
      }
      else {
        return "ERROR";
      }
    }
    catch (final Throwable e) {
      logger.exception(e);

      return e.getMessage();
    }
    finally {
      // cleanup
      logger.out();
    }
  }

  //TODO - update the formatting inline with what Tulio has posted
  @Override
  public GeneratorResponse generateLicense(final GeneratorRequest request) throws LicGeneratorException {
    logger.in();

    logger.yaml(Log.Level.debug, request);

    final List<FeatureLine> licenseElements = request
            .getEntitledProducts().stream()
            .flatMap(x -> x.getFeatures().stream())
            .map(x -> FeatureLine.create(x, request.getStartDate(), request.getExpirationDate()))
            .collect(Collectors.toList());


    logger.yaml(Log.Level.debug, licenseElements);

    return new GeneratorResponse() {
      {
        this.licenseFiles = makeLicenseFiles(
                request.getLicenseTechnology().getLicenseFileDefinitions(),
                Utils.safeSerializeYaml(licenseElements),
                null);

        this.complete = true;
      }
    };
  }


  @Override
  public ConsolidatedLicense consolidateFulfillments(final FulfillmentRecordSet request) throws LicGeneratorException {

//    logger.yaml(Log.Level.info, request);

    final Map<String, FeatureLine> licenseElements = new HashMap<>();

    request.getFulfillments().forEach(fid -> {
//      logger.array(Log.Level.debug, "fid", fid.getFulfillmentId());

      fid.getLicenseFiles().forEach(file -> {

        if (file.getName().equals(licenseFileName)) {
//          logger.array(Log.Level.debug, "file", file.getName(), file.getValue());

          final List<FeatureLine> lines = FeatureLine.deserializeList(file.getValue().toString());
//          logger.yaml(Log.Level.debug, lines);

          lines.forEach(line -> {
            final String key = line.key();
//            logger.array(Log.Level.debug, "key", key);

            if (!licenseElements.containsKey(key)) {
              licenseElements.put(key, line);
            }
            else {
              licenseElements.get(key).featureCount += line.featureCount;
            }
          });
        }
      });
    });

    logger.yaml(Log.Level.debug, licenseElements);


    request.getFulfillments().stream().findAny().ifPresent(fid -> {
      logger.yaml(Log.Level.debug, fid);
    });

    return new ConsolidatedLicense() {
      {
        this.fulfillments = request.getFulfillments();

        request.getFulfillments().stream().findAny().ifPresent(fid -> {
          this.licFiles = makeLicenseFiles(fid.getLicenseTechnology().getLicenseFileDefinitions(), Utils.safeSerializeYaml(licenseElements), null);
        });

//        this.licFiles = new ArrayList<>();
//
//        this.licFiles.add(new LicenseFileMapItem() {
//          {
//            this.name = licenseFileName;
//            this.value = "LICENSE";//Utils.safeSerializeYaml(licenseElements);
//          }
//        });
//
//        this.licFiles.add(new LicenseFileMapItem() {
//          {
//            this.name = signatureFileName;
//            this.value = "SIGNATURE";
//          }
//        });

      }
    };
  }
}
