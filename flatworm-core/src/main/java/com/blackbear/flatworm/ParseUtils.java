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

import com.blackbear.flatworm.config.SegmentElement;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;
import com.blackbear.flatworm.errors.FlatwormParserException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

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
     * Determine how best to add the {@code toAdd} instance to the collection found in {@code target} by seeing if either the {@code
     * Segment.addMethod} has a value or if {@code Segement.collectionPropertyName} has a value. If neither values exist then no action is
     * taken.
     *
     * @param segment The {@link SegmentElement} instance containing the configuration information.
     * @param target  The instance with the collection to which the {@code toAdd} instance is to be added.
     * @param toAdd   The instance to be added to the specified collection.
     * @throws FlatwormParserException should the attempt to add the {@code toAdd} instance to the specified collection fail for any
     *                                 reason.
     */
    public static void addValueToCollection(SegmentElement segment, Object target, Object toAdd) throws FlatwormParserException {
        if (!StringUtils.isBlank(segment.getCollectionPropertyName())) {
            addValueToCollection(target, segment.getCollectionPropertyName(), toAdd);
        } else if (!StringUtils.isBlank(segment.getAddMethod())) {
            invokeAddMethod(target, segment.getAddMethod(), toAdd);
        }
    }

    /**
     * Invoke an add method on the {@code target} instance by the {@code methodName} and pass it the {@code toAdd} instance to add.
     *
     * @param target     The instance on which the {@code methodName} (addMethod) will be invoked.
     * @param methodName The name of the "add" method to be invoked - it should take an Object parameter.
     * @param toAdd      The instance to pass to the {@code methodName} method.
     * @throws FlatwormParserException should invoking the method fail for any reason.
     */
    public static void invokeAddMethod(Object target, String methodName, Object toAdd) throws FlatwormParserException {
        try {
            Method method = target.getClass().getMethod(methodName, toAdd.getClass());
            method.invoke(target, toAdd);
        } catch (Exception e) {
            throw new FlatwormParserException(String.format(
                    "Unable to invoke add method %s on bean %s with object of type %s", methodName,
                    target.getClass().getSimpleName(), toAdd.getClass().getSimpleName()), e);
        }
    }

    /**
     * Attempt to find the collection property on {@code target} indicated by the {@code collectionPropertyName} and then see if the
     * "collection" returned has an {@code add(Object)} method and if it does - invoke it with the {@code toAdd} instance. If any of the
     * parameters are {@code null} then no action is taken.
     *
     * @param target                 The object that has the collection property for which the {@code toAdd} instance will be added.
     * @param collectionPropertyName The name of the Java Bean property that will return a collection (may not be a {@link
     *                               java.util.Collection} so long as there is an {@code add(Object)} method). Note that if this returns a
     *                               null value a {@link FlatwormConfigurationException} will be thrown.
     * @param toAdd                  The instance to add to the collection indicated.
     * @throws FlatwormParserException Should the underlying collection referenced by {@code collectionPropertyName} be {@code null} or
     *                                 non-existent, should no {@code add(Object)} method exist on the collection and should any error occur
     *                                 while invoking the {@code add(Object)} method if it is found (reflection style errors).
     */
    public static void addValueToCollection(Object target, String collectionPropertyName, Object toAdd) throws FlatwormParserException {
        if (target == null || StringUtils.isBlank(collectionPropertyName) || toAdd == null) return;
        try {
            PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(target, collectionPropertyName);
            if (propertyDescriptor != null) {
                Object collectionInstance = PropertyUtils.getProperty(target, collectionPropertyName);
                if (collectionInstance != null) {
                    // Once compiled, generics lose their type reference and it defaults to a simple java.lang.Object.class
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
                    "Unable to invoke the add method on the collection for property %s in bean %s with object of type %s",
                    collectionPropertyName, target.getClass().getName(), toAdd.getClass().getName()),
                    e);
        }
    }
}
