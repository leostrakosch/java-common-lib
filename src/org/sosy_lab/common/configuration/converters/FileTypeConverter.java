/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common.configuration.converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.io.Paths;

import com.google.common.collect.ImmutableSet;

/**
 * A {@link TypeConverter} for options of type {@link File} or {@link Path} which offers some
 * additional features like a common base directory for all output files.
 * In order to use these features, the options need to be annotated with
 * {@link FileOption}.
 *
 * This type converter should be registered for the type {@link FileOption}.
 *
 * The additional features are:
 * - All specified relative paths are resolved against a given root directory.
 * - All relative paths of output files are resolved against a separate output directory.
 * - All output files can be disabled by a central switch.
 *
 * In order to configure these features, the normal configuration options are used.
 */
@Options
public class FileTypeConverter implements TypeConverter {

  private static final ImmutableSet<Class<?>> SUPPORTED_TYPES = ImmutableSet.<Class<?>>of(
      File.class, Path.class, PathTemplate.class);

  @Option(name="output.path", description="directory to put all output files in")
  private String outputDirectory = "output/";
  private final Path outputPath;

  @Option(name="output.disable", description="disable all default output files"
    + "\n(any explicitly given file will still be written)")
  private boolean disableOutput = false;

  @Option (description="base directory for all input & output files"
    + "\n(except for the configuration file itself)")
  private String rootDirectory = ".";
  private final Path rootPath;

  public FileTypeConverter(Configuration config) throws InvalidConfigurationException {
    config.inject(this, FileTypeConverter.class);

    rootPath = Paths.get(rootDirectory);
    outputPath = rootPath.resolve(outputDirectory);
  }

  public String getOutputDirectory() {
    return outputPath.toString();
  }

  public Path getOutputPath() {
    return outputPath;
  }

  private void checkApplicability(Class<?> type, @Nullable Annotation secondaryOption, String optionName) {
    if (!SUPPORTED_TYPES.contains(type)
        || !(secondaryOption instanceof FileOption)) {

      throw new UnsupportedOperationException("A FileTypeConverter can handle only options of type File and with a @FileOption annotation, but " + optionName + " does not fit.");
    }
  }

  @Override
  public Object convert(String optionName, String pValue, Class<?> pType, Type pGenericType,
      Annotation secondaryOption, Path pSource) throws InvalidConfigurationException {

    checkApplicability(pType, secondaryOption, optionName);

    return handleFileOption(optionName, Paths.get(pValue),
        ((FileOption)secondaryOption).value(), pType, pSource);
  }

  @Override
  public <T> T convertDefaultValue(String optionName, T pDefaultValue, Class<T> pType, Type pGenericType,
      Annotation secondaryOption) throws InvalidConfigurationException {

    checkApplicability(pType, secondaryOption, optionName);

    FileOption.Type typeInfo = ((FileOption)secondaryOption).value();

    if (pDefaultValue == null) {
      if (typeInfo == FileOption.Type.REQUIRED_INPUT_FILE) {
        throw new UnsupportedOperationException("The option " + optionName + " specifies a required input file, but the option is neither required nor has a default value.");
      }

      return null;
    }

    if (disableOutput && typeInfo == FileOption.Type.OUTPUT_FILE) {
      // disable output by setting the option to null
      return null;
    }

    Path defaultValue;
    if (pType.equals(File.class)) {
      defaultValue = Paths.get((File) pDefaultValue);
    } else if (pType.equals(PathTemplate.class)) {
      defaultValue = Paths.get(((PathTemplate)pDefaultValue).getTemplate());
    } else {
      defaultValue = (Path)pDefaultValue;
    }

    @SuppressWarnings("unchecked")
    T value = (T)handleFileOption(optionName, defaultValue, typeInfo, pType, null);
    return value;
  }


  /** This function returns a file. It sets the path of the file to
   * the given outputDirectory in the given rootDirectory.
   *
   * @param optionName name of option only for error handling
   * @param file the file name to adjust
   * @param typeInfo info about the type of the file (outputfile, inputfile) */
  private Object handleFileOption(final String optionName, Path file, final FileOption.Type typeInfo,
          final Class<?> targetType, final Path source)
          throws InvalidConfigurationException {

    if (typeInfo == FileOption.Type.OUTPUT_FILE) {
      file = outputPath.resolve(file);
    } else if (source != null) {
      Path baseDir = source.getParent();
      if (baseDir != null) {
        file = baseDir.resolve(file);
      }
    } else {
      file = rootPath.resolve(file);
    }

    if (file.isDirectory()) {
      throw new InvalidConfigurationException("Option " + optionName
          + " specifies a directory instead of a file: " + file);
    }

    if (typeInfo == FileOption.Type.REQUIRED_INPUT_FILE) {
      try {
        Files.checkReadableFile(file);
      } catch (FileNotFoundException e) {
        throw new InvalidConfigurationException("Option " + optionName
            + " specifies an invalid input file: " + e.getMessage(), e);
      }
    }

    if (targetType.equals(File.class)) {
      return file.toFile();
    } else if (targetType.equals(PathTemplate.class)) {
      return PathTemplate.ofFormatString(file.toString());
    } else {
      assert targetType.equals(Path.class);
      return file;
    }
  }
}
