package com.revenera.gcs.implementor.hcl;

import com.flexnet.external.type.*;
import com.flexnet.external.webservice.keygenerator.LicGeneratorException;
import com.revenera.gcs.implementor.AbstractImplementor;
import com.revenera.gcs.utils.GeneratorImplementor;
import com.revenera.gcs.utils.Utils;

import java.time.Instant;
import java.util.stream.Collectors;

@GeneratorImplementor(technology = "DID")
public class CsiLicenseGenerator extends AbstractImplementor {
  @Override
  public String technologyName() {
    return "FRI";
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
  public GeneratorResponse generateLicense(final GeneratorRequest request) throws LicGeneratorException {

    final StringBuilder license = new StringBuilder();

    request.getEntitledProducts().stream().forEach(product -> {

      product.getFeatures().stream().forEach(feature -> {
        license.append(String.format("%s | %s | %s | %s | %s | %s | %d",
                                     product.getName(),
                                     product.getVersion(),
                                     Utils.format(request.getStartDate(),Utils.yyyy_mm_dd),
                                     Utils.format(request.getExpirationDate(),Utils.yyyy_mm_dd),
                                     feature.getName(),
                                     feature.getVersion(),
                                     feature.getCount()))
               .append("\n");

      });
    });

    return new GeneratorResponse() {
      {
        this.licenseFiles = makeLicenseFiles(request.getLicenseTechnology().getLicenseFileDefinitions(), license.toString(), null);

        setLicenseText("#Generated by external server at " + Instant.now().toString());

        setLicenseFileName("nofile.txt");
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
}
