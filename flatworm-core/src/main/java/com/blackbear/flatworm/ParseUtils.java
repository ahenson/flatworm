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

import com.blackbear.flatworm.errors.FlatwormCreatorException;

import java.lang.reflect.Method;

public class ParseUtils {
    public static Object newBeanInstance(Object beanType) throws FlatwormCreatorException {
        try {
            return beanType.getClass().newInstance();
        } catch (Exception e) {
            throw new FlatwormCreatorException("Unable to create new instance of bean '"
                    + beanType.getClass() + "'", e);
        }
    }

    public static void invokeAddMethod(Object target, String methodName, Object toAdd)
            throws FlatwormCreatorException {
        try {
            Method method = target.getClass().getMethod(methodName, toAdd.getClass());
            method.invoke(target, toAdd);
        } catch (Exception e) {
            throw new FlatwormCreatorException(String.format(
                    "Unable to invoke add method %s on bean %s with object of type %s", methodName, target
                            .getClass().getSimpleName(), toAdd.getClass().getSimpleName()), e);
        }
    }
}
