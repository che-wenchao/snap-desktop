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

package org.esa.snap.ui.tooladapter.dialogs;

import org.esa.snap.core.gpf.descriptor.OSFamily;
import org.esa.snap.core.gpf.descriptor.SystemVariable;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.dependency.BundleType;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterOp;
import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterRegistry;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.modules.ModulePackager;
import org.esa.snap.modules.ModuleSuiteDescriptor;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.tooladapter.actions.EscapeAction;
import org.esa.snap.ui.tooladapter.dialogs.components.EntityForm;
import org.esa.snap.ui.tooladapter.model.ProgressWorker;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dialog for creating a module suite nbm package
 *
 * @author  Cosmin Cara
 */
public class ModuleSuiteDialog extends ModalDialog {

    private static final int YEAR;
    private JTable operatorsTable;
    private ModuleSuiteDescriptor descriptor;
    private Map<OSFamily, org.esa.snap.core.gpf.descriptor.dependency.Bundle> bundles;
    private EntityForm<ModuleSuiteDescriptor> descriptorForm;
    private BundleForm bundleForm;
    private Set<ToolAdapterOperatorDescriptor> initialSelection;
    private Map<String, SystemVariable> commonVariables;

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        YEAR = calendar.get(Calendar.YEAR);
    }

    ModuleSuiteDialog(AppContext appContext, String title, String helpID, Set<ToolAdapterOperatorDescriptor> selection) {
        super(appContext.getApplicationWindow(), title, ID_OK | ID_CANCEL, helpID);
        this.descriptor = new ModuleSuiteDescriptor();
        this.descriptor.setAuthors(System.getProperty("user.name"));
        this.descriptor.setName("NewBundle");
        this.descriptor.setVersion("1");
        this.descriptor.setCopyright("(C)" + String.valueOf(YEAR) + " " + this.descriptor.getAuthors());
        this.bundles = new HashMap<>();
        this.bundles.put(OSFamily.windows,
                new org.esa.snap.core.gpf.descriptor.dependency.Bundle(
                    new ToolAdapterOperatorDescriptor("bundle", ToolAdapterOp.class),
                    BundleType.ZIP,
                    SystemUtils.getAuxDataPath().toString()) {{
                        setOS(OSFamily.windows);
                }});
        this.bundles.put(OSFamily.linux,
                new org.esa.snap.core.gpf.descriptor.dependency.Bundle(
                        new ToolAdapterOperatorDescriptor("bundle", ToolAdapterOp.class),
                        BundleType.ZIP,
                        SystemUtils.getAuxDataPath().toString()) {{
                    setOS(OSFamily.linux);
                }});
        this.bundles.put(OSFamily.macosx,
                new org.esa.snap.core.gpf.descriptor.dependency.Bundle(
                        new ToolAdapterOperatorDescriptor("bundle", ToolAdapterOp.class),
                        BundleType.ZIP,
                        SystemUtils.getAuxDataPath().toString()) {{
                    setOS(OSFamily.macosx);
                }});
        this.initialSelection = new HashSet<>();
        this.commonVariables = new HashMap<>();
        if (selection != null) {
            this.initialSelection.addAll(selection);
            this.initialSelection.forEach(d -> {
                for (SystemVariable variable : d.getVariables()) {
                    this.commonVariables.put(variable.getKey(), variable);
                }
            });
        }
        JPanel contentPanel = createContentPanel();
        setContent(contentPanel);
        super.getJDialog().setMinimumSize(contentPanel.getPreferredSize());
        EscapeAction.register(super.getJDialog());
    }

    private JPanel createContentPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        JPanel panel = new JPanel(layout);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Suite description:"), constraints);
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Adapters to include:"), constraints);

        JPanel descriptorPanel = createDescriptorPanel();
        descriptorPanel.setPreferredSize(new Dimension(350, 250));
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(descriptorPanel, constraints);
        JTable adaptersTable = createAdaptersTable();
        JScrollPane scrollPane = new JScrollPane(adaptersTable);
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, constraints);

        JPanel bundlePanel = createBundlePanel();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(bundlePanel, constraints);

        panel.setPreferredSize(new Dimension(680, 350));
        return panel;
    }

    private JTable createAdaptersTable() {
        java.util.List<ToolAdapterOperatorDescriptor> toolboxSpis = new ArrayList<>();
        toolboxSpis.addAll(ToolAdapterRegistry.INSTANCE.getOperatorMap().values()
                .stream()
                .map(e -> (ToolAdapterOperatorDescriptor) e.getOperatorDescriptor())
                .collect(Collectors.toList()));
        toolboxSpis.sort(Comparator.comparing(ToolAdapterOperatorDescriptor::getAlias));
        AdapterListModel model = new AdapterListModel(toolboxSpis);
        operatorsTable = new JTable(model);
        operatorsTable.getSelectionModel().addListSelectionListener(e -> onSelectionChanged());
        TableColumn checkColumn = operatorsTable.getColumnModel().getColumn(0);
        int checkColumnWidth = 24;
        checkColumn.setMaxWidth(checkColumnWidth);
        checkColumn.setPreferredWidth(checkColumnWidth);
        checkColumn.setResizable(false);
        TableColumn aliasColumn = operatorsTable.getColumnModel().getColumn(1);
        aliasColumn.setPreferredWidth(10 * checkColumnWidth);
        return operatorsTable;
    }

    private JPanel createDescriptorPanel() {
        this.descriptorForm = new EntityForm<>(this.descriptor);
        JPanel panel = descriptorForm.getPanel();
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return panel;
    }

    private JPanel createBundlePanel() {
        this.bundleForm = new BundleForm(SnapApp.getDefault().getAppContext(),
                                         this.bundles.get(OSFamily.windows),
                                         this.bundles.get(OSFamily.linux),
                                         this.bundles.get(OSFamily.macosx),
                                         new ArrayList<>(this.commonVariables.values()));
        this.bundleForm.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return this.bundleForm;
    }

    private void onSelectionChanged() {
        ToolAdapterOperatorDescriptor[] selection = ((AdapterListModel) this.operatorsTable.getModel()).getSelectedItems();
        this.commonVariables.clear();
        for (ToolAdapterOperatorDescriptor descriptor : selection) {
            List<SystemVariable> variables = descriptor.getVariables();
            for (SystemVariable variable : variables) {
                this.commonVariables.put(variable.getKey(), variable);
            }
        }
        this.bundleForm.setVariables(new ArrayList<>(this.commonVariables.values()));
    }

    @Override
    protected void onOK() {
        ToolAdapterOperatorDescriptor[] selection = ((AdapterListModel) this.operatorsTable.getModel()).getSelectedItems();
        if (selection.length > 0) {
            this.descriptor = this.descriptorForm.applyChanges();
            final Map<OSFamily, org.esa.snap.core.gpf.descriptor.dependency.Bundle> bundles = this.bundleForm.applyChanges();
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(getButton(ID_OTHER)) == JFileChooser.APPROVE_OPTION) {
                File targetFolder = fileChooser.getSelectedFile();
                final String nbmName = this.descriptor.getName() + ".nbm";
                ProgressWorker worker = new ProgressWorker("Export Module Suite", "Creating NetBeans module suite " + nbmName,
                        () -> {
                            try {
                                ModulePackager.packModules(this.descriptor, new File(targetFolder, nbmName), bundles, selection);
                                Dialogs.showInformation(String.format(Bundle.MSG_Export_Complete_Text(), targetFolder.getAbsolutePath()), null);
                            } catch (IOException e) {
                                SystemUtils.LOG.warning(e.getMessage());
                                Dialogs.showError(e.getMessage());
                            }
                        });
                worker.executeWithBlocking();
                super.onOK();
            }
        } else {
            Dialogs.showWarning("Please select at least one adapter");
        }
    }

    private class AdapterListModel extends AbstractTableModel {
        private Object[][] checkedList;
        private String[] columnNames;

        AdapterListModel(List<ToolAdapterOperatorDescriptor> descriptors) {
            super();
            this.checkedList = new Object[descriptors.size()][2];
            for (int i = 0; i < descriptors.size(); i++) {
                ToolAdapterOperatorDescriptor descriptor = descriptors.get(i);
                this.checkedList[i][0] = ModuleSuiteDialog.this.initialSelection.contains(descriptor);
                this.checkedList[i][1] = descriptor;
            }
            this.columnNames = new String[] { "", "Adapter Alias" };
        }

        @Override
        public String getColumnName(int column) {
            return this.columnNames[column];
        }

        @Override
        public int getRowCount() {
            return this.checkedList != null ? this.checkedList.length : 0;
        }

        @Override
        public int getColumnCount() {
            return columnNames != null ? columnNames.length : 0;
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return this.checkedList[row][0];
                case 1:
                    return ((ToolAdapterOperatorDescriptor) this.checkedList[row][1]).getAlias();
                default:
                    return null;
            }
        }

        ToolAdapterOperatorDescriptor[] getSelectedItems() {
            List<ToolAdapterOperatorDescriptor> selection = new ArrayList<>();
            for (Object[] aCheckedList : this.checkedList) {
                if (aCheckedList[0] == Boolean.TRUE) {
                    selection.add((ToolAdapterOperatorDescriptor) aCheckedList[1]);
                }
            }
            return selection.toArray(new ToolAdapterOperatorDescriptor[selection.size()]);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 0)
                return;
            this.checkedList[rowIndex][0] = aValue;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Boolean.class;
                case 1:
                    return String.class;
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 1;
        }
    }
}
