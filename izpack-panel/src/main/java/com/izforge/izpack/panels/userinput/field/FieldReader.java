/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2012 Tim Anderson
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

import java.util.ArrayList;
import java.util.List;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.binding.OsModel;
import com.izforge.izpack.api.exception.IzPackException;

/**
 * Field reader.
 *
 * @author Tim Anderson
 */
public class FieldReader extends ElementReader
{
    /**
     * The field element.
     */
    private final IXMLElement field;

    /**
     * The field specification. May be {@code null}.
     */
    private final IXMLElement spec;

    /**
     * Variable attribute name.
     */
    protected static final String VARIABLE = "variable";

    /**
     * Text size attribute name.
     */
    private static final String TEXT_SIZE = "size";

    /**
     * The field specification element name.
     */
    protected static final String SPEC = "spec";

    /**
     * The validator element name.
     */
    private static final String VALIDATOR = "validator";


    /**
     * Constructs a {@code FieldReader}.
     *
     * @param field  the field element to read
     * @param config the configuration
     */
    public FieldReader(IXMLElement field, Config config)
    {
        super(config);
        this.field = field;
        this.spec = getSpec(field, config);
    }

    /**
     * Returns the 'field' element.
     *
     * @return the 'field' element
     */
    public IXMLElement getField()
    {
        return field;
    }

    /**
     * Returns the 'spec' element.
     *
     * @return the 'spec' element, or {@code null} if none is present
     */
    public IXMLElement getSpec()
    {
        return spec;
    }

    /**
     * Returns the variable that the field reads and updates.
     * <p/>
     * This implementation throws an exception if the variable is not present; subclasses override and return
     * {@code null} if the variable is optional.
     *
     * @return the 'variable' attribute, or {@code null} if the variable is optional but not present
     * @throws IzPackException if the 'variable' attribute is mandatory but not present
     */
    public String getVariable()
    {
        return getConfig().getAttribute(getField(), VARIABLE);
    }

    /**
     * Returns the packs that this field applies to.
     *
     * @return the list of pack names
     */
    public List<String> getPacks()
    {
        return getPacks(field);
    }

    /**
     * Returns the operating systems that this field applies to.
     *
     * @return the operating systems, or an empty list if the field applies to all operating systems
     */
    public List<OsModel> getOsModels()
    {
        return getOsModels(field);
    }

    /**
     * Returns the default value of the field.
     * <p/>
     * This is obtained from the 'set' attribute of the 'spec' element.
     *
     * @return the default value. May be {@code null}
     */
    public String getDefaultValue()
    {
        return (spec != null) ? getConfig().getString(spec, "set", null) : null;
    }

    /**
     * Returns the field size.
     *
     * @return the field size, or {@code -1} if no size is specified, or the specified size is invalid
     */
    public int getSize()
    {
        int result = -1;
        if (spec != null)
        {
            result = getConfig().getInt(spec, TEXT_SIZE, result);
        }
        return result;
    }

    public List<FieldValidator> getValidators()
    {
        List<FieldValidator> result = new ArrayList<FieldValidator>();
        Config config = getConfig();
        for (IXMLElement element : field.getChildrenNamed(VALIDATOR))
        {
            FieldValidatorReader reader = new FieldValidatorReader(element, config);
            result.add(new FieldValidator(reader, config.getFactory()));
        }
        return result;
    }

    public FieldProcessor getProcessor()
    {
        IXMLElement element = (spec != null) ? spec.getFirstChildNamed("processor") : null;
        return element != null ? new FieldProcessor(element, getConfig()) : null;
    }

    public String getDescription()
    {
        return getText(field.getFirstChildNamed("description"));
    }

    public String getLabel()
    {
        return getText(getSpec());
    }

    public boolean getRevalidate()
    {
        return (spec != null) && getConfig().getBoolean(spec, "revalidate", false);
    }

    /**
     * Returns the condition that determines if the field is displayed or not.
     *
     * @return the condition. May be {@code null}
     */
    public String getCondition()
    {
        return getConfig().getString(getField(), "conditionid", null);
    }

    /**
     * Returns the 'spec' element.
     *
     * @param field  the parent field element
     * @param config the configuration
     * @return the 'spec' element, or {@code null} if the spec element is optional and not found
     * @throws IzPackException if the 'spec' element is mandatory but not present
     */
    protected IXMLElement getSpec(IXMLElement field, Config config)
    {
        return config.getElement(field, SPEC);
    }

    /**
     * Extracts the text from an element. The text can be defined:
     * <ol>
     * <li>in the locale's messages, under the key defined by the {@code id} attribute; or
     * <li>as value of the attribute {@code txt}.
     * </ol>
     *
     * @param element the element from which to extract the text
     * @return the text, or {@code null} if none can be found
     */
    protected String getText(IXMLElement element)
    {
        return getConfig().getText(element);
    }

}
