package com.revenera.gcs.implementor.hcl;

import com.flexnet.external.type.GeneratorRequest;
import com.flexnet.external.type.GeneratorResponse;
import com.flexnet.external.webservice.keygenerator.LicGeneratorException;
import com.revenera.gcs.implementor.AbstractImplementor;
import com.revenera.gcs.utils.GeneratorImplementor;

import java.time.Instant;

@GeneratorImplementor(technology = "ULT")
public class NoLicenseGenerator extends AbstractImplementor {
  @Override
  public String technologyName() {
    return "Unenforced license technology";
  }

  @Override
  public String technologyId() {
    return "ULT";
  }

  @Override
  public GeneratorResponse generateLicense(final GeneratorRequest request) throws LicGeneratorException {

    return new GeneratorResponse() {
      {
        this.licenseText = "";
        this.message = "activated at " + Instant.now().toString();
      }
    };
  }
}
