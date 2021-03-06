/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import com.google.common.io.CharSource;
import com.google.common.testing.AbstractPackageSanityTests;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.sosy_lab.common.Classes;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    ignoreClasses(Classes.IS_GENERATED);

    setDefault(String[].class, new String[] {"test"});
    setDefault(Path.class, Paths.get("test"));
    setDefault(Configuration.class, Configuration.defaultConfiguration());
    setDefault(CharSource.class, CharSource.wrap("test"));
    setDefault(Optional.class, Optional.empty());
  }
}
