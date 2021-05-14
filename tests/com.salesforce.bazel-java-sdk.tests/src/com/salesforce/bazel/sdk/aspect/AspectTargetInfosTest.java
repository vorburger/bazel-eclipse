/**
 * Copyright (c) 2019, Salesforce.com, Inc. All rights reserved.
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
package com.salesforce.bazel.sdk.aspect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import com.salesforce.bazel.sdk.model.BazelTargetKind;

public class AspectTargetInfosTest {

    @Test
    public void testLookupByLabel() {
        AspectTargetInfo lib = getAspectTargetInfo("foo1", BazelTargetKind.JAVA_LIBRARY, "a/b/c/d/Foo.java");
        AspectTargetInfo test = getAspectTargetInfo("foo2", BazelTargetKind.JAVA_TEST, "a/b/c/d/Foo.java");
        AspectTargetInfo bin = getAspectTargetInfo("foo3", BazelTargetKind.JAVA_BINARY, "a/b/c/d/Foo.java");
        AspectTargetInfo seleniumTest =
                getAspectTargetInfo("foo4", BazelTargetKind.JAVA_WEB_TEST_SUITE, "a/b/c/d/Foo.java");

        AspectTargetInfos apis = new AspectTargetInfos(lib, test, bin, seleniumTest);

        assertSame(lib, apis.lookupByLabel("foo1"));
        assertSame(test, apis.lookupByLabel("foo2"));
        assertSame(bin, apis.lookupByLabel("foo3"));
        assertSame(seleniumTest, apis.lookupByLabel("foo4"));
        assertNull(apis.lookupByLabel("blah"));
    }

    @Test
    public void testLookupByTargetKind__singleTargetKind() {
        AspectTargetInfo lib = getAspectTargetInfo("foo1", BazelTargetKind.JAVA_LIBRARY, "a/b/c/d/Foo.java");
        AspectTargetInfo test = getAspectTargetInfo("foo2", BazelTargetKind.JAVA_TEST, "a/b/c/d/Foo.java");
        AspectTargetInfo bin = getAspectTargetInfo("foo3", BazelTargetKind.JAVA_BINARY, "a/b/c/d/Foo.java");
        AspectTargetInfo seleniumTest =
                getAspectTargetInfo("foo4", BazelTargetKind.JAVA_WEB_TEST_SUITE, "a/b/c/d/Foo.java");

        AspectTargetInfos apis = new AspectTargetInfos(lib, test, bin, seleniumTest);

        Collection<AspectTargetInfo> infos = apis.lookupByTargetKind(EnumSet.of(BazelTargetKind.JAVA_TEST));
        assertEquals(1, infos.size());
        assertSame(test, infos.iterator().next());

        infos = apis.lookupByTargetKind(EnumSet.of(BazelTargetKind.JAVA_WEB_TEST_SUITE));
        assertEquals(1, infos.size());
        assertSame(seleniumTest, infos.iterator().next());
    }

    @Test
    public void testLookupByTargetKind__multipleTargetKinds() {
        AspectTargetInfo lib = getAspectTargetInfo("foo1", BazelTargetKind.JAVA_LIBRARY, "a/b/c/d/Foo.java");
        AspectTargetInfo test = getAspectTargetInfo("foo2", BazelTargetKind.JAVA_TEST, "a/b/c/d/Foo.java");
        AspectTargetInfo bin = getAspectTargetInfo("foo3", BazelTargetKind.JAVA_BINARY, "a/b/c/d/Foo.java");
        AspectTargetInfo seleniumTest =
                getAspectTargetInfo("foo4", BazelTargetKind.JAVA_WEB_TEST_SUITE, "a/b/c/d/Foo.java");

        AspectTargetInfos apis = new AspectTargetInfos(lib, test, bin, seleniumTest);

        Collection<AspectTargetInfo> infos =
                apis.lookupByTargetKind(EnumSet.of(BazelTargetKind.JAVA_TEST, BazelTargetKind.JAVA_BINARY));

        assertEquals(2, infos.size());
        assertTrue(infos.contains(test));
        assertTrue(infos.contains(bin));
    }

    @Test
    public void testLookupByRootSourcePath() {
        AspectTargetInfo api = getAspectTargetInfo("foo", "a/b/c/d/Foo.java");

        AspectTargetInfos infos = new AspectTargetInfos(api);

        assertSame(api, infos.lookupByRootSourcePath("a/b/c/d/Foo.java").iterator().next());
        assertSame(api, infos.lookupByRootSourcePath("a/b/c/d").iterator().next());
        assertSame(api, infos.lookupByRootSourcePath("a/b/c/").iterator().next());
        assertSame(api, infos.lookupByRootSourcePath("a/b").iterator().next());
        assertSame(api, infos.lookupByRootSourcePath("a/").iterator().next());
        assertSame(api, infos.lookupByRootSourcePath("a").iterator().next());
        assertEquals(0, infos.lookupByRootSourcePath("f").size());
    }

    @Test
    public void testLookupByRootSourcePath__noSubstringMatch() {
        AspectTargetInfo api = getAspectTargetInfo("myclass", "projects/services/scone/MyClass.java");

        AspectTargetInfos apis = new AspectTargetInfos(api);
        Collection<AspectTargetInfo> infos = apis.lookupByRootSourcePath("projects/services/scone");

        assertEquals(1, infos.size());
        assertSame(api, infos.iterator().next());
        assertEquals(0, apis.lookupByRootSourcePath("projects/services/scon").size());
    }

    @Test
    public void testLookupByRootSourcePath__multipleMatching() {
        AspectTargetInfo foo = getAspectTargetInfo("foo", "a/b/c/aaa/Foo.java");
        AspectTargetInfo blah = getAspectTargetInfo("blah", "a/b/c/zzz/Blah.java");

        AspectTargetInfos apis = new AspectTargetInfos(foo, blah);

        Collection<AspectTargetInfo> infos = apis.lookupByRootSourcePath("a/b/c");
        assertEquals(2, infos.size());
        assertTrue(infos.contains(foo));
        assertTrue(infos.contains(blah));
        infos = apis.lookupByRootSourcePath("a/b/c/aaa");
        assertEquals(1, infos.size());
        assertTrue(infos.contains(foo));
    }

    @Test
    public void testLookupByRootSourcePath__sourcesWithCommonRootPathValidation() {
        AspectTargetInfo foo = getAspectTargetInfo("foo", "a/b/c/aaa/ccc/Foo.java", "a/b/c/aaa/ddd/Blah.java");

        AspectTargetInfos apis = new AspectTargetInfos(foo);

        Collection<AspectTargetInfo> infos = apis.lookupByRootSourcePath("a/b/c");
        assertEquals(1, infos.size());

        infos = apis.lookupByRootSourcePath("a/b/c/aaa");
        assertEquals(1, infos.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testLookupByRootSourcePath__sourcesWithoutCommonRootPathValidation_partialPath() {
        AspectTargetInfo foo = getAspectTargetInfo("foo", "a/b/c/aaa/Foo.java", "a/b/c/zzz/Blah.java");

        AspectTargetInfos apis = new AspectTargetInfos(foo);

        apis.lookupByRootSourcePath("a/b/c/aaa");
    }

    @Test(expected = IllegalStateException.class)
    public void testLookupByRootSourcePath__sourcesWithoutCommonRootPathValidation_fullPath() {
        AspectTargetInfo foo = getAspectTargetInfo("foo", "a/b/c/aaa/Foo.java", "x/y/z/aaa/Blah.java");

        AspectTargetInfos apis = new AspectTargetInfos(foo);

        apis.lookupByRootSourcePath("a/b/c");
    }

    private static AspectTargetInfo getAspectTargetInfo(String label, String... sourcePaths) {
        return getAspectTargetInfo(label, BazelTargetKind.JAVA_LIBRARY, sourcePaths);
    }

    private static AspectTargetInfo getAspectTargetInfo(String label, BazelTargetKind targetKind,
            String... sourcePaths) {
        List<String> sourcePathList = Arrays.asList(sourcePaths);

        return new AspectTargetInfo(new File(""), new ArrayList<>(), new ArrayList<>(), "some/path",
                targetKind.toString().toLowerCase(), label, new ArrayList<>(), sourcePathList,
                "main-class");
    }

}
