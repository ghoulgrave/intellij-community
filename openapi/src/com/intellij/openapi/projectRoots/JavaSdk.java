/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.projectRoots;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;

public abstract class JavaSdk extends SdkType implements JavaSdkType, ApplicationComponent {
  public JavaSdk(@NonNls String name) {
    super(name);
  }

  public static JavaSdk getInstance() {
    return ApplicationManager.getApplication().getComponent(JavaSdk.class);
  }

  public final Sdk createJdk(final String jdkName, String jreHome) {
    return createJdk(jdkName, jreHome, true);
  }

  public abstract int compareTo(@NotNull String versionString, @NotNull String versionNumber);

  public abstract Sdk createJdk(@NonNls String jdkName, String home, boolean isJre);

  public static boolean checkForJdk(File file) {
    file = new File(file.getAbsolutePath() + File.separator + "bin");
    if (!file.exists()) return false;
    FileFilter fileFilter = new FileFilter() {
      @SuppressWarnings({"HardCodedStringLiteral"})
      public boolean accept(File f) {
        if (f.isDirectory()) return false;
        if (Comparing.strEqual(FileUtil.getNameWithoutExtension(f), "javac")) return true;
        if (Comparing.strEqual(FileUtil.getNameWithoutExtension(f), "javah")) return true;
        return false;
      }
    };
    File[] children = file.listFiles(fileFilter);
    return (children != null && children.length >= 2);
  }

  public static boolean checkForJre(String file){
    File ioFile = new File(new File(file.replace('/', File.separatorChar)).getAbsolutePath() + File.separator + "bin");
    if (!ioFile.exists()) return false;
    FileFilter fileFilter = new FileFilter() {
      @SuppressWarnings({"HardCodedStringLiteral"})
      public boolean accept(File f) {
        if (f.isDirectory()) return false;
        if (Comparing.strEqual(FileUtil.getNameWithoutExtension(f), "java")) return true;
        return false;
      }
    };
    File[] children = ioFile.listFiles(fileFilter);
    return (children != null && children.length >= 1);
  }


}
