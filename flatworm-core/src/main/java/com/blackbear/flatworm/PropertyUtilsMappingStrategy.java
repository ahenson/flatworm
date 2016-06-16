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

import com.blackbear.flatworm.converters.ConversionOption;
import com.blackbear.flatworm.errors.FlatwormParserException;

import org.apache.commons.beanutils.PropertyUtils;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * The default BeanMappingStrategy. Uses Jakarta Commons PropertyUtils to map
 * the raw input data into the specified beans.
 *
 * @author Dave Derry
 *
 */
@Slf4j
public class PropertyUtilsMappingStrategy implements BeanMappingStrategy {
    @Override
    public void mapBean(Object bean, String beanName, String property, Object value,
                        Map<String, ConversionOption> conv) throws FlatwormParserException {
        try {
            ConversionOption option = conv.get("append");
            if (option != null && "true".equalsIgnoreCase(option.getValue())) {
                Object currentValue = PropertyUtils.getProperty(bean, property);
                if (currentValue != null)
                    value = currentValue.toString() + value;
            }
            PropertyUtils.setProperty(bean, property, value);
        } catch (Exception e) {
            log.error("While running set property method for " + beanName + "." + property
                    + "with value '" + value + "'", e);
            throw new FlatwormParserException("Setting field " + beanName + "." + property);
        }
    }
}
