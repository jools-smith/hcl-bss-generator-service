package com.revenera.gcs;

import com.revenera.gcs.implementor.AbstractImplementor;
import com.revenera.gcs.implementor.ImplementorFactory;
import com.revenera.gcs.utils.AnnotationManager;
import com.revenera.gcs.utils.Diagnostics;
import com.revenera.gcs.utils.GeneratorImplementor;
import com.revenera.gcs.utils.Log;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The root of the service, registered as a listener will set stuff up when the context is initialized
 */
@WebListener
public class Application implements ServletContextListener {
  private static final Log logger = Log.create(Application.class);

  private String executable;

  /** instance */
  private static final AtomicReference<Application> singleton = new AtomicReference<>();

  public static Application singleton() {
    return singleton.get();
  }

  public static Application getInstance() {
    return singleton.get();
  }

  /** build */
  private final String build;
  public String getBuildSequence() {
    return build;
  }

  /** version */
  private final String version;
  public String getVersionDate() {
    return version;
  }

  /** implementor factory */
  private final ImplementorFactory implementorFactory = new ImplementorFactory();
  public final ImplementorFactory getImplementorFactory() {
    return implementorFactory;
  }

  /** diagnostics */
  private final Diagnostics diagnostics = new Diagnostics();
  public Diagnostics getDiagnostics() {
    return diagnostics;
  }

  private void logAttributeNames(final ServletContextEvent event) {
    final Enumeration<String> itt = event.getServletContext().getAttributeNames();
    while (itt.hasMoreElements()) {
      logger.log(Log.Level.trace, itt.nextElement());
    }
  }

  public Application() {
    /// we can reduce this potentially -- once levels have been assessed
    Log.setLoggingLevel(Log.Level.trace);

    logger.me(this);

    this.build = "1010";
    this.version = "2025.01.31";

    singleton.getAndSet(this);

    logger.log(Log.Level.info, String.format("version | %s | %s", version, build));
  }


  public Path getResourcePath(final String...parts) {
    return Paths.get(this.executable, parts);
  }

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    logger.in();

    try {
      logAttributeNames(event);

      this.executable = event.getServletContext().getRealPath("/WEB-INF");
      logger.array(Log.Level.info, "resources", getResourcePath());;

      final AnnotationManager manager = new AnnotationManager();

      final List<String> files = manager.findClassFilesInPackage(AbstractImplementor.class);

      for (final String typename : files) {

        final Class<?> type = Class.forName(typename);

        if (type.isAnnotationPresent(GeneratorImplementor.class)) {

          final GeneratorImplementor ann = type.getAnnotation(GeneratorImplementor.class);

          logger.array(Log.Level.debug, ann.technology(), type.getName());

          if (AbstractImplementor.class.isAssignableFrom(type)) {

            implementorFactory.addImplementor((AbstractImplementor) type.newInstance());
          }
        }
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
  public void contextDestroyed(final ServletContextEvent event) {
    logger.in();
    try {
      logAttributeNames(event);
    }
    catch (final Throwable t) {
      logger.exception(t);
    }
    finally {
      logger.out();
    }
  }
}
