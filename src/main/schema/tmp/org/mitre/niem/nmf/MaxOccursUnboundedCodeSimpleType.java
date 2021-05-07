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
 * <p>Java class for MaxOccursUnboundedCodeSimpleType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;simpleType name="MaxOccursUnboundedCodeSimpleType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="unbounded"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "MaxOccursUnboundedCodeSimpleType")
@XmlEnum
public enum MaxOccursUnboundedCodeSimpleType {


    /**
     * There is no maximum number of times that a property may occur.
     * 
     */
    @XmlEnumValue("unbounded")
    UNBOUNDED("unbounded");
    private final String value;

    MaxOccursUnboundedCodeSimpleType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MaxOccursUnboundedCodeSimpleType fromValue(String v) {
        for (MaxOccursUnboundedCodeSimpleType c: MaxOccursUnboundedCodeSimpleType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}