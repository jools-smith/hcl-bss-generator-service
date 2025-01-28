package com.revenera.gcs.implementor;

import com.flexnet.external.webservice.keygenerator.LicenseGeneratorServiceInterface;
import com.revenera.gcs.utils.Log;

import java.util.HashMap;
import java.util.Map;

public class ImplementorFactory {
  private final static Log logger = Log.create(ImplementorFactory.class);

  public final static String default_technology_name = "DEF";

  private final Map<String, LicenseGeneratorServiceInterface> implementors = new HashMap<>();

  public void addImplementor(final AbstractImplementor imp) {

    logger.log(Log.Level.debug, imp.technologyName() + " -> " + imp.getClass().getSimpleName());

    this.implementors.put(imp.technologyName(), imp);
  }

  public LicenseGeneratorServiceInterface getDefaultImplementor() {
    return this.implementors.get(default_technology_name);
  }

  public LicenseGeneratorServiceInterface getImplementor(final String technology) {
    if (this.implementors.containsKey(technology)) {
      return this.implementors.get(technology);
    }
    else {
      return getDefaultImplementor();
    }
  }
}
