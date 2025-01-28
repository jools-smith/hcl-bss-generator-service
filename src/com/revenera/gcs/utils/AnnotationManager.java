package com.revenera.gcs.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationManager {
  static final String class_suffix = ".class";

  private final String root;

  public AnnotationManager() throws URISyntaxException {
    final URL url = Objects.requireNonNull(
            Thread.currentThread()
                    .getContextClassLoader()
                    .getResource(""));

    this.root =  Paths.get(url.toURI()).toFile() + File.separator;
  }

  public Optional<List<File>> getClassFilesInPath(final File directory) {
    final File[] files = directory.listFiles();
    if (Objects.nonNull(files)) {

      final List<File> names = new ArrayList<>();

      for (final File file : files) {
        if (file.isFile()) {
          if (file.getName().endsWith(class_suffix)) {
            names.add(file.getAbsoluteFile());
          }
        }
        else if (file.isDirectory()) {
          getClassFilesInPath(file).ifPresent(names::addAll);
        }
      }
      return Optional.of(names);
    }
    else {
      return Optional.empty();
    }
  }

  public String fileToPackageName(final File file) {
    return file.getAbsoluteFile()
            .toString()
            .replace(root, "")
            .replace(class_suffix, "")
            .replace(File.separator, ".");
  }

  public List<String> findClassFilesInPackage(final Class<?> type) throws IOException, URISyntaxException {

    final List<File> files = new ArrayList<>();

    final String packageName = type.getPackage().getName();

    final Path path = Paths.get(packageName.replace(".", "/"));

    final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path.toString());

    if (resources.hasMoreElements()) {
      getClassFilesInPath(Paths.get(resources.nextElement().toURI()).toFile()).ifPresent(files::addAll);
    }

    return files.stream().map(this::fileToPackageName).collect(Collectors.toList());
  }
}