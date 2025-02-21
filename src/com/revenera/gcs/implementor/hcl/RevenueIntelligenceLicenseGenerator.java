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

  @Override
  public String toString() {
    final StringBuilder bfr = new StringBuilder();
    bfr.append(featureName)
       .append(" ")
       .append(featureCount)
       .append(" ");

    if (this.expirationDate > 0) {
      bfr.append(new Date(this.expirationDate).toInstant().toString())
         .append(" ");;
    }

    return bfr.toString();
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

  private enum RIFileNames {
    License,
    Signature
  }

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

  @Override
  public GeneratorResponse generateLicense(final GeneratorRequest request) throws LicGeneratorException {
    logger.in();

//    logger.yaml(Log.Level.debug, request);

    final List<FeatureLine> licenseElements = request
            .getEntitledProducts().stream()
            .flatMap(x -> x.getFeatures().stream())
            .map(x -> FeatureLine.create(x, request.getStartDate(), request.getExpirationDate()))
            .collect(Collectors.toList());

    return new GeneratorResponse() {
      {
        this.licenseFiles = Collections.singletonList(new LicenseFileMapItem() {
          {
            name = RIFileNames.License.toString();
            value = Utils.safeSerializeYaml(licenseElements);
          }
        });
//        logger.yaml(Log.Level.debug, this.licenseFiles);

        this.complete = true;

        logger.yaml(Log.Level.debug, this);
      }
    };
  }


  @Override
  public ConsolidatedLicense consolidateFulfillments(final FulfillmentRecordSet request) throws LicGeneratorException {

    logger.array(Log.Level.debug, Application.getInstance().getVersionDate(), Application.getInstance().getBuildSequence());

    final Map<String, FeatureLine> licenseElements = new TreeMap<>();

    request.getFulfillments().stream()
           .flatMap(fid -> fid.getLicenseFiles().stream())
           .filter(file -> file.getName().equals(RIFileNames.License.toString()))
           .forEach(file -> {
              final List<FeatureLine> lines = FeatureLine.deserializeList(file.getValue().toString());

              lines.forEach(line -> {
                final String key = line.key();

                if (!licenseElements.containsKey(key)) {
                  licenseElements.put(key, line);
                }
                else {
                  licenseElements.get(key).featureCount += line.featureCount;
                }
              });
            });

    // build intermediate format

    final String str = licenseElements.values().stream().map(FeatureLine::toString).collect(Collectors.joining("\n"));
    logger.yaml(Log.Level.debug, str);

    return new ConsolidatedLicense() {
      {
        this.fulfillments = request.getFulfillments();

        this.licFiles = Arrays.asList(
          new LicenseFileMapItem() {
            {
              this.name = RIFileNames.License.toString();
              this.value = licenseElements.values().stream()
                                          .map(FeatureLine::toString)
                                          .collect(Collectors.joining("\n"));
            }
          },
          new LicenseFileMapItem() {
            {
              this.name = RIFileNames.Signature.toString();
              this.value = "SIGNATURE";
            }
          }
        );

//        logger.yaml(Log.Level.debug, this.licFiles);
      }
    };
  }
}
