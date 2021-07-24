/**
 * Copyright (c) 2021, Salesforce.com, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package com.salesforce.bazel.sdk.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.salesforce.bazel.sdk.path.FSPathHelper;

/**
 * SourcePath helps us split paths to source files into the source directory and pkg+file path.
 * src/main/java/com/salesforce/foo/Foo.java => src/main/java + com/salesforce/foo/Foo.java
 * <p>
 * In order for this to work, the caller needs to know the package name (com/salesforce/foo) through some other means
 * (e.g. parse the package line in the java file).
 */
public class SourcePathTest {

    @Test
    public void happyTests() {
        // Maven main
        String filepath = seps("src/main/java/com/salesforce/foo/Foo.java");
        String pkgpath = seps("com/salesforce/foo");
        SourcePath sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNotNull(sp);
        assertEquals(seps("src/main/java"), sp.pathToDirectory);
        assertEquals(seps("com/salesforce/foo/Foo.java"), sp.pathToFile);

        // Maven test
        filepath = seps("src/test/java/com/salesforce/foo/Foo.java");
        pkgpath = seps("com/salesforce/foo");
        sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNotNull(sp);
        assertEquals(seps("src/test/java"), sp.pathToDirectory);
        assertEquals(seps("com/salesforce/foo/Foo.java"), sp.pathToFile);

        // default package
        filepath = seps("src/main/java/Foo.java");
        pkgpath = seps("");
        sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNotNull(sp);
        assertEquals(seps("src/main/java"), sp.pathToDirectory);
        assertEquals("Foo.java", sp.pathToFile);

        // custom
        filepath = seps("sources/com/salesforce/foo/Foo.java");
        pkgpath = seps("com/salesforce/foo");
        sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNotNull(sp);
        assertEquals("sources", sp.pathToDirectory);
        assertEquals(seps("com/salesforce/foo/Foo.java"), sp.pathToFile);
    }

    @Test
    public void unhappyTests() {
        // package mismatch (bar vs foo)
        String filepath = seps("src/main/java/com/salesforce/foo/Foo.java");
        String pkgpath = seps("com/salesforce/bar");
        SourcePath sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNull(sp);

        // subpackage
        filepath = seps("src/main/com/salesforce/foo/bar/Foo.java");
        pkgpath = seps("com/salesforce/foo");
        sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNull(sp);

        // no path for file
        filepath = seps("Foo.java");
        pkgpath = seps("com/salesforce/foo");
        sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNull(sp);

        // null path
        sp = SourcePath.splitNamespacedPath(null, "something");
        assertNull(sp);

        // null package
        sp = SourcePath.splitNamespacedPath("Foo.java", null);
        assertNull(sp);

        // Forgot the file
        filepath = seps("src/main/java/com/salesforce/foo");
        pkgpath = seps("com/salesforce/foo");
        sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNull(sp);

        // Forgot the file 2
        filepath = seps("src/main/java/com/salesforce/foo/");
        pkgpath = seps("com/salesforce/foo");
        sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNull(sp);
    }

    @Test
    public void edgeCaseTests() {
        // Duped path elements
        String filepath = seps("src/main/java/com/salesforce/foo/com/salesforce/foo/Foo.java");
        String pkgpath = seps("com/salesforce/foo");
        SourcePath sp = SourcePath.splitNamespacedPath(filepath, pkgpath);
        assertNotNull(sp);
        assertEquals(seps("src/main/java/com/salesforce/foo"), sp.pathToDirectory);
        assertEquals(seps("com/salesforce/foo/Foo.java"), sp.pathToFile);
    }

    // convert unix paths to windows paths, if running tests on windows
    private String seps(String path) {
        return FSPathHelper.osSeps(path);
    }
}
