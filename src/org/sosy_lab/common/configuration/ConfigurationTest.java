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
package org.sosy_lab.common.configuration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;

public class ConfigurationTest {

  private static enum TestEnum {
    E1, E2, E3;
  }

  @Options
  private static class TestEnumSetOptions {

    @Option(description="Test injection of EnumSet")
    private EnumSet<? extends TestEnum> values = EnumSet.of(TestEnum.E1, TestEnum.E3);
  }

  @Before
  public void setUp() {
    Configuration.setBuilderFactory(null);
  }

  @Test
  public void testEnumSet() throws InvalidConfigurationException {
    Configuration config = Configuration.builder()
                           .setOption("values", "E3, E2")
                           .build();

    TestEnumSetOptions options = new TestEnumSetOptions();
    config.inject(options);
    assertEquals(EnumSet.of(TestEnum.E2, TestEnum.E3), options.values);
  }

  @Test
  public void testEnumSetDefault() throws InvalidConfigurationException {
    testDefault(TestEnumSetOptions.class);
  }


  @Options
  private static class TestPatternOptions {

    @Option(description="Test injection of Pattern instances")
    private Pattern regexp = Pattern.compile(".*");
  }

  @Test
  public void testPattern() throws InvalidConfigurationException {
    Configuration config = Configuration.builder()
                           .setOption("regexp", "foo.*bar")
                           .build();

    TestPatternOptions options = new TestPatternOptions();
    config.inject(options);
    assertTrue(options.regexp.matcher("fooTESTbar").matches());
    assertFalse(options.regexp.matcher("barTESTfoo").matches());
  }

  @Test(expected=InvalidConfigurationException.class)
  public void testInvalidPattern() throws InvalidConfigurationException {
    Configuration config = Configuration.builder()
                           .setOption("regexp", "*foo.*bar")
                           .build();

    TestPatternOptions options = new TestPatternOptions();
    config.inject(options);
  }

  @Test
  public void testPatternDefault() throws InvalidConfigurationException {
    testDefault(TestPatternOptions.class);
  }

  @Test
  public void shouldReturnCustomFactory() throws Exception {
    AbstractConfigurationBuilderFactory mockFactory = mock(AbstractConfigurationBuilderFactory.class);
    Configuration.setBuilderFactory(mockFactory);

    assertEquals(mockFactory, Configuration.getBuilderFactory());
  }

  @Test
  public void shouldReturnDefaultBuilder() throws Exception {
    ConfigurationBuilder builder = Configuration.builder();

    assertTrue(builder instanceof Builder);
  }

  @Test
  public void shouldReturnCustomBuilder() throws Exception {
    ConfigurationBuilder mockBuilder = mock(ConfigurationBuilder.class);
    AbstractConfigurationBuilderFactory stubFactory = mock(AbstractConfigurationBuilderFactory.class);
    when(stubFactory.getBuilder()).thenReturn(mockBuilder);

    Configuration.setBuilderFactory(stubFactory);

    assertEquals(mockBuilder, Configuration.builder());
  }

  /**
   * This is parameterized test case that checks whether the injection with
   * a default configuration does not change the value of the fields.
   * @param clsWithOptions A class with some declared options and a default constructor.
   */
  private void testDefault(Class<?> clsWithOptions) throws InvalidConfigurationException {
    Configuration config = Configuration.defaultConfiguration();

    try {
      Constructor<?> constructor = clsWithOptions.getDeclaredConstructor(new Class<?>[0]);
      constructor.setAccessible(true);

      Object injectedInstance = constructor.newInstance();
      config.inject(injectedInstance);

      Object defaultInstance = constructor.newInstance();

      for (Field field : clsWithOptions.getFields()) {
        field.setAccessible(true);
        Object injectedValue = field.get(injectedInstance);
        Object defaultValue = field.get(defaultInstance);

        assertEquals(defaultValue, injectedValue);
      }

    } catch (ReflectiveOperationException e) {
      Throwables.propagate(e);
    }
  }
}
