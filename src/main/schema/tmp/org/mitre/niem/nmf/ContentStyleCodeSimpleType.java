//
// This file was generated by the Eclipse Implementation of JAXB, v3.0.0 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.05.07 at 03:04:07 PM EDT 
//


package org.mitre.niem.nmf;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ContentStyleCodeSimpleType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;simpleType name="ContentStyleCodeSimpleType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="HasObjectProperty"/&gt;
 *     &lt;enumeration value="HasValue"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ContentStyleCodeSimpleType")
@XmlEnum
public enum ContentStyleCodeSimpleType {


    /**
     * A class contains object properties.
     * 
     */
    @XmlEnumValue("HasObjectProperty")
    HAS_OBJECT_PROPERTY("HasObjectProperty"),

    /**
     * A class contains a datatype value.
     * 
     */
    @XmlEnumValue("HasValue")
    HAS_VALUE("HasValue");
    private final String value;

    ContentStyleCodeSimpleType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ContentStyleCodeSimpleType fromValue(String v) {
        for (ContentStyleCodeSimpleType c: ContentStyleCodeSimpleType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
