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
 * A data type for an extension of a class.
 * 
 * <p>Java class for ExtensionOfType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ExtensionOfType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://release.niem.gov/niem/structures/5.0/}ObjectType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}Class"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}HasValue" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}HasDataProperty" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}HasObjectProperty" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{http://reference.niem.gov/specification/metamodel/5.0alpha1}ExtensionOfAugmentationPoint" maxOccurs="unbounded" minOccurs="0"/&gt;
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
@XmlType(name = "ExtensionOfType", propOrder = {
    "clazz",
    "hasValue",
    "hasDataProperty",
    "hasObjectProperty",
    "extensionOfAugmentationPoint"
})
public class ExtensionOfType
    extends ObjectType
{

    @XmlElement(name = "Class", required = true, nillable = true)
    protected ClassType clazz;
    @XmlElement(name = "HasValue", nillable = true)
    protected List<HasValueType> hasValue;
    @XmlElement(name = "HasDataProperty", nillable = true)
    protected List<HasDataPropertyType> hasDataProperty;
    @XmlElement(name = "HasObjectProperty", nillable = true)
    protected List<HasObjectPropertyType> hasObjectProperty;
    @XmlElement(name = "ExtensionOfAugmentationPoint")
    protected List<Object> extensionOfAugmentationPoint;

    /**
     * Gets the value of the clazz property.
     * 
     * @return
     *     possible object is
     *     {@link ClassType }
     *     
     */
    public ClassType getClazz() {
        return clazz;
    }

    /**
     * Sets the value of the clazz property.
     * 
     * @param value
     *     allowed object is
     *     {@link ClassType }
     *     
     */
    public void setClazz(ClassType value) {
        this.clazz = value;
    }

    /**
     * Gets the value of the hasValue property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the hasValue property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHasValue().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link HasValueType }
     * 
     * 
     */
    public List<HasValueType> getHasValue() {
        if (hasValue == null) {
            hasValue = new ArrayList<HasValueType>();
        }
        return this.hasValue;
    }

    /**
     * Gets the value of the hasDataProperty property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the hasDataProperty property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHasDataProperty().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link HasDataPropertyType }
     * 
     * 
     */
    public List<HasDataPropertyType> getHasDataProperty() {
        if (hasDataProperty == null) {
            hasDataProperty = new ArrayList<HasDataPropertyType>();
        }
        return this.hasDataProperty;
    }

    /**
     * Gets the value of the hasObjectProperty property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the hasObjectProperty property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHasObjectProperty().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link HasObjectPropertyType }
     * 
     * 
     */
    public List<HasObjectPropertyType> getHasObjectProperty() {
        if (hasObjectProperty == null) {
            hasObjectProperty = new ArrayList<HasObjectPropertyType>();
        }
        return this.hasObjectProperty;
    }

    /**
     * Gets the value of the extensionOfAugmentationPoint property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the extensionOfAugmentationPoint property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExtensionOfAugmentationPoint().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getExtensionOfAugmentationPoint() {
        if (extensionOfAugmentationPoint == null) {
            extensionOfAugmentationPoint = new ArrayList<Object>();
        }
        return this.extensionOfAugmentationPoint;
    }

}