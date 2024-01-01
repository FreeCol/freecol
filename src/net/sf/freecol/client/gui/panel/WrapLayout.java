/**
 *  Copyright (C) 2002-2023   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * FlowLayout like layout manager that fully supports wrapping of components.
 */
public class WrapLayout implements LayoutManager {


    private int hgap = 0;
    private int vgap = 0;
    
    
    public int getHgap() {
        return hgap;
    }
    
    public int getVgap() {
        return vgap;
    }
    
    
    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void removeLayoutComponent(Component comp) {}

    /**
     * Returns the preferred dimensions for this layout given the <i>visible</i>
     * components in the specified target container.
     * 
     * @param target the component which needs to be laid out
     * @return the preferred dimensions to lay out the subcomponents of the
     *         specified container
     */
    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    /**
     * Returns the minimum dimensions needed to layout the <i>visible</i> components
     * contained in the specified target container.
     * 
     * @param target the component which needs to be laid out
     * @return the minimum dimensions to lay out the subcomponents of the specified
     *         container
     */
    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    /**
     * Returns the minimum or preferred dimension needed to layout the target
     * container.
     *
     * @param target    target to get layout size for
     * @param preferred should preferred size be calculated
     * @return the dimension to layout the target container
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            final Layout mostCompactLayout = determineLayout(target, preferred); 
            return mostCompactLayout.dimension;
        }
    }
    
    public Dimension layoutSize(Container target, int targetWidth, boolean preferred) {
        synchronized (target.getTreeLock()) {
            final Layout mostCompactLayout = determineLayout(target, targetWidth, preferred); 
            return mostCompactLayout.dimension;
        }
    }
    
    private Layout determineLayout(Container target, boolean preferred) {
        int targetWidth = target.getSize().width;
        Container container = target;
        while (container.getSize().width == 0 && container.getParent() != null) {
            container = container.getParent();
        }
        targetWidth = container.getSize().width;
        if (targetWidth == 0) {
            targetWidth = Integer.MAX_VALUE;
        }
        
        return determineLayout(target, targetWidth, preferred);
    }

    private Layout determineLayout(Container target, int targetWidth, boolean preferred) {
        Layout mostCompactLayout = internalLayoutSize(target, targetWidth, preferred);
        if (targetWidth <= 0) {
            return mostCompactLayout;
        }
        for (int i=0; i<3; i++) {
            final Layout candidateLayout = internalLayoutSize(target, mostCompactLayout.widestColumn - 1, preferred);
            if (candidateLayout.dimension.height <= mostCompactLayout.dimension.height) {
                mostCompactLayout = candidateLayout;
            } else {
                break;
            }
        }
        return mostCompactLayout;
    }
    
    private Layout internalLayoutSize(Container target, int targetWidth, boolean preferred) {
        // Each row must fit with the width allocated to the containter.
        // When the container width = 0, the preferred width of the container
        // has not yet been calculated so lets ask for the maximum.

        final Map<Component, Rectangle> positions = new HashMap<>();
        
        final int hgap = getHgap();
        final int vgap = getVgap();
        final Insets insets = target.getInsets();
        final int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
        final int maxWidth = targetWidth - horizontalInsetsAndGap;

        // Fit components into the allowed width

        final Dimension dim = new Dimension(0, 0);
        int rowWidth = 0;
        int rowHeight = 0;

        final int nmembers = target.getComponentCount();

        int widestColumn = Integer.MIN_VALUE;
        for (int i = 0; i < nmembers; i++) {
            Component m = target.getComponent(i);

            if (m.isVisible()) {
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                // Can't add the component to current row. Start a new row.
                if (rowWidth + d.width > maxWidth) {
                    if (rowWidth > widestColumn) {
                        widestColumn = rowWidth;
                    }
                    addRow(dim, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }

                // Add a horizontal gap for all components after the first

                if (rowWidth != 0) {
                    rowWidth += hgap;
                }
                positions.put(m, new Rectangle(rowWidth, dim.height, d.width, d.height));

                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
        }
        if (rowWidth > widestColumn) {
            widestColumn = rowWidth;
        }
        addRow(dim, rowWidth, rowHeight);

        dim.width += horizontalInsetsAndGap;
        dim.height += insets.top + insets.bottom + vgap * 2;

        // When using a scroll pane or the DecoratedLookAndFeel we need to
        // make sure the preferred size is less than the size of the
        // target containter so shrinking the container size works
        // correctly. Removing the horizontal gap is an easy way to do this.

        Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

        if (scrollPane != null && target.isValid()) {
            dim.width -= (hgap + 1);
        }

        return new Layout(dim, widestColumn, positions);
    }
    
    private static class Layout {
        private Dimension dimension;
        private int widestColumn;
        private Map<Component, Rectangle> positions; 
        
        private Layout(Dimension dimension, int widestColumn, Map<Component, Rectangle> positions) {
            this.dimension = dimension;
            this.widestColumn = widestColumn;
            this.positions = positions;
        }
    }

    /*
     * A new row has been completed. Use the dimensions of this row to update the
     * preferred size for the container.
     *
     * @param dim update the width and height when appropriate
     * 
     * @param rowWidth the width of the row to add
     * 
     * @param rowHeight the height of the row to add
     */
    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);

        if (dim.height > 0) {
            dim.height += getVgap();
        }

        dim.height += rowHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void layoutContainer(Container parent) {
        layoutContainer(parent, true);
    }
    
    public void layoutContainer(Container parent, boolean preferred) {
        synchronized (parent.getTreeLock()) {
            final Dimension size = parent.getSize();
            final Layout mostCompactLayout = determineLayout(parent, size.width, preferred);
            final Point offset = determineOffsetForCentering(size, mostCompactLayout.dimension);
            for (Entry<Component, Rectangle> child : mostCompactLayout.positions.entrySet()) {
                child.getKey().setBounds(reposition(child.getValue(), offset));
            }
        }
    }
    
    final Point determineOffsetForCentering(Dimension displaySize, Dimension contentSize) {
        return new Point((displaySize.width - contentSize.width) / 2,
                (displaySize.height - contentSize.height) / 2);
    }
    
    final Rectangle reposition(Rectangle bounds, Point offset) {
        return new Rectangle(bounds.x + offset.x, bounds.y + offset.y, bounds.width, bounds.height);
    }
}