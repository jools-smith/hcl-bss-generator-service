package com.flexnet.external.webservice.keygenerator;

import com.flexnet.external.type.*;
import com.revenera.gcs.Application;
import com.revenera.gcs.ServiceBase;
import com.revenera.gcs.utils.Log;

import javax.jws.WebService;

@WebService(
        endpointInterface = "com.flexnet.external.webservice.keygenerator.LicenseGeneratorServiceInterface",
        wsdlLocation = "WEB-INF/wsdl/schema/LicenseGeneratorService.wsdl"
)
public class LicenseGeneratorServiceImpl extends ServiceBase implements LicenseGeneratorServiceInterface {

  @Override
  public PingResponse ping(final PingRequest payload) throws LicGeneratorException {
    super.logger.in();

    super.logger.yaml(Log.Level.trace, payload);

    try {
      return Application.getInstance().getImplementorFactory().getImplementor(payload.getStr()).ping(payload);
    }
    catch (final Throwable t) {
      throw new LicGeneratorException(t.getMessage(), this.serviceException.apply(t));
    }
    finally {
      super.logger.out();
    }
  }

  @Override
  public Status validateProduct(final ProductRequest payload) throws LicGeneratorException {
    super.logger.in();

    try {
      final String tech = super.getLicenseTechnology(payload);

      return Application.getInstance().getImplementorFactory().getImplementor(tech).validateProduct(payload);
    }
    catch (final Throwable t) {
      throw new LicGeneratorException(t.getMessage(), this.serviceException.apply(t));
    }
    finally {
      super.logger.out();
    }
  }

  @Override
  public Status validateLicenseModel(final LicenseModelRequest payload) throws LicGeneratorException {
    super.logger.in();

    try {
      final String tech = super.getLicenseTechnology(payload);

      return Application.getInstance().getImplementorFactory().getImplementor(tech).validateLicenseModel(payload);
    }
    catch (final Throwable t) {
      throw new LicGeneratorException(t.getMessage(), this.serviceException.apply(t));
    }
    finally {
      super.logger.out();
    }
  }

  @Override
  public GeneratorResponse generateLicense(final GeneratorRequest payload) throws LicGeneratorException {
    super.logger.in();


    try {
      final String tech = super.getLicenseTechnology(payload);

      return Application.getInstance().getImplementorFactory().getImplementor(tech).generateLicense(payload);
    }
    catch (final Throwable t) {
      throw new LicGeneratorException(t.getMessage(), this.serviceException.apply(t));
    }
    finally {
      super.logger.out();
    }
  }

  @Override
  public ConsolidatedLicense consolidateFulfillments(final FulfillmentRecordSet payload) throws LicGeneratorException {
    super.logger.in();

    try {
      final String tech = super.getLicenseTechnology(payload);

      return Application.getInstance().getImplementorFactory().getImplementor(tech).consolidateFulfillments(payload);
    }
    catch (final Throwable t) {
      throw new LicGeneratorException(t.getMessage(), this.serviceException.apply(t));
    }
    finally {
      super.logger.out();
    }
  }

  @Override
  public LicenseFileDefinitionMap generateLicenseFilenames(final GeneratorRequest payload) throws LicGeneratorException {
    super.logger.in();

    try {
      final String tech = super.getLicenseTechnology(payload);

      return Application.getInstance().getImplementorFactory().getImplementor(tech).generateLicenseFilenames(payload);
    }
    catch (final Throwable t) {
      throw new LicGeneratorException(t.getMessage(), this.serviceException.apply(t));
    }
    finally {
      super.logger.out();
    }
  }


  @Override
  public LicenseFileDefinitionMap generateConsolidatedLicenseFilenames(final ConsolidatedLicenseResquest payload)
      throws LicGeneratorException {
    super.logger.in();
    try {
      final String tech = super.getLicenseTechnology(payload);

      return Application.getInstance().getImplementorFactory().getImplementor(tech).generateConsolidatedLicenseFilenames(payload);
    }
    catch (final Throwable t) {
      throw new LicGeneratorException(t.getMessage(), this.serviceException.apply(t));
    }
    finally {
      super.logger.out();
    }
  }

  @Override
  public String generateCustomHostIdentifier(final HostIdRequest payload) throws LicGeneratorException {
    super.logger.in();
    try {
      return Application.getInstance().getImplementorFactory().getDefaultImplementor().generateCustomHostIdentifier(payload);
    }
    catch (final Throwable t) {
      throw new LicGeneratorException(t.getMessage(), this.serviceException.apply(t));
    }
    finally {
      super.logger.out();
    }
  }
}
