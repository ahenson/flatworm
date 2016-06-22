/*
 * Flatworm - A Java Flat File Importer/Exporter Copyright (C) 2004 James M. Turner.
 * Extended by James Lawrence 2005
 * Extended by Josh Brackett in 2011 and 2012
 * Extended by Alan Henson in 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.blackbear.flatworm;

import com.blackbear.flatworm.config.CardinalityBO;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;
import com.blackbear.flatworm.errors.FlatwormParserException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;

public class ParseUtils {
    /**
     * Create a new instance of the class represented by the {@code beanType} instance.
     *
     * @param beanType Represents an instance of the class that is to be newly instantiated.
     * @return a new instance of the class that the {@code beanType} object represents.
     * @throws FlatwormParserException should creating the new instance fail for any reason.
     */
    public static Object newBeanInstance(Object beanType) throws FlatwormParserException {
        try {
            return beanType.getClass().newInstance();
        } catch (Exception e) {
            throw new FlatwormParserException("Unable to create new instance of bean '" + beanType.getClass() + "'", e);
        }
    }

    /**
     * Determine how best to add an object to another object based upon the {@link CardinalityBO} configuration.
     *
     * @param target      The object to be updated.
     * @param toAdd       The object to add to the {@code target} object.
     * @param cardinality The {@link CardinalityBO} instance that defines how the updates are to occur.
     * @throws FlatwormParserException should updating the object fail for any reason or if the cardinality rules are violated.
     */
    public static void addObjectToProperty(Object target, Object toAdd, CardinalityBO cardinality)
            throws FlatwormParserException {
        if (cardinality.getCardinalityMode() == CardinalityMode.SINGLE) {
            setProperty(target, cardinality.getPropertyName(), toAdd);
        } else {
            addValueToCollection(cardinality, target, toAdd);
        }
    }

    /**
     * Invoke a setter for a the given {@code propertyName}.
     *
     * @param target       The {@code Object} that contains the setter property to be invoked.
     * @param propertyName The name of the property.
     * @param toAdd        The value to pass to the property.
     * @throws FlatwormParserException should invoking the setter method fail for any reason.
     */
    public static void setProperty(Object target, String propertyName, Object toAdd) throws FlatwormParserException {
        try {
            PropertyUtils.setProperty(target, propertyName, toAdd);
        } catch (Exception e) {
            throw new FlatwormParserException(e.getMessage(), e);
        }
    }

    /**
     * Determine how best to add the {@code toAdd} instance to the collection found in {@code target} by seeing if either the {@code
     * Segment.addMethod} has a value or if {@code Segment.propertyName} has a value. If neither values exist then no action is taken.
     *
     * @param cardinality The {@link CardinalityBO} instance containing the configuration information.
     * @param target      The instance with the collection to which the {@code toAdd} instance is to be added.
     * @param toAdd       The instance to be added to the specified collection.
     * @throws FlatwormParserException should the attempt to add the {@code toAdd} instance to the specified collection fail for any
     *                                 reason.
     */
    public static void addValueToCollection(CardinalityBO cardinality, Object target, Object toAdd) throws FlatwormParserException {
        if (cardinality.getCardinalityMode() != CardinalityMode.SINGLE) {

            boolean addToCollection = true;
            if (cardinality.getCardinalityMode() == CardinalityMode.STRICT
                    || cardinality.getCardinalityMode() == CardinalityMode.RESTRICTED) {

                try {
                    PropertyDescriptor propDesc = PropertyUtils.getPropertyDescriptor(target, cardinality.getPropertyName());
                    Object currentValue = PropertyUtils.getProperty(target, cardinality.getPropertyName());
                    int currentSize;
                    
                    if(Collection.class.isAssignableFrom(propDesc.getPropertyType())) {
                        currentSize = Collection.class.cast(currentValue).size();
                    }
                    else if(propDesc.getPropertyType().isArray()) {
                        currentSize = Array.getLength(currentValue);
                    }
                    else {
                        throw new FlatwormParserException(String.format("Bean %s has a Cardinality Mode of %s for property %s, " +
                                "suggesting that it is an Array or some instance of java.util.Collection. However, the property type " +
                                "is %s, which is not currently supported.",
                                target.getClass().getName(), cardinality.getCardinalityMode().name(), 
                                cardinality.getPropertyName(), propDesc.getPropertyType().getName()));
                    }
                    
                    addToCollection = currentSize < cardinality.getMaxCount() || cardinality.getMaxCount() < 0;
                    
                } catch (Exception e) {
                    throw new FlatwormParserException(String.format("Failed to load property %s on bean %s when determining if a " +
                            "value could be added to the collection.", cardinality.getPropertyName(), target.getClass().getName()), e);
                }

                if (!addToCollection && cardinality.getCardinalityMode() == CardinalityMode.STRICT) {
                    throw new FlatwormParserException(String.format("Cardinality limit of %d exceeded for property %s of bean %s " +
                                    "with Cardinality Mode set to %s.",
                            cardinality.getMaxCount(), cardinality.getPropertyName(), target.getClass().getName(),
                            cardinality.getCardinalityMode().name()));
                }
            }

            // Add it if we have determined that's allowed.
            if (addToCollection) {
                if (!StringUtils.isBlank(cardinality.getAddMethod())) {
                    invokeAddMethod(target, cardinality.getAddMethod(), toAdd);
                } else if (!StringUtils.isBlank(cardinality.getPropertyName())) {
                    addValueToCollection(target, cardinality.getPropertyName(), toAdd);
                }
            }
        } else {
            throw new FlatwormParserException(String.format("Object %s attempted to be added to Object %s as part of a collection," +
                            " but the configuration has it configured as a %s Cardinality Mode.",
                    toAdd.getClass().getName(), target.getClass().getName(), cardinality.getCardinalityMode().name()));
        }
    }

    /**
     * Invoke an add method on the {@code target} instance by the {@code functionName} and pass it the {@code toAdd} instance to add.
     *
     * @param target     The instance on which the {@code functionName} (addMethod) will be invoked.
     * @param methodName The name of the "add" method to be invoked - it should take an Object parameter.
     * @param toAdd      The instance to pass to the {@code functionName} method.
     * @throws FlatwormParserException should invoking the method fail for any reason.
     */
    public static void invokeAddMethod(Object target, String methodName, Object toAdd) throws FlatwormParserException {
        try {
            Method method = target.getClass().getMethod(methodName, toAdd.getClass());
            method.invoke(target, toAdd);
        } catch (Exception e) {
            throw new FlatwormParserException(String.format(
                    "Unable to invoke add method %s on bean %s with object of converterName %s", methodName,
                    target.getClass().getSimpleName(), toAdd.getClass().getSimpleName()), e);
        }
    }

    /**
     * Attempt to find the collection property on {@code target} indicated by the {@code propertyName} and then see if the "collection"
     * returned has an {@code add(Object)} method and if it does - invoke it with the {@code toAdd} instance. If any of the parameters are
     * {@code null} then no action is taken.
     *
     * @param target                 The object that has the collection property for which the {@code toAdd} instance will be added.
     * @param collectionPropertyName The name of the Java BeanBO property that will return a collection (may not be a {@link
     *                               java.util.Collection} so long as there is an {@code add(Object)} method). Note that if this returns a
     *                               null value a {@link FlatwormConfigurationException} will be thrown.
     * @param toAdd                  The instance to add to the collection indicated.
     * @throws FlatwormParserException Should the underlying collection referenced by {@code propertyName} be {@code null} or non-existent,
     *                                 should no {@code add(Object)} method exist on the collection and should any error occur while
     *                                 invoking the {@code add(Object)} method if it is found (reflection style errors).
     */
    public static void addValueToCollection(Object target, String collectionPropertyName, Object toAdd) throws FlatwormParserException {
        if (target == null || StringUtils.isBlank(collectionPropertyName) || toAdd == null) return;
        try {
            PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(target, collectionPropertyName);
            if (propertyDescriptor != null) {
                Object collectionInstance = PropertyUtils.getProperty(target, collectionPropertyName);
                if (collectionInstance != null) {
                    // Once compiled, generics lose their converterName reference and it defaults to a simple java.lang.Object.class
                    // so that's the method parameter we'll search by.
                    Method addMethod = propertyDescriptor.getPropertyType().getMethod("add", Object.class);
                    if (addMethod != null) {
                        addMethod.invoke(collectionInstance, toAdd);
                    } else {
                        throw new FlatwormParserException(String.format(
                                "The collection instance %s for property %s in class %s does not have an add method.",
                                collectionInstance.getClass().getName(), propertyDescriptor.getName(), target.getClass().getName()));
                    }
                } else {
                    throw new FlatwormParserException(String.format(
                            "Unable to invoke the add method on collection %s as it is currently null for instance %s.",
                            propertyDescriptor.getName(), target.getClass().getName()));
                }
            } else {
                throw new FlatwormParserException(String.format(
                        "%s does not have a getter for property %s - the %s instance could therefore not be added to the collection.",
                        target.getClass().getName(), collectionPropertyName, toAdd.getClass().getName()));
            }
        } catch (Exception e) {
            throw new FlatwormParserException(String.format(
                    "Unable to invoke the add method on the collection for property %s in bean %s with object of converterName %s",
                    collectionPropertyName, target.getClass().getName(), toAdd.getClass().getName()),
                    e);
        }
    }

    /**
     * Attempt to determine the {@link CardinalityMode} based upon the {@code fieldType}. {@code Collection} based classes
     * and {@code Arrays} will return {@code CardinalityMode.LOOSE} - everything else will return {@code CardinalityMode.SINGLE}.
     * @param fieldType The field type to evaluate.
     * @return {@code CardinalityMode.LOOSE} when the {@code fieldType} is a implementation of a {@link Collection} interface or if
     * {@code fieldType} is an {@code Array}. {@code CardinalityMode.SINGLE} will be returned for all other cases.
     */
    public static CardinalityMode resolveCardinality(Class<?> fieldType) {
        CardinalityMode mode = CardinalityMode.SINGLE;
        
        if(fieldType != null && 
                (Collection.class.isAssignableFrom(fieldType) || fieldType.isArray())) {
            mode = CardinalityMode.LOOSE;
        }
        
        return mode;
    }
}
