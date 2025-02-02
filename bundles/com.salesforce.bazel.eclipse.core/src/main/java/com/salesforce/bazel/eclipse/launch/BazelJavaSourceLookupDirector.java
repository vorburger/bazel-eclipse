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
package com.salesforce.bazel.eclipse.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

import com.salesforce.bazel.eclipse.component.ComponentContext;

/**
 * Copied and adapted from org.eclipse.jdt.internal.launching.JavaSourceLookupDirector.
 */
public class BazelJavaSourceLookupDirector extends AbstractSourceLookupDirector {

    private static final String JAVA_SRC_PATH_COMP = "org.eclipse.jdt.launching.sourceLookup.javaSourcePathComputer";

    private final IJavaProject mainProject;
    private final List<IJavaProject> otherProjects;

    public BazelJavaSourceLookupDirector(IJavaProject mainProject, List<IJavaProject> otherProjects) {
        this.mainProject = mainProject;
        this.otherProjects = otherProjects;
        setSourcePathComputer(getLaunchManager().getSourcePathComputer(JAVA_SRC_PATH_COMP));
    }

    @Override
    public void initializeParticipants() {
        addParticipants(new ISourceLookupParticipant[] { new JavaSourceLookupParticipant() });
        List<ISourceContainer> sourceContainers = new ArrayList<>();
        sourceContainers.add(new JavaProjectSourceContainer(mainProject));
        addSourceJarsForDependencies(mainProject, sourceContainers);
        for (IJavaProject project : otherProjects) {
            sourceContainers.add(new JavaProjectSourceContainer(project));
            addSourceJarsForDependencies(project, sourceContainers);
        }
        setSourceContainers(sourceContainers.toArray(new ISourceContainer[sourceContainers.size()]));
    }

    private static void addSourceJarsForDependencies(IJavaProject project, List<ISourceContainer> sourceContainers) {
        IClasspathEntry[] resolvedClasspath =
                ComponentContext.getInstance().getJavaCoreHelper().getResolvedClasspath(project, true);
        for (IClasspathEntry e : resolvedClasspath) {
            if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                IPath sourceAttachmentPath = e.getSourceAttachmentPath();
                if (sourceAttachmentPath != null) {
                    sourceContainers.add(new ExternalArchiveSourceContainer(sourceAttachmentPath.toOSString(), false));
                }
            }
        }
    }

    private static ILaunchManager getLaunchManager() {
        return DebugPlugin.getDefault().getLaunchManager();
    }

}
