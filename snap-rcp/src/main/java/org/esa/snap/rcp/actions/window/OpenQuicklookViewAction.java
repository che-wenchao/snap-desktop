/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.actions.window;

import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.quicklooks.QuicklookToolView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.windows.WindowManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.EXPLORER;

/**
 * This action opens a Quicklook View of the currently selected Quicklook Node
 */
@ActionID(category = "File", id = "OpenQuicklookViewAction" )
@ActionRegistration(displayName = "#CTL_OpenQuicklookViewAction_MenuText" )
@ActionReferences({
        @ActionReference(path = "Context/Product/Quicklook", position = 100),
        @ActionReference(path = "Menu/Window", position = 130)
})
@NbBundle.Messages("CTL_OpenQuicklookViewAction_MenuText=Open Quicklook Window")
public class OpenQuicklookViewAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private Lookup lookup;
    private final Lookup.Result<ProductNode> result;

    public OpenQuicklookViewAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OpenQuicklookViewAction(Lookup lookup) {
        this.lookup = lookup;
        result = lookup.lookupResult(ProductNode.class);
        result.addLookupListener(
                WeakListeners.create(LookupListener.class, this, result));
        setEnableState();
        putValue(Action.NAME, Bundle.CTL_OpenQuicklookViewAction_MenuText());
    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new OpenQuicklookViewAction(lookup);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnableState();
    }

    private void setEnableState() {
        ProductNode productNode = lookup.lookup(ProductNode.class);
        setEnabled(productNode != null && productNode instanceof Quicklook);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        openQuicklookView((Quicklook) SnapApp.getDefault().getSelectedProductNode(EXPLORER));
    }
    
    public void openQuicklookView(final Quicklook ql) {
        openDocumentWindow(ql);
    }

    private void openDocumentWindow(final Quicklook ql) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final QuicklookToolView window = (QuicklookToolView)WindowManager.getDefault().findTopComponent("QuicklookToolView");
                if(window != null) {
                    window.open();
                    window.requestActive();
                    window.setSelectedQuicklook(ql);
                }
            }
        });
    }
}
