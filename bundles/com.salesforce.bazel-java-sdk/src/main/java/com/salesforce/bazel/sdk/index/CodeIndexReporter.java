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
 */
package com.salesforce.bazel.sdk.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.salesforce.bazel.sdk.index.model.CodeLocationDescriptor;

/**
 * Report generator for the contents of an index. Intentionally simple and not general purpose.
 * Subclass this to provide more custom options, or (more likely) implement your own reporter
 * for your custom needs.
 */
public class CodeIndexReporter {
    protected CodeIndex index;
    
    public CodeIndexReporter(CodeIndex index) {
        this.index = index;
    }
    

    // FILTERING
    
    /**
     * Decides what to filter out of the report. Subclasses should implement a filtering method that 
     * meets their needs.
     */
    protected boolean accept(Map<String, String> options, CodeIndexEntry entry) {
        if (options.containsKey("suppressDeprecated")) {
            CodeLocationDescriptor loc = entry.getPrimaryLocation();
            if (loc.bazelLabel.startsWith("@deprecated")) {
                // this artifact is deprecated and the user is not interested in seeing it in the report
                return false;
            }
        }
        return true;
    }
    

    
    // SIMPLE REPORT
    
    public void printIndexAsText(Map<String, String> options) {
        println("");
        println("ARTIFACT INDEX (" + index.artifactDictionary.size() + " entries)");
        println("----------------------------------------");
        for (String artifact : index.artifactDictionary.keySet()) {
            CodeIndexEntry entry = index.artifactDictionary.get(artifact);
            if (accept(options, entry)) {
                printArtifact(options, artifact, entry);
            }
        }
        println("");
        println("FILE INDEX (" + index.fileDictionary.size() + " entries)");
        println("----------------------------------------");
        for (String filename : index.fileDictionary.keySet()) {
            CodeIndexEntry entry = index.fileDictionary.get(filename);
            if (accept(options, entry)) {
                printArtifact(options, filename, entry);
            }
        }
        println("");
        println("TYPE INDEX (" + index.typeDictionary.size() + " entries)");
        println("----------------------------------------");
        for (String classname : index.typeDictionary.keySet()) {
            CodeIndexEntry entry = index.typeDictionary.get(classname);
            if (accept(options, entry)) {
                printArtifact(options, classname, entry);
            }
        }
        println("");
    }

    protected void printArtifact(Map<String, String> options, String artifact, CodeIndexEntry entry) {
        println("  " + artifact);
        if (entry.singleLocation != null) {
            println("    " + entry.singleLocation.id.locationIdentifier);
        } else {
            for (CodeLocationDescriptor loc : entry.multipleLocations) {
                println("    " + loc.id.locationIdentifier);
            }
        }
    }

    protected void println(String line) {
        System.out.println(line);
    }

    
    // CSV REPORT
    
    /**
     * Provides the list of artifacts in the index. Format: CSV
     */
    public List<String> buildArtifactReportAsCSV(Map<String, String> options) {
        List<String>  rows = new ArrayList<>();
        Set<String> artifactNames = index.artifactDictionary.keySet();
        artifactNames = new TreeSet<>(artifactNames);
        
        if (index.getOptions().doComputeArtifactAges()) {
            rows.add("Artifact Name, Bazel Label, Version, Age (in days)");
        } else {
            rows.add("Artifact Name, Bazel Label, Version");
        }
        
        for (String artifactName : artifactNames) {
            CodeIndexEntry entry = index.artifactDictionary.get(artifactName);
            
            if (accept(options, entry)) {
                if (entry.singleLocation != null) {
                    rows.add(writeCsvRow(options, artifactName, entry.singleLocation));
                } else {
                    for (CodeLocationDescriptor location : entry.multipleLocations) {
                        rows.add(writeCsvRow(options, artifactName, location));
                    }
                }
            }
        }
        
        return rows;
    }
        
    protected String writeCsvRow(Map<String, String> options, String artifactName, CodeLocationDescriptor location) {
        if (index.getOptions().doComputeArtifactAges()) {
            return artifactName + ", " + location.bazelLabel + ", " + location.version + ", " + location.ageInDays;
        }
        return artifactName + ", " + location.bazelLabel + ", " + location.version;
    }
    
    
    // HISTOGRAM
    
    /**
     * Provides the age histogram of artifacts in the index. 
     */
    public IndexArtifactHistogram buildArtifactAgeHistogramReport() {
        Set<String> artifactNames = index.artifactDictionary.keySet();
        artifactNames = new TreeSet<>(artifactNames);
        IndexArtifactHistogram histogram = new IndexArtifactHistogram();
        
        for (String artifactName : artifactNames) {
            CodeIndexEntry entry = index.artifactDictionary.get(artifactName);
            
            if (entry.singleLocation != null) {
                histogram.recordArtifactAge(entry.singleLocation.ageInDays);
            } else {
                for (CodeLocationDescriptor location : entry.multipleLocations) {
                    histogram.recordArtifactAge(location.ageInDays);
                }
            }
        }
        
        return histogram;
    }

    /**
     * Provides the age histogram of artifacts in the index. Format: CSV
     */
    public List<String> buildArtifactAgeHistogramReportAsCSV() {
        List<String>  rows = new ArrayList<>();
        IndexArtifactHistogram histogram = buildArtifactAgeHistogramReport();

        if (!index.indexOptions.doComputeArtifactAges()) {
            rows.add("Dependency age histogram could not be computed because age computation for dependencies is disabled.");
            return rows;
        }
        rows.add("Age (in Years), Number of Dependencies");
        
        for (int i = 0; i<histogram.MAX_YEAR_AGE; i++) {
            rows.add("" + i +", "+ histogram.countsPerYearOfAge[i]);
        }
        
        rows.add("-1, "+histogram.undeterminedAgeCount);
        
        return rows;
    }

    /**
     * Simple histogram, buckets divided by year. Over time this should become more sophisticated.
     * Please resist the temptation to bring in a dependency to provide a better histogram, as we
     * don't allow dependencies in the Bazel Java SDK.
     */
    public static class IndexArtifactHistogram {
        public final int MAX_YEAR_AGE = 40;
        public int[] countsPerYearOfAge = new int[MAX_YEAR_AGE];
        public int undeterminedAgeCount = 0;
        
        public void recordArtifactAge(int ageInDays) {
            if (ageInDays == -1) {
                // age not determined
                undeterminedAgeCount++;
                return;
            }
            int ageInYears = ageInDays / 365; // I will ignore your protests about leap years!
            
            if (ageInYears >= MAX_YEAR_AGE) {
                // some bogus future date was recorded as the age
                undeterminedAgeCount++;
                return;
            }
            countsPerYearOfAge[ageInYears] = countsPerYearOfAge[ageInYears] + 1;
        }
    }
}
