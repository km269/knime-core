/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Special <code>NodeExtraInfo</code> object used by the workflow editor.
 * Basically this stores the visual bounds of the node in the workflow editor
 * pane. Note: To be independent of draw2d/GEF this doesn't use the "natural"
 * <code>Rectangle</code> object, but simply stores an <code>int[]</code>.
 *
 * TODO This needs to be in "core", as by now the WFM tries to make instances of
 * this class while <code>load()</code>ing.
 *
 *
 * see org.eclipse.draw2d.geometry.Rectangle
 *
 * @author Florian Georg, University of Konstanz
 */
public class NodeUIInformation implements UIInformation {
    /** TODO: Version id of this extra info implementation. */
    // private static final String VERSION = "1.0";

    /** TODO: The key under which the type is registered. * */
    // private static final String KEY_VERSION = "extrainfo.node.version";

    /** The key under which the bounds are registered. * */
    private static final String KEY_BOUNDS = "extrainfo.node.bounds";

    private int[] m_bounds = new int[]{0, 0, -1, -1};
    
    /** Set to true if the bounds are absolute (correct in the context of the
     * editor). It's false if the coordinates refer to relative coordinates and
     * need to be adjusted by the NodeContainerFigure#initFigure... method.
     * This field is transient and not stored as part of the 
     * {@link #save(NodeSettingsWO)} method. A loaded object has always absolute
     * coordinates.
     */
    private final boolean m_hasAbsoluteCoordinates; 

    /** Creates new object, the bounds to be set are assumed to be absolute
     * (m_isInitialized is true). */
    public NodeUIInformation() {
        m_hasAbsoluteCoordinates = true;
    }
    
    /** Inits new node figure with given coordinates.
     * @param x x coordinate
     * @param y y coordinate
     * @param width width of figure
     * @param height height of figure
     * @param absoluteCoords If the coordinates are absolute.
     */
    public NodeUIInformation(final int x, final int y, 
            final int width, final int height, final boolean absoluteCoords) {
        m_bounds[0] = x;
        m_bounds[1] = y;
        m_bounds[2] = width;
        m_bounds[3] = height;
        m_hasAbsoluteCoordinates = absoluteCoords;
    }

    /**
     * {@inheritDoc}
     */
    public void save(final NodeSettingsWO config) {
        config.addIntArray(KEY_BOUNDS, m_bounds);
    }

    /**
     * {@inheritDoc}
     */
    public void load(final NodeSettingsRO conf)
        throws InvalidSettingsException {
        m_bounds = conf.getIntArray(KEY_BOUNDS);
    }

    /**
     * Returns if the loaded UI information is complete.
     *
     * @return <code>true</code> if it is filled properly, <code>false</code>
     * otherwise
     */
    public boolean isFilledProperly() {
        if (m_bounds == null) {
            return false;
        }
        return true;
    }
    
    /**
     * @return the hasAbsoluteCoordinates (transient) field
     */
    public boolean hasAbsoluteCoordinates() {
        return m_hasAbsoluteCoordinates;
    }
    
    /**
     * Sets the location. *
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param w width
     * @param h height
     *
     */
    public void setNodeLocation(final int x, final int y, final int w,
            final int h) {
        m_bounds[0] = x;
        m_bounds[1] = y;
        m_bounds[2] = w;
        m_bounds[3] = h;
    }

    /**
     * @return Returns a clone of the bounds.
     */
    public int[] getBounds() {
        return m_bounds.clone();
    }

    /**
     * Changes the position by setting the bounds left top corner according to
     * the given moving distance.
     *
     * @param moveDist the distance to change the left top corner
     * @return A clone of this ui information, whereby its x,y coordinates
     *         are shifted by the argument values.
     */
    public NodeUIInformation createNewWithOffsetPosition(final int[] moveDist) {
        return new NodeUIInformation(
                m_bounds[0] + moveDist[0], m_bounds[1] + moveDist[1],
                m_bounds[2], m_bounds[3], m_hasAbsoluteCoordinates);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInformation clone() {
        NodeUIInformation newObject;
        try {
            newObject = (NodeUIInformation)super.clone();
        } catch (CloneNotSupportedException e) {
            NodeLogger.getLogger(getClass()).fatal("Clone exception", e);
            newObject = new NodeUIInformation();
        }
        newObject.m_bounds = this.m_bounds.clone();
        return newObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_bounds == null) {
            return "not set";
        }
        return "x " + m_bounds[0] + " y " + m_bounds[1]
               + " width " + m_bounds[2] + " height "  + m_bounds[3]
               + (m_hasAbsoluteCoordinates ? "(absolute)" : "(relative)");
    }
}
