package org.esa.snap.binning.operator.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.Grid;
import com.bc.ceres.swing.ListControlBar;
import org.esa.snap.binning.operator.VariableConfig;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls adding, removing and moving rows in the variable table.
 */
class VariableTableController extends ListControlBar.AbstractListController {

    final Grid grid;
    private final BinningFormModel binningFormModel;
    private final List<VariableItem> variableItems;

    VariableTableController(Grid grid, BinningFormModel binningFormModel) {
        this.grid = grid;
        this.binningFormModel = binningFormModel;
        this.variableItems = new ArrayList<>();
        addVariableConfigs(this.binningFormModel.getVariableConfigs());
    }

    @Override
    public boolean addRow(int index) {
        return editVariableItem(new VariableItem(), -1);
    }

    @Override
    public boolean removeRows(int[] indices) {
        grid.removeDataRows(indices);
        for (int i = indices.length - 1; i >= 0; i--) {
            variableItems.remove(indices[i]);
        }
        updateBinningFormModel();
        return true;
    }

    @Override
    public boolean moveRowUp(int index) {
        grid.moveDataRowUp(index);

        VariableItem vi1 = variableItems.get(index - 1);
        VariableItem vi2 = variableItems.get(index);
        variableItems.set(index - 1, vi2);
        variableItems.set(index, vi1);

        updateBinningFormModel();

        return true;
    }

    @Override
    public boolean moveRowDown(int index) {
        grid.moveDataRowDown(index);

        VariableItem vi1 = variableItems.get(index);
        VariableItem vi2 = variableItems.get(index + 1);
        variableItems.set(index, vi2);
        variableItems.set(index + 1, vi1);

        updateBinningFormModel();

        return true;
    }

    @Override
    public void updateState(ListControlBar listControlBar) {
    }

    void setVariableConfigs(VariableConfig[] variableConfigs) {
        clearGrid();
        variableItems.clear();
        addVariableConfigs(variableConfigs);
        updateBinningFormModel();
    }

    private void addDataRow(VariableItem vi) {
        EmptyBorder emptyBorder = new EmptyBorder(2, 2, 2, 2);

        JLabel nameLabel = new JLabel(vi.variableConfig.getName());
        nameLabel.setBorder(emptyBorder);

        JLabel exprLabel = new JLabel(vi.variableConfig.getExpr());
        exprLabel.setBorder(emptyBorder);

        JLabel validExprLabel = new JLabel(vi.variableConfig.getValidExpr());
        validExprLabel.setBorder(emptyBorder);


        final AbstractButton editButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/snap/resources/images/icons/Edit16.gif"),
                                                                         false);
        editButton.setRolloverEnabled(true);
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int rowIndex = grid.findDataRowIndex(editButton);
                editVariableItem(variableItems.get(rowIndex), rowIndex);
            }
        });

        grid.addDataRow(
            /*1*/ nameLabel,
            /*2*/ exprLabel,
            /*3*/ validExprLabel,
            /*4*/ editButton);

        variableItems.add(vi);

    }

    private boolean editVariableItem(VariableItem variableItem, int rowIndex) {
        final Product contextProduct = binningFormModel.getContextProduct();
        if (contextProduct == null) {
            AbstractDialog.showInformationDialog(grid, "At least one source product must be set first", "Information");
            return false;
        }
        boolean newVariable = rowIndex == -1;
        if (newVariable) {
            int newVarIndex = variableItems.size();
            String varName;
            do {
                varName = "variable_" + newVarIndex++;
            } while (contextProduct.containsRasterDataNode(varName));

            variableItem.variableConfig.setName(varName);
        }

        String oldVarName = variableItem.variableConfig.getName();
        final VariableItemDialog variableItemDialog = new VariableItemDialog(SwingUtilities.getWindowAncestor(grid), variableItem,
                                                                             newVariable, contextProduct);
        if (variableItemDialog.show() == ModalDialog.ID_OK) {
            if (newVariable) {
                addDataRow(variableItem);
            } else {
                updateDataRow(variableItem, rowIndex);
                if (!oldVarName.equals(variableItem.variableConfig.getName())) {
                    updateVariableExpressions(oldVarName, variableItem.variableConfig.getName());
                }
            }
            updateBinningFormModel();
            return true;
        }
        return false;
    }

    private void updateVariableExpressions(String oldVarName, String newVarName) {
        for (int i = 0; i < variableItems.size(); i++) {
            VariableItem updatedItem = variableItems.get(i);
            VariableConfig updatedVarConfig = updatedItem.variableConfig;
            if (updatedVarConfig.getExpr().contains(oldVarName)) {
                String updatedExpr = updatedVarConfig.getExpr().replace(oldVarName, newVarName);
                updatedVarConfig.setExpr(updatedExpr);
                updateDataRow(updatedItem, i);
            }
        }
    }

    private void updateBinningFormModel() {
        VariableConfig[] variableConfigs = new VariableConfig[variableItems.size()];
        for (int i = 0; i < variableItems.size(); i++) {
            VariableItem variableItem = variableItems.get(i);
            variableConfigs[i] = variableItem.variableConfig;
        }
        try {
            binningFormModel.setVariableConfigs(variableConfigs);
        } catch (ValidationException e) {
            AbstractDialog.showErrorDialog(grid, e.getMessage(), "Aggregator Configuration");
        }

    }

    private void clearGrid() {
        int[] rowIndices = new int[grid.getDataRowCount()];
        for (int i = 0; i < rowIndices.length; i++) {
            rowIndices[i] = i;
        }
        removeRows(rowIndices);
    }

    private void updateDataRow(VariableItem variableItem, int rowIndex) {
        JComponent[] components = grid.getDataRow(rowIndex);
        ((JLabel) components[0]).setText(variableItem.variableConfig.getName());
        ((JLabel) components[1]).setText(variableItem.variableConfig.getExpr());
        ((JLabel) components[2]).setText(variableItem.variableConfig.getValidExpr());
    }

    private void addVariableConfigs(VariableConfig[] variableConfigs) {
        for (VariableConfig variableConfig : variableConfigs) {
            addDataRow(new VariableItem(variableConfig));
        }
    }

}
