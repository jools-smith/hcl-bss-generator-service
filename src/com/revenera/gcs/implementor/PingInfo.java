package com.revenera.gcs.implementor;

import org.apache.commons.lang3.SystemUtils;

public class PingInfo {
  public class OS {
    public final String name = SystemUtils.OS_NAME;
    public final String version = SystemUtils.OS_VERSION;
    public final String architecture = SystemUtils.OS_ARCH;
  }

  public class ENV {
    public final Integer availableProcessors;
    public final Long freeMemory;
    public final Long totalMemory;
    public final Long maxMemory;

    ENV() {
      final Runtime runtime = Runtime.getRuntime();

      this.availableProcessors = runtime.availableProcessors();
      this.freeMemory = runtime.freeMemory();
      this.totalMemory = runtime.totalMemory();
      this.maxMemory = runtime.maxMemory();
    }
  }

  public final OS system = new OS();
  public final ENV environment = new ENV();
  public final String hostName = SystemUtils.getHostName();
  @SuppressWarnings("deprecation")
  public final String userName = SystemUtils.getUserName();

  PingInfo() {

  }

  public static PingInfo create() {
    return new PingInfo();
  }

}
