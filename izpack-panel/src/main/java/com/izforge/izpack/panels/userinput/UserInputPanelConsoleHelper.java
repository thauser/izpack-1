/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2002 Jan Blok
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

package com.izforge.izpack.panels.userinput;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.factory.ObjectFactory;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.api.substitutor.VariableSubstitutor;
import com.izforge.izpack.core.substitutor.VariableSubstitutorImpl;
import com.izforge.izpack.installer.console.PanelConsole;
import com.izforge.izpack.installer.console.PanelConsoleHelper;
import com.izforge.izpack.panels.userinput.field.UserInputPanelSpec;
import com.izforge.izpack.panels.userinput.processor.Processor;
import com.izforge.izpack.util.Console;
import com.izforge.izpack.util.OsVersion;

/**
 * The user input panel console helper class.
 *
 * @author Mounir El Hajj
 */
public class UserInputPanelConsoleHelper extends PanelConsoleHelper implements PanelConsole
{
    private static final Logger logger = Logger.getLogger(UserInputPanelConsoleHelper.class.getName());

    private static final String FIELD_NODE_ID = "field";

    protected static final String ATTRIBUTE_CONDITIONID_NAME = "conditionid";

    private static final String VARIABLE = "variable";

    private static final String SET = "set";

    private static final String TEXT = "txt";

    private static final String SPEC = "spec";

    private static final String PWD = "pwd";

    private static final String TYPE_ATTRIBUTE = "type";

    private static final String TEXT_FIELD = "text";

    private static final String COMBO_FIELD = "combo";

    private static final String STATIC_TEXT = "staticText";

    private static final String CHOICE = "choice";

    private static final String DIR = "dir";

    private static final String FILE = "file";

    private static final String PASSWORD = "password";

    private static final String VALUE = "value";

    private static final String RADIO_FIELD = "radio";

    private static final String TITLE_FIELD = "title";

    private static final String CHECK_FIELD = "check";

    private static final String RULE_FIELD = "rule";

    private static final String SPACE = "space";

    private static final String DIVIDER = "divider";

    static final String DISPLAY_FORMAT = "displayFormat";

    static final String PLAIN_STRING = "plainString";

    static final String SPECIAL_SEPARATOR = "specialSeparator";

    static final String LAYOUT = "layout";

    static final String RESULT_FORMAT = "resultFormat";

    private static final String DESCRIPTION = "description";

    private static final String TRUE = "true";

    private static final String NAME = "name";

    private static final String FAMILY = "family";

    private static final String OS = "os";

    private static final String SELECTEDPACKS = "createForPack";


    private static Input SPACE_INTPUT_FIELD = new Input(SPACE, null, null, SPACE, "\r", 0);
    private static Input DIVIDER_INPUT_FIELD = new Input(DIVIDER, null, null, DIVIDER,
                                                         "------------------------------------------", 0);

    public List<Input> listInputs;

    /**
     * The panel meta-data.
     */
    private Panel panel;

    /**
     * The resources.
     */
    private final Resources resources;

    /**
     * The factory for creating field validators.
     */
    private final ObjectFactory factory;

    /**
     * Constructs an <tt>UserInputPanelConsoleHelper</tt>.
     *
     * @param panel     the panel meta-data
     * @param resources the resources
     * @param factory   the object factory
     */
    public UserInputPanelConsoleHelper(Panel panel, Resources resources, ObjectFactory factory)
    {
        this.panel = panel;
        this.resources = resources;
        this.factory = factory;
        listInputs = new ArrayList<Input>();
    }

    @Override
    public boolean runConsoleFromProperties(InstallData installData, Properties properties)
    {
        collectInputs(installData);
        for (Input listInput : listInputs)
        {
            String strVariableName = listInput.strVariableName;
            if (strVariableName != null)
            {
                String strVariableValue = properties.getProperty(strVariableName);
                if (strVariableValue != null)
                {
                    installData.setVariable(strVariableName, strVariableValue);
                }
            }
        }
        return true;
    }

    @Override
    public boolean runGeneratePropertiesFile(InstallData installData,
                                             PrintWriter printWriter)
    {
        collectInputs(installData);
        for (Input input : listInputs)
        {
            if (input.strVariableName != null)
            {
                printWriter.println(input.strVariableName + "=");
            }
        }
        return true;
    }

    /**
     * Runs the panel using the specified console.
     *
     * @param installData the installation data
     * @param console     the console
     * @return <tt>true</tt> if the panel ran successfully, otherwise <tt>false</tt>
     */
    @Override
    public boolean runConsole(InstallData installData, Console console)
    {
        boolean processpanel = collectInputs(installData);
        if (!processpanel)
        {
            return true;
        }
        boolean status = true;
        for (Input field : listInputs)
        {
            if (TEXT_FIELD.equals(field.strFieldType)
                    || FILE.equals(field.strFieldType)
                    || RULE_FIELD.equals(field.strFieldType)
                    || DIR.equals(field.strFieldType))
            {
                status = status && processTextField(field, installData, console);
            }
            else if (COMBO_FIELD.equals(field.strFieldType)
                    || RADIO_FIELD.equals(field.strFieldType))
            {
                status = status && processComboRadioField(field, installData, console);
            }
            else if (CHECK_FIELD.equals(field.strFieldType))
            {
                status = status && processCheckField(field, installData, console);
            }
            else if (STATIC_TEXT.equals(field.strFieldType)
                    || TITLE_FIELD.equals(field.strFieldType)
                    || DIVIDER.equals(field.strFieldType)
                    || SPACE.equals(field.strFieldType))
            {
                status = status && processSimpleField(field, installData, console);
            }
            else if (PASSWORD.equals(field.strFieldType))
            {
                status = status && processPasswordField((Password) field, installData, console);
            }
        }

        return promptEndPanel(installData, console);
    }

    private boolean collectInputs(InstallData installData)
    {

        listInputs.clear();
        IXMLElement spec = null;

        UserInputPanelSpec model = new UserInputPanelSpec(resources, installData, factory);
        IXMLElement data = model.getPanelSpec(panel);

        List<IXMLElement> forPacks = data.getChildrenNamed(SELECTEDPACKS);
        List<IXMLElement> forOs = data.getChildrenNamed(OS);

        if (itemRequiredFor(forPacks, installData) && itemRequiredForOs(forOs))
        {
            spec = data;
        }

        if (spec == null)
        {
            return false;
        }
        List<IXMLElement> fields = spec.getChildrenNamed(FIELD_NODE_ID);
        for (IXMLElement field : fields)
        {
            forPacks = field.getChildrenNamed(SELECTEDPACKS);
            forOs = field.getChildrenNamed(OS);

            if (itemRequiredFor(forPacks, installData) && itemRequiredForOs(forOs))
            {

                String conditionid = field.getAttribute(ATTRIBUTE_CONDITIONID_NAME);
                if (conditionid != null)
                {
                    // check if condition is fulfilled
                    if (!installData.getRules().isConditionTrue(conditionid, installData))
                    {
                        continue;
                    }
                }
                Input in = getInputFromField(field, installData);
                if (in != null)
                {
                    listInputs.add(in);
                }
            }
        }
        return true;
    }

    private boolean processSimpleField(Input input, InstallData installData, Console console)
    {
        console.println(installData.getVariables().replace(input.strText));
        return true;
    }

    private boolean processPasswordField(Password password, InstallData installData, Console console)
    {
        boolean result = false;
        for (int i = 0; i < password.input.length; i++)
        {
            result = processTextField(password.input[i], installData, console);
            if (!result)
            {
                break;
            }
        }

        return result;

    }

    boolean processTextField(Input input, InstallData installData, Console console)
    {
        String variable = input.strVariableName;
        String set;
        String fieldText;
        if ((variable == null) || (variable.length() == 0))
        {
            return false;
        }

        if (input.listChoices.size() == 0)
        {
            logger.warning("No 'spec' element defined in file field");
            return false;
        }

        set = installData.getVariable(variable);
        if (set == null)
        {
            set = input.strDefaultValue;
            if (set == null)
            {
                set = "";
            }
        }

        if (!"".equals(set))
        {
            set = installData.getVariables().replace(set);
        }

        fieldText = input.listChoices.get(0).strText;
        String value = console.prompt(fieldText + " [" + set + "] ", null);
        if (value == null)
        {
            return false;
        }
        value = value.trim();
        if ("".equals(value))
        {
            value = set;
        }
        installData.setVariable(variable, value);
        return true;
    }

    private boolean processComboRadioField(Input input, InstallData installData, Console console)
    {
        // TODO protection if selection not valid and no set value
        String variable = input.strVariableName;
        if ((variable == null) || (variable.length() == 0))
        {
            return false;
        }
        String currentvariablevalue = installData.getVariable(variable);
        //If we dont do this, choice with index=0 will always be displayed, no matter what is selected
        input.iSelectedChoice = -1;
        boolean userinput = false;

        // display the description for this combo or radio field
        if (input.strText != null)
        {
            console.println(input.strText);
        }

        List<Choice> lisChoices = input.listChoices;
        if (lisChoices.size() == 0)
        {
            logger.warning("No 'spec' element defined in file field");
            return false;
        }
        if (currentvariablevalue != null)
        {
            userinput = true;
        }
        for (int i = 0; i < lisChoices.size(); i++)
        {
            Choice choice = lisChoices.get(i);
            String value = choice.strValue;
            // if the choice value is provided via a property to the process, then
            // set it as the selected choice, rather than defaulting to what the
            // spec defines.
            if (userinput)
            {
                if ((value != null) && (value.length() > 0) && (currentvariablevalue.equals(value)))
                {
                    input.iSelectedChoice = i;
                }
            }
            else
            {
                String set = choice.strSet;
                if (set != null)
                {
                    set = installData.getVariables().replace(set);
                    if (set.equals(TRUE))
                    {
                        input.iSelectedChoice = i;
                    }
                }
            }
            console.println(i + "  [" + (input.iSelectedChoice == i ? "x" : " ") + "] "
                                    + (choice.strText != null ? choice.strText : ""));
        }

        int value = console.prompt("input selection:", 0, lisChoices.size(), input.iSelectedChoice, -1);
        if (value == -1)
        {
            return false;
        }
        input.iSelectedChoice = value;
        installData.setVariable(variable, input.listChoices.get(input.iSelectedChoice).strValue);
        return true;

    }

    private boolean processCheckField(Input input, InstallData installData, Console console)
    {
        String variable = input.strVariableName;
        if ((variable == null) || (variable.length() == 0))
        {
            return false;
        }
        String currentvariablevalue = installData.getVariable(variable);
        if (currentvariablevalue == null)
        {
            currentvariablevalue = "";
        }
        List<Choice> choices = input.listChoices;
        if (choices.size() == 0)
        {
            logger.warning("No 'spec' element defined in check field");
            return false;
        }
        Choice choice;
        for (int i = 0; i < choices.size(); i++)
        {
            choice = choices.get(i);
            String value = choice.strValue;

            if ((value != null) && (value.length() > 0) && (currentvariablevalue.equals(value)))
            {
                input.iSelectedChoice = i;
            }
            else
            {
                String set = input.strDefaultValue;
                if (set != null)
                {
                    set = installData.getVariables().replace(set);
                    if (set.equals(TRUE))
                    {
                        input.iSelectedChoice = 1;
                    }
                }
            }
        }
        choice = choices.get(0);
        System.out.println("  [" + (input.iSelectedChoice == 1 ? "x" : " ") + "] "
                                   + (choice.strText != null ? choice.strText : ""));
        int value = console.prompt("input 1 to select, 0 to deselect:", 0, 1, input.iSelectedChoice, -1);
        if (value == -1)
        {
            return false;
        }
        input.iSelectedChoice = value;

        installData.setVariable(variable, input.listChoices.get(input.iSelectedChoice).strValue);
        return true;

    }

    private Input getInputFromField(IXMLElement field, InstallData installData)
    {
        String strVariableName = field.getAttribute(VARIABLE);
        String strFieldType = field.getAttribute(TYPE_ATTRIBUTE);
        if (TITLE_FIELD.equals(strFieldType))
        {
            String strText = field.getAttribute(TEXT);
            return new Input(strVariableName, null, null, TITLE_FIELD, strText, 0);
        }

        if (STATIC_TEXT.equals(strFieldType))
        {
            String strText = field.getAttribute(TEXT);
            return new Input(strVariableName, null, null, STATIC_TEXT, strText, 0);
        }

        if (TEXT_FIELD.equals(strFieldType) || FILE.equals(strFieldType) || DIR.equals(strFieldType))
        {
            List<Choice> choicesList = new ArrayList<Choice>();
            String strFieldText = null;
            String strSet = null;
            String strText = null;
            IXMLElement spec = field.getFirstChildNamed(SPEC);
            IXMLElement description = field.getFirstChildNamed(DESCRIPTION);
            if (spec != null)
            {
                strText = spec.getAttribute(TEXT);
                strSet = spec.getAttribute(SET);
            }
            if (description != null)
            {
                strFieldText = description.getAttribute(TEXT);
            }
            choicesList.add(new Choice(strText, null, strSet));
            return new Input(strVariableName, strSet, choicesList, strFieldType, strFieldText, 0);

        }

        if (RULE_FIELD.equals(strFieldType))
        {

            List<Choice> choicesList = new ArrayList<Choice>();
            String strFieldText = null;
            String strSet = null;
            String strText = null;
            IXMLElement spec = field.getFirstChildNamed(SPEC);
            IXMLElement description = field.getFirstChildNamed(DESCRIPTION);
            if (spec != null)
            {
                strText = spec.getAttribute(TEXT);
                strSet = spec.getAttribute(SET);
            }
            if (description != null)
            {
                strFieldText = description.getAttribute(TEXT);
            }
            if (strSet != null && spec.getAttribute(LAYOUT) != null)
            {
                StringTokenizer layoutTokenizer = new StringTokenizer(spec.getAttribute(LAYOUT));
                List<String> listSet = Arrays.asList(new String[layoutTokenizer.countTokens()]);
                StringTokenizer setTokenizer = new StringTokenizer(strSet);
                String token;
                while (setTokenizer.hasMoreTokens())
                {
                    token = setTokenizer.nextToken();
                    if (token.contains(":"))
                    {
                        listSet.set(new Integer(token.substring(0, token.indexOf(":"))),
                                    token.substring(token.indexOf(":") + 1));
                    }
                }

                int iCounter = 0;
                StringBuilder buffer = new StringBuilder();
                String format = spec.getAttribute(RESULT_FORMAT);
                String strSpecialSeparator = spec.getAttribute(SPECIAL_SEPARATOR);
                while (layoutTokenizer.hasMoreTokens())
                {
                    token = layoutTokenizer.nextToken();
                    if (token.matches(".*:.*:.*"))
                    {
                        buffer.append(listSet.get(iCounter) != null ? listSet.get(iCounter) : "");
                        iCounter++;
                    }
                    else
                    {
                        if (SPECIAL_SEPARATOR.equals(format))
                        {
                            buffer.append(strSpecialSeparator);
                        }
                        else if (PLAIN_STRING.equals(format))
                        {

                        }
                        else
                        // if (DISPLAY_FORMAT.equals(strRusultFormat))
                        {
                            buffer.append(token);
                        }

                    }
                }
                strSet = buffer.toString();
            }
            choicesList.add(new Choice(strText, null, strSet));
            return new Input(strVariableName, strSet, choicesList, TEXT_FIELD, strFieldText, 0);

        }

        if (COMBO_FIELD.equals(strFieldType) || RADIO_FIELD.equals(strFieldType))
        {
            List<Choice> choicesList = new ArrayList<Choice>();
            String strFieldText = null;
            int selection = -1;
            IXMLElement spec = field.getFirstChildNamed(SPEC);
            IXMLElement description = field.getFirstChildNamed(DESCRIPTION);
            List<IXMLElement> choices = null;
            if (spec != null)
            {
                choices = spec.getChildrenNamed(CHOICE);
            }
            if (description != null)
            {
                strFieldText = description.getAttribute(TEXT);
            }
            for (int i = 0; i < choices.size(); i++)
            {

                IXMLElement choice = choices.get(i);
                String processorClass = choice.getAttribute("processor");

                if (processorClass != null && !"".equals(processorClass))
                {
                    String choiceValues = "";
                    try
                    {
                        choiceValues = ((Processor) Class.forName(processorClass).newInstance())
                                .process(null);
                    }
                    catch (Throwable t)
                    {
                        t.printStackTrace();
                    }
                    String set = choice.getAttribute(SET);
                    if (set == null)
                    {
                        set = "";
                    }
                    if (set != null && !"".equals(set))
                    {
                        VariableSubstitutor variableSubstitutor = new VariableSubstitutorImpl(
                                installData.getVariables());
                        set = variableSubstitutor.substitute(set);
                    }

                    StringTokenizer tokenizer = new StringTokenizer(choiceValues, ":");
                    int counter = 0;
                    while (tokenizer.hasMoreTokens())
                    {
                        String token = tokenizer.nextToken();
                        String choiceSet = null;
                        if (token.equals(set)
                                )
                        {
                            choiceSet = "true";
                            selection = counter;
                        }
                        choicesList.add(new Choice(
                                token,
                                token,
                                choiceSet));
                        counter++;

                    }
                }
                else
                {
                    String value = choice.getAttribute(VALUE);

                    String set = choice.getAttribute(SET);
                    if (set != null)
                    {
                        if (set != null && !"".equals(set))
                        {
                            VariableSubstitutor variableSubstitutor = new VariableSubstitutorImpl(installData
                                                                                                          .getVariables());
                            set = variableSubstitutor.substitute(set);
                        }
                        if (set.equalsIgnoreCase(TRUE))
                        {
                            selection = i;

                        }
                    }


                    choicesList.add(new Choice(
                            choice.getAttribute(TEXT),
                            value,
                            set));

                }
            }

            if (choicesList.size() == 1)
            {
                selection = 0;
            }

            return new Input(strVariableName, null, choicesList, strFieldType, strFieldText, selection);
        }

        if (CHECK_FIELD.equals(strFieldType))
        {
            List<Choice> choicesList = new ArrayList<Choice>();
            String strFieldText = null;
            String strSet = null;
            String strText;
            int iSelectedChoice = 0;
            IXMLElement spec = field.getFirstChildNamed(SPEC);
            IXMLElement description = field.getFirstChildNamed(DESCRIPTION);
            if (spec != null)
            {
                strText = spec.getAttribute(TEXT);
                strSet = spec.getAttribute(SET);
                choicesList.add(new Choice(strText, spec.getAttribute("false"), null));
                choicesList.add(new Choice(strText, spec.getAttribute("true"), null));
                if (strSet != null)
                {
                    if (strSet.equalsIgnoreCase(TRUE))
                    {
                        iSelectedChoice = 1;
                    }
                }
            }
            else
            {
                System.out.println("No spec specified for input of type check");
            }

            if (description != null)
            {
                strFieldText = description.getAttribute(TEXT);
            }
            return new Input(strVariableName, strSet, choicesList, CHECK_FIELD, strFieldText,
                             iSelectedChoice);
        }


        if (SPACE.equals(strFieldType))
        {
            return SPACE_INTPUT_FIELD;

        }

        if (DIVIDER.equals(strFieldType))
        {
            return DIVIDER_INPUT_FIELD;
        }


        if (PASSWORD.equals(strFieldType))
        {
            List<Choice> choicesList = new ArrayList<Choice>();
            String strFieldText = null;
            String strSet;
            String strText;

            IXMLElement spec = field.getFirstChildNamed(SPEC);
            if (spec != null)
            {

                List<IXMLElement> pwds = spec.getChildrenNamed(PWD);
                if (pwds == null || pwds.size() == 0)
                {
                    System.out.println("No pwd specified in the spec for type password");
                    return null;
                }

                Input[] inputs = new Input[pwds.size()];
                for (int i = 0; i < pwds.size(); i++)
                {

                    IXMLElement pwde = pwds.get(i);
                    strText = pwde.getAttribute(TEXT);
                    strSet = pwde.getAttribute(SET);
                    choicesList.add(new Choice(strText, null, strSet));
                    inputs[i] = new Input(strVariableName, strSet, choicesList, strFieldType, strFieldText, 0);

                }
                return new Password(strFieldType, inputs);

            }

            System.out.println("No spec specified for input of type password");
            return null;
        }


        System.out.println(strFieldType + " field collection not implemented");

        return null;
    }

    /*--------------------------------------------------------------------------*/

    /**
     * Verifies if an item is required for any of the packs listed. An item is required for a pack
     * in the list if that pack is actually selected for installation. <br>
     * <br>
     * <b>Note:</b><br>
     * If the list of selected packs is empty then <code>true</code> is always returnd. The same is
     * true if the <code>packs</code> list is empty.
     *
     * @param packs a <code>Vector</code> of <code>String</code>s. Each of the strings denotes a
     *              pack for which an item should be created if the pack is actually installed.
     * @return <code>true</code> if the item is required for at least one pack in the list,
     *         otherwise returns <code>false</code>.
     */
    /*--------------------------------------------------------------------------*/
    /*
     * $ @design
     *
     * The information about the installed packs comes from InstallData.selectedPacks. This assumes
     * that this panel is presented to the user AFTER the PacksPanel.
     * --------------------------------------------------------------------------
     */
    private boolean itemRequiredFor(List<IXMLElement> packs, InstallData idata)
    {

        String selected;
        String required;

        if (packs.size() == 0)
        {
            return (true);
        }

        // ----------------------------------------------------
        // We are getting to this point if any packs have been
        // specified. This means that there is a possibility
        // that some UI elements will not get added. This
        // means that we can not allow to go back to the
        // PacksPanel, because the process of building the
        // UI is not reversable.
        // ----------------------------------------------------
        // packsDefined = true;

        // ----------------------------------------------------
        // analyze if the any of the packs for which the item
        // is required have been selected for installation.
        // ----------------------------------------------------
        for (int i = 0; i < idata.getSelectedPacks().size(); i++)
        {
            selected = idata.getSelectedPacks().get(i).getName();

            for (IXMLElement pack : packs)
            {
                required = pack.getAttribute(NAME, "");
                if (selected.equals(required))
                {
                    return (true);
                }
            }
        }

        return (false);
    }

    /**
     * Verifies if an item is required for the operating system the installer executed. The
     * configuration for this feature is: <br/>
     * &lt;os family="unix"/&gt; <br>
     * <br>
     * <b>Note:</b><br>
     * If the list of the os is empty then <code>true</code> is always returnd.
     *
     * @param os The <code>Vector</code> of <code>String</code>s. containing the os names
     * @return <code>true</code> if the item is required for the os, otherwise returns
     *         <code>false</code>.
     */
    public boolean itemRequiredForOs(List<IXMLElement> os)
    {
        if (os.size() == 0)
        {
            return true;
        }

        for (IXMLElement osElement : os)
        {
            String family = osElement.getAttribute(FAMILY);
            boolean match = false;

            if ("windows".equals(family))
            {
                match = OsVersion.IS_WINDOWS;
            }
            else if ("mac".equals(family))
            {
                match = OsVersion.IS_OSX;
            }
            else if ("unix".equals(family))
            {
                match = OsVersion.IS_UNIX;
            }
            if (match)
            {
                return true;
            }
        }
        return false;
    }


    public static class Input
    {

        public Input(String strFieldType)
        {
            this.strFieldType = strFieldType;
        }

        public Input(String strVariableName, String strDefaultValue, List<Choice> listChoices,
                     String strFieldType, String strFieldText, int iSelectedChoice)
        {
            this.strVariableName = strVariableName;
            this.strDefaultValue = strDefaultValue;
            this.listChoices = listChoices;
            this.strFieldType = strFieldType;
            this.strText = strFieldText;
            this.iSelectedChoice = iSelectedChoice;
        }

        String strVariableName;

        String strDefaultValue;

        List<Choice> listChoices;

        String strFieldType;

        String strText;

        int iSelectedChoice = -1;
    }

    public static class Choice
    {

        public Choice(String strText, String strValue, String strSet)
        {
            this.strText = strText;
            this.strValue = strValue;
            this.strSet = strSet;
        }

        String strText;

        String strValue;

        String strSet;
    }

    public static class Password extends Input
    {

        public Password(String strFieldType, Input[] input)
        {
            super(strFieldType);
            this.input = input;
        }

        Input[] input;


    }

}
