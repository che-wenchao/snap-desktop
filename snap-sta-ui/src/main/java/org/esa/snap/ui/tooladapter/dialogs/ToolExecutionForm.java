/*
 *
 *  * Copyright (C) 2015 CS SI
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
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.ui.tooladapter.dialogs;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.binding.PropertyPane;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.descriptor.TemplateParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolParameterDescriptor;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.core.gpf.ui.DefaultIOParametersPanel;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.tooladapter.preferences.ToolAdapterOptionsController;
import org.esa.snap.utils.SpringUtilities;
import org.openide.util.NbPreferences;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Form for displaying execution parameters.
 * This form is part of <code>ToolAdapterExecutionDialog</code>.
 *
 * @author Ramona Manda
 */
class ToolExecutionForm extends JTabbedPane {
    private AppContext appContext;
    private ToolAdapterOperatorDescriptor operatorDescriptor;
    private PropertySet propertySet;
    private TargetProductSelector targetProductSelector;
    private DefaultIOParametersPanel ioParamPanel;
    private String fileExtension;
    private JCheckBox checkDisplayOutput;
    ConsolePane console;
    private final String TIF_EXTENSION = ".tif";
    JSplitPane bottomPane;

    public ToolExecutionForm(AppContext appContext, ToolAdapterOperatorDescriptor descriptor, PropertySet propertySet,
                             TargetProductSelector targetProductSelector) {
        this.appContext = appContext;
        this.operatorDescriptor = descriptor;
        this.propertySet = propertySet;
        this.targetProductSelector = targetProductSelector;

        //before executing, the sourceProduct and sourceProductFile must be removed from the list, since they cannot be edited
        Property sourceProperty = this.propertySet.getProperty(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_FILE);
        if(sourceProperty != null) {
            this.propertySet.removeProperty(sourceProperty);
        }
        sourceProperty = this.propertySet.getProperty(ToolAdapterConstants.TOOL_SOURCE_PRODUCT_ID);
        if(sourceProperty != null) {
            this.propertySet.removeProperty(sourceProperty);
        }
        // if the tool is handling by itself the output product name, then remove targetProductFile from the list,
        // since it may bring only confusion in this case
        if (operatorDescriptor.isHandlingOutputName()) {
            sourceProperty = this.propertySet.getProperty(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE);
            if (sourceProperty != null) {
                this.propertySet.removeProperty(sourceProperty);
            }
        }

        //initialise the target product's directory to the working directory
        final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
        targetProductSelectorModel.setProductDir(operatorDescriptor.resolveVariables(operatorDescriptor.getWorkingDir()));

        if(!operatorDescriptor.isHandlingOutputName() || operatorDescriptor.getSourceProductCount() > 0) {
            ioParamPanel = createIOParamTab();
            addTab("I/O Parameters", ioParamPanel);
        }
        JPanel processingParamPanel = new JPanel(new SpringLayout());
        checkDisplayOutput = new JCheckBox("Display execution output");
        checkDisplayOutput.setSelected(Boolean.parseBoolean(NbPreferences.forModule(Dialogs.class).get(ToolAdapterOptionsController.PREFERENCE_KEY_SHOW_EXECUTION_OUTPUT, "false")));
        processingParamPanel.add(checkDisplayOutput);
        bottomPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        bottomPane.setTopComponent(createProcessingParamTab());
        console = new ConsolePane();
        if (checkDisplayOutput.isSelected()) {
            bottomPane.setBottomComponent(console);
            bottomPane.setDividerLocation(0.6);
        }
        processingParamPanel.add(bottomPane);
        checkDisplayOutput.addActionListener((ActionEvent e) -> {
            if (!checkDisplayOutput.isSelected()) {
                bottomPane.remove(console);
            } else {
                bottomPane.setBottomComponent(console);
            }
            refreshDimension();
        });
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension((int)(screen.getWidth() / 3), (int)(screen.getHeight() / 2.5)));
        setMinimumSize(new Dimension(200, 200));
        SpringUtilities.makeCompactGrid(processingParamPanel, 2, 1, 2, 2, 2, 2);
        addTab("Processing Parameters", processingParamPanel);
        updateTargetProductFields();
    }

    /**
     * Reset the split procentage to 60%.
     */
    public void refreshDimension(){
        bottomPane.setDividerLocation(0.6);
        bottomPane.revalidate();
        bottomPane.repaint();
    }

    /**
     * Method called before actually showing the form, in which additional
     * initialisation may occur.
     */
    public void prepareShow() {
        if (ioParamPanel != null) {
            ioParamPanel.initSourceProductSelectors();
        }
    }

    /**
     * Method called before hiding the form, in which additional
     * cleanup may be performed.
     */
    public void prepareHide() {
        if (ioParamPanel != null) {
            ioParamPanel.releaseSourceProductSelectors();
        }
    }

    /**
     * Fetches the list of products selected in UI
     *
     * @return  The list of selected products to be used as input source
     */
    public Product[] getSourceProducts() {
        List<Product> sourceProducts = new ArrayList<>();
        if (ioParamPanel != null){
            ArrayList<SourceProductSelector> sourceProductSelectorList = ioParamPanel.getSourceProductSelectorList();
            sourceProducts.addAll(sourceProductSelectorList.stream().map(SourceProductSelector::getSelectedProduct).collect(Collectors.toList()));
        }
        return sourceProducts.toArray(new Product[sourceProducts.size()]);
    }

    public Object getPropertyValue(String propertyName) {
        Object result = null;
        if (propertySet.isPropertyDefined(propertyName)) {
            result = propertySet.getProperty(propertyName).getValue();
        }
        return result;
    }

    /**
     * Gets the target (destination) product file
     *
     * @return  The target product file
     */
    public File getTargetProductFile() {
        return targetProductSelector.getModel().getProductFile();
    }

    public boolean shouldDisplayOutput() {
        return checkDisplayOutput.isSelected();
    }

    private DefaultIOParametersPanel createIOParamTab() {
        final DefaultIOParametersPanel ioPanel = new DefaultIOParametersPanel(appContext, operatorDescriptor,
                targetProductSelector, !operatorDescriptor.isHandlingOutputName());
        final ArrayList<SourceProductSelector> sourceProductSelectorList = ioPanel.getSourceProductSelectorList();
        if (!sourceProductSelectorList.isEmpty()) {
            final SourceProductSelector sourceProductSelector = sourceProductSelectorList.get(0);
            sourceProductSelector.addSelectionChangeListener(new SourceProductChangeListener());
        }
        return ioPanel;
    }

    private JScrollPane createProcessingParamTab() {
        Arrays.stream(operatorDescriptor.getToolParameterDescriptors().toArray()).filter(p -> !((ToolParameterDescriptor)p).isTemplateParameter()).forEach(p ->
        {
            ToolParameterDescriptor param = (ToolParameterDescriptor)p;
            String label = param.getAlias();
            String propName = param.getName();
            if (label != null && !label.isEmpty() && propertySet.isPropertyDefined(propName)) {
                Property property = propertySet.getProperty(propName);
                property.getDescriptor().setDisplayName(label);
            }
        });

        Arrays.stream(operatorDescriptor.getToolParameterDescriptors().toArray())
                .filter(p -> ((ToolParameterDescriptor)p).isTemplateParameter())
                .forEach(tp -> {
                    TemplateParameterDescriptor tParam = (TemplateParameterDescriptor) tp;
                    propertySet.getProperty(tParam.getName()).getDescriptor().setAttribute("visible", false);
                    Arrays.stream(tParam.getParameterDescriptors().toArray())
                            .forEach(p -> {
                                ToolParameterDescriptor param = (ToolParameterDescriptor) p;
                                String label = param.getAlias();
                                String propName = param.getName();
                                if (label != null && !label.isEmpty() && propertySet.isPropertyDefined(propName)) {
                                    Property property = propertySet.getProperty(propName);
                                    property.getDescriptor().setDisplayName(label);
                                }
                            });
                });
        PropertyPane parametersPane = new PropertyPane(propertySet);
        final JPanel parametersPanel = parametersPane.createPanel();
        parametersPanel.addPropertyChangeListener(evt -> {
            if (!(evt.getNewValue() instanceof JTextField)) return;
            JTextField field = (JTextField) evt.getNewValue();
            String text = field.getText();
            if (text != null && text.isEmpty()) {
                field.setCaretPosition(text.length());
            }
        });
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        //parametersPanel.setPreferredSize(ioParamPanel.getPreferredSize());
        return new JScrollPane(parametersPanel);
    }

    private void updateTargetProductFields() {
        TargetProductSelectorModel model = targetProductSelector.getModel();
        Property property = propertySet.getProperty(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE);
        model.setSaveToFileSelected(false);
        if (!operatorDescriptor.isHandlingOutputName()) {
            Object value = property.getValue();
            if (value != null) {
                File file = operatorDescriptor.resolveVariables(new File(property.getValueAsText()));
                if (!file.isAbsolute()) {
                    file = new File(operatorDescriptor.getWorkingDir(), file.getAbsolutePath());
                }
                String productName = FileUtils.getFilenameWithoutExtension(file);
                if (fileExtension == null) {
                    fileExtension = FileUtils.getExtension(file);
                }
                model.setProductName(productName);
            }
        } else {
            try {
                model.setProductName("Output Product");
                if (property != null) {
                    property.setValue(null);
                }
                targetProductSelector.setEnabled(false);
                targetProductSelector.getSaveToFileCheckBox().setEnabled(false);
            } catch (ValidationException e) {
                Logger.getLogger(ToolExecutionForm.class.getName()).severe(e.getMessage());
            }
        }
        targetProductSelector.getProductDirTextField().setEnabled(false);
    }

    private class SourceProductChangeListener extends AbstractSelectionChangeListener {

        private static final String TARGET_PRODUCT_NAME_SUFFIX = "_processed";

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            if (!operatorDescriptor.isHandlingOutputName()) {
                Property targetProperty = propertySet.getProperty(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE);
                Object value = targetProperty.getValue();
                String productName = "";
                final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
                if (selectedProduct != null) {
                    productName = FileUtils.getFilenameWithoutExtension(selectedProduct.getName());
                }
                final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
                productName += TARGET_PRODUCT_NAME_SUFFIX;
                targetProductSelectorModel.setProductName(productName);
                String workingDir = operatorDescriptor.getWorkingDir();
                if (value != null) {
                    File oldValue = operatorDescriptor.resolveVariables(value instanceof File ? (File) value : new File((String) value));
                    if (fileExtension == null)
                        fileExtension = TIF_EXTENSION;
                    File current;
                    File parent;
                    if (oldValue != null && (parent = oldValue.getParentFile()) != null) {
                        current = new File(parent.getAbsolutePath(), productName + fileExtension);
                    } else {
                        current = new File(workingDir, productName + fileExtension);
                    }
                    propertySet.setValue(ToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE, current);
                } else {
                    try {
                        targetProperty.setValue(new File(workingDir, productName + TIF_EXTENSION));
                    } catch (ValidationException ignored) {
                    }
                }
            }
        }
    }

}
