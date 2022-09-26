/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2022 The MITRE Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.niem.xsd;

import org.javatuples.Pair;

/**
 * A record for appinfo attributes found on global declarations and definitions,
 * or on xs:element declarations found within a complex type definition
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public record Appinfo (
    String nsuri,                       // namespace containing the appinfo attribute
    String attLname,                    // attribute local name (in appinfo namespace)
    String attValue,                    // attribute value
    Pair<String,String> componentEQN,   // namespace and lname of global decl/defn with the appinfo
    Pair<String,String> elementEQN      // namespace and lname of element within complex type defn (or null)
    ) { }
