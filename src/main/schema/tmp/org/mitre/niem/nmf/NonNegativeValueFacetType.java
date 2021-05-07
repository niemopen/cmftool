//
// This file was generated by the Eclipse Implementation of JAXB, v3.0.0 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.05.07 at 03:04:07 PM EDT 
//


package org.mitre.niem.nmf;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * A data type for a restriction of the length of a data type.
 * 
 * <p>Java class for NonNegativeValueFacetType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="NonNegativeValueFacetType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://release.niem.gov/niem/structures/5.0/}ObjectType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}NonNegativeValue"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}DefinitionText"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}NonNegativeValueFacetAugmentationPoint" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;anyAttribute processContents='lax' namespace='urn:us:gov:ic:ntk urn:us:gov:ic:ism'/&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NonNegativeValueFacetType", propOrder = {
    "nonNegativeValue",
    "definitionText",
    "nonNegativeValueFacetAugmentationPoint"
})
public class NonNegativeValueFacetType
    extends ObjectType
{

    @XmlElement(name = "NonNegativeValue", required = true, nillable = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger nonNegativeValue;
    @XmlElement(name = "DefinitionText", required = true, nillable = true)
    protected String definitionText;
    @XmlElement(name = "NonNegativeValueFacetAugmentationPoint")
    protected List<Object> nonNegativeValueFacetAugmentationPoint;

    /**
     * Gets the value of the nonNegativeValue property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNonNegativeValue() {
        return nonNegativeValue;
    }

    /**
     * Sets the value of the nonNegativeValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setNonNegativeValue(BigInteger value) {
        this.nonNegativeValue = value;
    }

    /**
     * Gets the value of the definitionText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefinitionText() {
        return definitionText;
    }

    /**
     * Sets the value of the definitionText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefinitionText(String value) {
        this.definitionText = value;
    }

    /**
     * Gets the value of the nonNegativeValueFacetAugmentationPoint property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the nonNegativeValueFacetAugmentationPoint property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNonNegativeValueFacetAugmentationPoint().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getNonNegativeValueFacetAugmentationPoint() {
        if (nonNegativeValueFacetAugmentationPoint == null) {
            nonNegativeValueFacetAugmentationPoint = new ArrayList<Object>();
        }
        return this.nonNegativeValueFacetAugmentationPoint;
    }

}