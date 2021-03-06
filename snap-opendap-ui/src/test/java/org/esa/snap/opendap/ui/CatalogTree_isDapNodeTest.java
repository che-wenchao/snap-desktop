package org.esa.snap.opendap.ui;

import org.esa.snap.opendap.datamodel.OpendapLeaf;
import org.junit.Test;
import thredds.catalog.InvDataset;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.junit.Assert.*;

public class CatalogTree_isDapNodeTest {

    @Test
    public void testThatNullIsResolvedToFalse() {
        final Object noDapNode = null;
        assertEquals(false, CatalogTreeUtils.isDapNode(noDapNode));
    }

    @Test
    public void testThatUserObjectWhichIsNoOpendapLeafIsResolvedToFalse() {
        final Integer userObject = 4;
        final DefaultMutableTreeNode noDapNode = new DefaultMutableTreeNode(userObject);
        assertEquals(false, CatalogTreeUtils.isDapNode(noDapNode));
    }

    @Test
    public void testThatOpendapLeafWhichHasNoDapServiceSetIsResolvedToFalse() {
        final OpendapLeaf userObject = new OpendapLeaf("name", new InvDataset(null, "") {
        });
        userObject.setDapAccess(false);
        final DefaultMutableTreeNode noDapNode = new DefaultMutableTreeNode(userObject);
        assertEquals(false, CatalogTreeUtils.isDapNode(noDapNode));
    }

    @Test
    public void testThatOpendapLeafWhichHasDapServiceSetIsResolvedToTrue() {
        final OpendapLeaf userObject = new OpendapLeaf("name", new InvDataset(null, "") {
        });
        userObject.setDapAccess(true);
        final DefaultMutableTreeNode notADapNode = new DefaultMutableTreeNode(userObject);
        assertEquals(true, CatalogTreeUtils.isDapNode(notADapNode));
    }
}
