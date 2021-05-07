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
 * A data type for a union of data types.
 * 
 * <p>Java class for UnionOfType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UnionOfType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://release.niem.gov/niem/structures/5.0/}ObjectType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}Datatype" maxOccurs="unbounded"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}UnionOfAugmentationPoint" maxOccurs="unbounded" minOccurs="0"/&gt;
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
@XmlType(name = "UnionOfType", propOrder = {
    "datatype",
    "unionOfAugmentationPoint"
})
public class UnionOfType
    extends ObjectType
{

    @XmlElement(name = "Datatype", required = true, nillable = true)
    protected List<DatatypeType> datatype;
    @XmlElement(name = "UnionOfAugmentationPoint")
    protected List<Object> unionOfAugmentationPoint;

    /**
     * Gets the value of the datatype property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the datatype property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDatatype().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DatatypeType }
     * 
     * 
     */
    public List<DatatypeType> getDatatype() {
        if (datatype == null) {
            datatype = new ArrayList<DatatypeType>();
        }
        return this.datatype;
    }

    /**
     * Gets the value of the unionOfAugmentationPoint property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the unionOfAugmentationPoint property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUnionOfAugmentationPoint().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getUnionOfAugmentationPoint() {
        if (unionOfAugmentationPoint == null) {
            unionOfAugmentationPoint = new ArrayList<Object>();
        }
        return this.unionOfAugmentationPoint;
    }

}