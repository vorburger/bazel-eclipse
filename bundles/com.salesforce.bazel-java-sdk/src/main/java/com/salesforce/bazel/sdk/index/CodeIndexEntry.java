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

import com.salesforce.bazel.sdk.index.model.CodeLocationDescriptor;

/**
 * A entry that is the value for each map in the CodeIndex. This class strives to be light on memory since there can be
 * tens of thousands, so we don't create a List for single length entries.
 * <p>
 * For example, there will be an instance of this class for each code artifact (e.g. a jar file). This entry will consolidate
 * multiple found locations of the file if it is found multiple times on the file system.
 */
public class CodeIndexEntry {
    public CodeLocationDescriptor singleLocation = null;
    public List<CodeLocationDescriptor> multipleLocations = null;

    public void addLocation(CodeLocationDescriptor newLocation) {
        if (multipleLocations != null) {
            for (CodeLocationDescriptor existing : multipleLocations) {
                if (existing.id.locationIdentifier.equals(newLocation.id.locationIdentifier)) {
                    // somehow we already added this (soft link?)
                    return;
                }
            }
            multipleLocations.add(newLocation);
        } else if (singleLocation != null) {
            if (singleLocation.id.locationIdentifier.equals(newLocation.id.locationIdentifier)) {
                // somehow we already added this (soft link?)
                return;
            }
            multipleLocations = new ArrayList<>(2);
            multipleLocations.add(singleLocation);
            multipleLocations.add(newLocation);
            singleLocation = null;
        } else {
            singleLocation = newLocation;
        }
    }
    
    /**
     * The canonical location for this artifact. If there were multiple found locations, this method will
     * return the 'primary' location, which is not well defined (normally the first location it was found).
     */
    public CodeLocationDescriptor getPrimaryLocation() {
        if (multipleLocations != null) {
            return multipleLocations.get(0);
        }
        return singleLocation;
        
    }
}
