package com.revenera.gcs.implementor.hcl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.flexnet.external.type.ConsolidatedLicense;
import com.flexnet.external.type.FulfillmentRecordSet;
import com.flexnet.external.type.GeneratorRequest;
import com.flexnet.external.type.GeneratorResponse;
import com.flexnet.external.webservice.keygenerator.LicGeneratorException;
import com.revenera.gcs.Application;
import com.revenera.gcs.implementor.AbstractImplementor;
import com.revenera.gcs.utils.Diagnostics;
import com.revenera.gcs.utils.GeneratorImplementor;
import com.revenera.gcs.utils.Log;
import com.revenera.gcs.utils.Utils;
import org.apache.commons.io.FileUtils;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@GeneratorImplementor(technology = "MEDIA")
public class FriLicenseGenerator extends AbstractImplementor {

  @Override
  public String technologyName() {
    return "BSS Solutions License Technology";
  }

  @Override
  public String technologyId() {
    return "MEDIA";
  }

  private String signLicense(final List<String> lines) {
    logger.in();
    try {
      // input raw text
      final String filename = "raw." + Instant.now().toString().replace(":", ".").replace("-", ".");
      logger.array(Log.Level.debug, "filename", filename);

      // working directory
      final Path directory = Application.getInstance().getResourcePath("licenses");
      logger.array(Log.Level.debug, "directory", directory);

      // input file for signer
      final Path input = Application.getInstance().getResourcePath("licenses", filename);
      logger.array(Log.Level.debug, "input", input);

      FileUtils.writeLines(input.toFile(), StandardCharsets.UTF_8.name(), lines);

      // path to signer
      final Path executable = Application.getInstance().getResourcePath("executable", "Test.exe");
      logger.array(Log.Level.debug, "executable", executable);

      final ProcessBuilder pb = new ProcessBuilder(executable.toAbsolutePath().toString(), filename);
      logger.log(Log.Level.debug, "created process");

      pb.directory(directory.toFile());

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


  class License {

    long toLong(final XMLGregorianCalendar date) {
      return date == null ? 0L : date.toGregorianCalendar().getTime().getTime();
    }

    public String productName;
    public String productVersion;
    public String featureName;
    public String featureVersion;
    public long featureCount;
    public XMLGregorianCalendar startDate;
    public XMLGregorianCalendar expirationDate;

    public License(final com.flexnet.external.type.Product product,
                   final com.flexnet.external.type.Feature feature,
                   final XMLGregorianCalendar startDate,
                   final XMLGregorianCalendar expirationDate) {
      this.productName = product.getName();
      this.productVersion = product.getVersion();
      this.featureName = feature.getName();
      this.featureVersion = feature.getVersion();
      this.featureCount = feature.getCount();
      this.startDate = startDate;
      this.expirationDate = expirationDate;
    }

    public void add(final License value) {
      if (key().equals(value.key())) {
        this.featureCount += value.featureCount;
      }
    }

    @JsonIgnore
    public String key() {
      return String.format("%s|%s|%s|%s|%d|%d",
                           this.productName,
                           this.productVersion,
                           this.featureName,
                           this.featureVersion,
                           toLong(this.startDate),
                           toLong(this.expirationDate));
    }
  }

  //TODO - update the formatting inline with what Tulio has posted
  @Override
  public GeneratorResponse generateLicense(final GeneratorRequest request) throws LicGeneratorException {
    logger.in();
    try {
      final Map<String, License> lines = new HashMap<>();

      request.getEntitledProducts().forEach(product -> {

        product.getFeatures().forEach(feature -> {
//          final String line = String.format("%s | %s | %s | %s | %s | %s | %d",
//                                  product.getName(),
//                                  product.getVersion(),
//                                  Utils.format(request.getStartDate(), Utils.yyyy_mm_dd),
//                                  //TODO: perpetual blows up
//                                  Utils.format(request.getExpirationDate(), Utils.yyyy_mm_dd),
//                                  feature.getName(),
//                                  feature.getVersion(),
//                                  feature.getCount());
//          logger.log(Log.Level.debug, line);

          final License license = new License(product, feature, request.getStartDate(), request.getExpirationDate());

          lines.put(license.key(), license);
        });
      });

      logger.yaml(Log.Level.debug, lines);

      final String license = Utils.safeSerializeJson(lines);

      return new GeneratorResponse() {
        {
          this.licenseFiles = makeLicenseFiles(request.getLicenseTechnology().getLicenseFileDefinitions(), license, null);
        }
      };
    }
    finally {
      logger.out();
    }
  }

  @Override
  public ConsolidatedLicense consolidateFulfillments(final FulfillmentRecordSet request) throws LicGeneratorException {

    final TypeReference<Map<String, License>> type = new TypeReference<Map<String, License>>() {

    };

    final Map<String, License> lines = new HashMap<>();

    // collate licenses
    request.getFulfillments().forEach(fulfilment -> {
      fulfilment.getLicenseFiles().forEach(file -> {
        Optional.ofNullable(file.getValue()).ifPresent(value -> {
          try {
            Utils.yaml_mapper.readValue(value.toString(), type).forEach((key, value1) -> {

              if (lines.containsKey(key)) {
                lines.get(key).add(value1);
              }
              else {
                lines.put(key, value1);
              }
            });
          }
          catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
      });
    });

    return new ConsolidatedLicense() {
      {
        final String license = Utils.safeSerializeYaml(lines);

        this.fulfillments = request.getFulfillments();

        request.getFulfillments().stream().findAny().ifPresent(fid -> {
          this.licFiles = makeLicenseFiles(fid.getLicenseTechnology().getLicenseFileDefinitions(), license, null);
        });
      }
    };
  }
}
