/**
 * Copyright (c) 2020, Salesforce.com, Inc. All rights reserved.
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
package com.salesforce.bazel.eclipse.project;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import com.salesforce.bazel.eclipse.runtime.api.ResourceHelper;
import com.salesforce.bazel.sdk.model.BazelPackageLocation;

/**
 * Useful utils for Eclipse+Bazel projects
 */
public class EclipseProjectUtils {

    public static Set<IProject> getDownstreamProjectsOf(IProject project, IJavaProject[] allImportedProjects) {
        Set<IProject> downstreamProjects = new LinkedHashSet<>(); // cannot be a TreeSet because Project doesn't implement Comparable
        collectDownstreamProjects(project, downstreamProjects, allImportedProjects);
        return downstreamProjects;
    }

    // determines all downstream projects, including transitives, of the specified "upstream" project, by looking at the
    // specified "allImportedProjects", and adds them to the specified "downstreams" Set.
    private static void collectDownstreamProjects(IProject upstream, Set<IProject> downstreams,
            IJavaProject[] allImportedProjects) {
        for (IJavaProject project : allImportedProjects) {
            try {
                for (String requiredProjectName : project.getRequiredProjectNames()) {
                    String upstreamProjectName = upstream.getName();
                    if (upstreamProjectName.equals(requiredProjectName)) {
                        IProject downstream = project.getProject();
                        if (!downstreams.contains(downstream)) {
                            downstreams.add(downstream);
                            collectDownstreamProjects(downstream, downstreams, allImportedProjects);
                        }
                    }
                }
            } catch (JavaModelException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    /**
     * Uses the last token in the Bazel package token (e.g. apple-api for //projects/libs/apple-api) for the name. But
     * if another project has already been imported with the same name, start appending a number to the name until it
     * becomes unique.
     */
    public static String computeEclipseProjectNameForBazelPackage(BazelPackageLocation packageInfo,
            List<IProject> previouslyImportedProjects, List<IProject> currentlyImportedProjectsList) {
        String packageName = packageInfo.getBazelPackageNameLastSegment();
        String finalPackageName = packageName;
        int index = 2;

        boolean foundUniqueName = false;
        while (!foundUniqueName) {
            foundUniqueName = true;
            if (doesProjectNameConflict(previouslyImportedProjects, finalPackageName)
                    || doesProjectNameConflict(currentlyImportedProjectsList, finalPackageName)) {
                finalPackageName = packageName + index;
                index++;
                foundUniqueName = false;
            }
        }
        return finalPackageName;
    }

    /**
     * Determines if candidate projectName conflicts with an existing project
     */
    public static boolean doesProjectNameConflict(List<IProject> existingProjectsList, String projectName) {
        for (IProject otherProject : existingProjectsList) {
            String otherProjectName = otherProject.getName();
            if (projectName.equals(otherProjectName)) {
                return true;
            }
        }
        return false;
    }

    public static void addNatureToEclipseProject(IProject eclipseProject, String nature, ResourceHelper resourceHelper) throws CoreException {
        if (!eclipseProject.hasNature(nature)) {
            IProjectDescription eclipseProjectDescription = resourceHelper.getProjectDescription(eclipseProject);
            String[] prevNatures = eclipseProjectDescription.getNatureIds();
            String[] newNatures = new String[prevNatures.length + 1];
            System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
            newNatures[prevNatures.length] = nature;
            eclipseProjectDescription.setNatureIds(newNatures);

            resourceHelper.setProjectDescription(eclipseProject, eclipseProjectDescription);
        }
    }
}
