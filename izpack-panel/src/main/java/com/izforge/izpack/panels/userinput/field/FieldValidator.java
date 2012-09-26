/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.panels.userinput.field;

import java.util.Map;

import com.izforge.izpack.api.factory.ObjectFactory;
import com.izforge.izpack.panels.userinput.validator.Validator;


/**
 * Describes a validator for a field.
 *
 * @author Tim Anderson
 */
public class FieldValidator
{

    /**
     * The validator class name.
     */
    private final String className;

    /**
     * The validation message. May be {@code null}
     */
    private final String message;

    /**
     * Validator parameters.
     */
    private Map<String, String> parameters;

    /**
     * The factory to create the validator.
     */
    private final ObjectFactory factory;


    /**
     * Constructs a {@code FieldValidator}.
     *
     * @param reader  the configuration reader
     * @param factory the factory to create the validator
     */
    public FieldValidator(FieldValidatorReader reader, ObjectFactory factory)
    {
        this(reader.getClassName(), reader.getParameters(), reader.getMessage(), factory);
    }

    /**
     * Constructs a {@code FieldValidator}.
     *
     * @param className the validator class name
     * @param parameters the validation parameters. May be {@code null}
     * @param message the validation error message. May be {@code null}
     * @param factory the factory for creating the validator
     */
    public FieldValidator(String className, Map<String, String> parameters, String message, ObjectFactory factory)
    {
        this.className = className;
        this.parameters = parameters;
        this.message = message;
        this.factory = factory;
    }

    /**
     * The validation parameters.
     *
     * @return the validation parameters. May be {@code null}
     */
    public Map<String, String> getParameters()
    {
        return parameters;
    }

    /**
     * Returns the validation error message.
     *
     * @return the validation error message. May be {@code null}
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Creates the validator.
     *
     * @return a new validator
     */
    public Validator create()
    {
        return factory.create(className, Validator.class);
    }
}
