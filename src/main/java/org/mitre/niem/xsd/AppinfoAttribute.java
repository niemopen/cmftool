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
 * or on xs:element references found within a complex type definition.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

// From a document like this:
// 
// <xs:schema
//   xmlns:appinfo="https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/"
//   xmlns:foo="http://example.com/foo/"
//   targetNamespace="http://example.com/bar/">
//   <xs:complexType name="MyComplexType" appinfo:deprecated="true">
//     <xs:complexContent>
//       <xs:extension base="foo:basetype">
//         <xs:sequence>
//           <xs:element ref="foo:PropertyText" maxOccurs="3" appinfo:orderedPropertyIndicator="true"/>
//         <xs:sequence>
//         <xs:attribute ref="foo:someAttribute" appinfo:augmentingNamespace="http://example.com/N6AugEx/1.0/"/>
// 
// you would see three Appinfo records:
// 
// AppinfoAttribute(
//     attLname = "deprecated", 
//     attValue = "true", 
//     componentEQN = Pair<"http://example.com/bar/","MyComplexType">, 
//     elementEQN   = null)
//
// AppinfoAttribute(
//     attLname = "orderedPropertyIndicator", 
//     attValue = "true", 
//     componentEQN = Pair<"http://example.com/bar/","MyComplexType", 
//     elementEQN   = Pair<"http://example.com/foo/","PropertyText")
//
// Appinfo("augmentingNamespace", 
//     attLname = "augmentingNamespace",
//     attValue = "http://example.com/N6AugEx/1.0/",
//     componentEQN = Pair<"http://example.com/bar/","MyComplexType">,
//     elementEQN   = Pair<"http://example.com/foo/","someAttribute">) 


public record AppinfoAttribute (
    String sdocName,                    // schema document (for log messages)
    int sdocLine,                       // line in schema document
    String attLname,                    // appinfo attribute name (eg. appinfo:name="value")
    String attValue,                    // appinfo attribute value (eg. appinfo:name="value")
    Pair<String,String> componentEQN,   // namespace URI and lname of global decl/defn with this appinfo attribute
    Pair<String,String> elementEQN      // namespace URI and lname of element with appinfo within complex type defn (or null)
    ) { }
