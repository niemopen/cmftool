//
// This file was generated by the Eclipse Implementation of JAXB, v3.0.0 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.05.07 at 03:04:07 PM EDT 
//


package org.mitre.niem.nmf;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * A data type for an occurrence of a value as content of a class.
 * 
 * <p>Java class for HasValueType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HasValueType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://release.niem.gov/niem/structures/5.0/}ObjectType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}Datatype"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}HasValueAugmentationPoint" maxOccurs="unbounded" minOccurs="0"/&gt;
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
@XmlType(name = "HasValueType", propOrder = {
    "datatype",
    "hasValueAugmentationPoint"
})
public class HasValueType
    extends ObjectType
{

    @XmlElement(name = "Datatype", required = true, nillable = true)
    protected DatatypeType datatype;
    @XmlElement(name = "HasValueAugmentationPoint")
    protected List<Object> hasValueAugmentationPoint;

    /**
     * Gets the value of the datatype property.
     * 
     * @return
     *     possible object is
     *     {@link DatatypeType }
     *     
     */
    public DatatypeType getDatatype() {
        return datatype;
    }

    /**
     * Sets the value of the datatype property.
     * 
     * @param value
     *     allowed object is
     *     {@link DatatypeType }
     *     
     */
    public void setDatatype(DatatypeType value) {
        this.datatype = value;
    }

    /**
     * Gets the value of the hasValueAugmentationPoint property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the hasValueAugmentationPoint property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHasValueAugmentationPoint().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getHasValueAugmentationPoint() {
        if (hasValueAugmentationPoint == null) {
            hasValueAugmentationPoint = new ArrayList<Object>();
        }
        return this.hasValueAugmentationPoint;
    }

}
