/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.utils;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.validators.NotEmptyValidator;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import org.esa.snap.core.util.StringUtils;
import org.jdesktop.swingx.prompt.PromptSupport;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by kraftek on 12/5/2016.
 */
public class UIUtils {

    private static final String FILE_FIELD_PROMPT = "browse for %s";
    private static final String TEXT_FIELD_PROMPT = "enter %s here";
    private static final String CAMEL_CASE_SPLIT = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";

    private static class UndoAction extends AbstractAction {
        private UndoManager undoManager;
        UndoAction(UndoManager manager) {
            this.undoManager = manager;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (undoManager != null && undoManager.canUndo()) {
                    undoManager.undo();
                }
            } catch (CannotUndoException ignored) {}
        }
    };

    private static class RedoAction extends AbstractAction {
        private UndoManager undoManager;
        RedoAction(UndoManager manager) {
            this.undoManager = manager;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (undoManager != null && undoManager.canRedo()) {
                    undoManager.redo();
                }
            } catch (CannotUndoException ignored) {}
        }
    };

    public static JTextField addTextField(JPanel parent, TextFieldEditor textEditor, String labelText,
                                PropertyContainer propertyContainer, BindingContext bindingContext,
                                String propertyName, boolean isRequired) {
        parent.add(new JLabel(labelText));
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
        if (isRequired) {
            propertyDescriptor.setValidator(new NotEmptyValidator());
        }
        JComponent editorComponent = textEditor.createEditorComponent(propertyDescriptor, bindingContext);
        UIUtils.addPromptSupport(editorComponent, "enter " + labelText.toLowerCase().replace(":", "") + " here");
        UIUtils.enableUndoRedo(editorComponent);
        parent.add(editorComponent);
        return (JTextField) editorComponent;
    }

    public static List<JRadioButton> addChoiceField(JPanel parent, String label, Map<String, String> valuesAndLabels,
                                                    PropertyContainer propertyContainer, String propertyName, Class enumClass) {
        parent.add(new JLabel(label));
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
        ButtonGroup rbGroup = new ButtonGroup();
        java.util.List<JRadioButton> components = new ArrayList<>(valuesAndLabels.size());
        for (Map.Entry<String, String> choice : valuesAndLabels.entrySet()) {
            JRadioButton button = new JRadioButton(choice.getValue());
            button.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    propertyDescriptor.setDefaultValue(Enum.valueOf(enumClass, choice.getKey()));
                }
            });
            rbGroup.add(button);
            parent.add(button);
            components.add(button);
        }
        return components;
    }

    public static JComboBox addComboField(JPanel parent, String labelText, PropertyContainer propertyContainer, BindingContext bindingContext,
                                 String propertyName, GridBagConstraints constraints) {
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(propertyName);
        PropertyEditor editor = PropertyEditorRegistry.getInstance().findPropertyEditor(propertyDescriptor);
        JComponent editorComponent = editor.createEditorComponent(propertyDescriptor, bindingContext);
        if (editorComponent instanceof JComboBox) {
            JComboBox comboBox = (JComboBox)editorComponent;
            comboBox.setEditable(false);
            comboBox.setEnabled(true);
            parent.add(new JLabel(labelText));
            parent.add(editorComponent);
            return comboBox;
        }
        return null;
    }

    public static void addPromptSupport(JComponent component, String text) {
        if (JTextComponent.class.isAssignableFrom(component.getClass())) {
            JTextComponent castedComponent = (JTextComponent) component;
            PromptSupport.setPrompt(text, castedComponent);
            PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.HIDE_PROMPT, castedComponent);
        }
    }

    public static void addPromptSupport(JComponent component, Property property) {
        addPromptSupport(component, property, null);
    }

    public static void addPromptSupport(JComponent component, Property property, String promptText) {
        if (JTextComponent.class.isAssignableFrom(component.getClass())) {
            JTextComponent castedComponent = (JTextComponent) component;
            String text;
            if (File.class.isAssignableFrom(property.getType())) {
                text = promptText != null ? promptText :
                        String.format(FILE_FIELD_PROMPT, separateWords(property.getName()));
            } else {
                if (promptText == null) {
                    text = property.getDescriptor().getDescription();
                    if (StringUtils.isNullOrEmpty(text)) {
                        text = String.format(TEXT_FIELD_PROMPT, separateWords(property.getName()));
                    }
                } else {
                    text = promptText;
                }
            }
            PromptSupport.setPrompt(text, castedComponent);
            PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.HIDE_PROMPT, castedComponent);
        }
    }

    public static void enableUndoRedo(JComponent component) {
        if (component != null) {
            if (JTextComponent.class.isAssignableFrom(component.getClass())) {
                UndoManager undoManager = new UndoManager();
                ((JTextComponent) component).getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
                InputMap inputMap = component.getInputMap(JComponent.WHEN_FOCUSED);
                ActionMap actionMap = component.getActionMap();
                inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Undo");
                inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "Redo");
                actionMap.put("Undo", new UndoAction(undoManager));
                actionMap.put("Redo", new RedoAction(undoManager));
            } else if (JPanel.class.equals(component.getClass())) {
                Arrays.stream(component.getComponents())
                        .filter(c -> JTextComponent.class.isAssignableFrom(c.getClass()))
                        .map(c -> (JTextComponent) c)
                        .forEach(UIUtils::enableUndoRedo);
            }
        }
    }

    private static String separateWords(String text) {
        return separateWords(text, true);
    }

    private static String separateWords(String text, boolean lowerCase) {
        String[] words = text.split(CAMEL_CASE_SPLIT);
        if (lowerCase) {
            for (int i = 0; i < words.length; i++) {
                words[i] = words[i].toLowerCase();
            }
        }
        return String.join(" ", words);
    }
}
