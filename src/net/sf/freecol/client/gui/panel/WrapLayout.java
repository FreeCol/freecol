/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * FlowLayout like layout manager that supports automatic wrapping to a new line when needed.
 */
public class WrapLayout implements LayoutManager {

    private static final int MAX_COMPRESS_TRIES = 3;

    public enum HorizontalAlignment {
        LEFT,
        CENTER,
        RIGHT;
    }
    
    public enum LayoutStyle {
        /**
         * Layout the components using the narrowest width per row that
         * still does not increase the height.
         */
        BALANCED,
        
        /**
         * Layout the components from top to bottom, only making a new row
         * when the width would otherwise increase beyond the bounds.
         */
        PREFER_TOP,
        
        /**
         * Layout the components from bottom to top, only making a new row
         * when the width would otherwise increase beyond the bounds.
         * 
         * Note that the components is in the same order as when using
         * PREFER_TOP, but the widest rows will tend to occur more often
         * at the bottom rows rather than the top rows.
         */
        PREFER_BOTTOM
    }
    
    public enum HorizontalGap {
        /**
         * No extra horizontal gaps will be added between components.
         */
        NONE,
        
        /**
         * Automatic horizontal gaps for every row.
         */
        AUTO,
        
        /**
         * Automatic horizontal gaps only for the first row.
         */
        AUTO_TOP,
        
        /**
         * Automatic horizontal gaps only for the last row.
         */
        AUTO_BOTTOM
    }
    
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;
    private LayoutStyle layoutStyle = LayoutStyle.BALANCED;
    private HorizontalGap horizontalGap = HorizontalGap.AUTO;
    private Dimension forceComponentSize = null;
    private boolean allComponentsWithTheSameSize = false;
    
    private int minHorizontalGap = 0;
    private int minVerticalGap = 0;
    private int maxHorizontalGap = 0;
    
    
    /**
     * Creates a {@code WrapLayout} with default settings. Specify layout rules using the
     * {@code with*} methods.
     * 
     * @see #withHorizontalAlignment(HorizontalAlignment)
     * @see #withLayoutStyle(LayoutStyle)
     * @see #withHorizontalGap(HorizontalGap, int)
     */
    public WrapLayout() {
        
    }
    
    
    /**
     * Sets the horizontal alignment.
     * @param horizontalAlignment The alignment to be used.
     * @return This object, in order to support method chaining.
     */
    public WrapLayout withHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
        this.horizontalAlignment = Objects.requireNonNull(horizontalAlignment, "horizontalAlignment");
        return this;
    }
    
    /**
     * Chooses the style when laying out components.
     * @param layoutStyle The style to be used.
     * @return This object, in order to support method chaining.
     */
    public WrapLayout withLayoutStyle(LayoutStyle layoutStyle) {
        this.layoutStyle = Objects.requireNonNull(layoutStyle, "layoutStyle");
        return this;
    }
    
    /**
     * Defines how horizontal gaps will be handled.
     * 
     * @param horizontalGap The method for choosing the gap size.
     * @return This object, in order to support method chaining.
     */
    public WrapLayout withHorizontalGap(HorizontalGap horizontalGap) {
        this.horizontalGap = Objects.requireNonNull(horizontalGap, "horizontalGap");
        this.minHorizontalGap = 0;
        this.maxHorizontalGap = Integer.MAX_VALUE;
        return this;
    }
    
    /**
     * Defines how horizontal gaps will be handled.
     * 
     * @param horizontalGap The method for choosing the gap size.
     * @param minHorizontalGap The minimum horizontal gap to always be placed between
     *      components (even if there is no room for it).
     * @param maxHorizontalGap The maximum allowed horizontal gap between components.
     * @return This object, in order to support method chaining.
     */
    public WrapLayout withHorizontalGap(HorizontalGap horizontalGap, int minHorizontalGap, int maxHorizontalGap) {
        if (maxHorizontalGap < 0) {
            throw new IllegalArgumentException("maxHorizontalGap must be >= 0. Argument: " + maxHorizontalGap);
        }
        this.minHorizontalGap = minHorizontalGap;
        this.horizontalGap = Objects.requireNonNull(horizontalGap, "horizontalGap");
        this.maxHorizontalGap = maxHorizontalGap;
        return this;
    }
    
    /**
     * Forces all child components to have the given size.
     * 
     * @param forceComponentSize The component size.
     * @return This object, in order to support method chaining.
     */
    public WrapLayout withForceComponentSize(Dimension forceComponentSize) {
        this.forceComponentSize = forceComponentSize;
        this.allComponentsWithTheSameSize = false;
        return this;
    }
    
    /**
     * Uses the biggest preferred/minimum size for all child components.
     * 
     * @param allComponentsWithTheSameSize {@code true} if all components should
     *      get the same same.
     * @return This object, in order to support method chaining.
     */
    public WrapLayout withAllComponentsWithTheSameSize(boolean allComponentsWithTheSameSize) {
        this.allComponentsWithTheSameSize = allComponentsWithTheSameSize;
        this.forceComponentSize = null;
        return this;
    }

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
        return minimum;
    }
    
    /**
     * Determines the layout size. To be used by other layout managers using
     * {@code WrapLayout} as a fallback.
     */
    public Dimension layoutSize(Container target, int targetWidth, boolean preferred) {
        synchronized (target.getTreeLock()) {
            final Layout mostCompactLayout = determineLayout(target, targetWidth, preferred); 
            return mostCompactLayout.dimension;
        }
    }

    
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            final Layout mostCompactLayout = determineLayout(target, preferred); 
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
        final boolean reverseComponentOrder = (layoutStyle == LayoutStyle.PREFER_BOTTOM);
        Layout mostCompactLayout = internalLayoutSize(target, targetWidth, preferred, reverseComponentOrder);
        if (targetWidth <= 0) {
            return mostCompactLayout;
        }
        if (layoutStyle != LayoutStyle.BALANCED) {
            return mostCompactLayout;
        }
        for (int i=0; i<MAX_COMPRESS_TRIES; i++) {
            final Layout candidateLayout = internalLayoutSize(target, mostCompactLayout.getWidestColumnWidth() - 1, preferred, reverseComponentOrder);
            if (candidateLayout.dimension.height <= mostCompactLayout.dimension.height) {
                mostCompactLayout = candidateLayout;
            } else {
                break;
            }
        }
        return mostCompactLayout;
    }
    
    private Layout internalLayoutSize(Container target, int targetWidth, boolean preferred, boolean layoutBottomToTop) {
        final Insets insets = target.getInsets();
        final int horizontalInsetsAndGap = insets.left + insets.right + (minHorizontalGap * 2);
        final int maxWidth = targetWidth - horizontalInsetsAndGap;

        final List<Row> rows = new ArrayList<>();
        final Dimension currentLayoutSize = new Dimension(0, 0);
        
        Dimension sharedComponentSize = forceComponentSize;
        if (allComponentsWithTheSameSize) {
            sharedComponentSize = new Dimension(0, 0);
            for (Component component : getVisibleComponents(target, layoutBottomToTop)) {
                final Dimension d = preferred ? component.getPreferredSize() : component.getMinimumSize();
                if (d.width > sharedComponentSize.width) {
                    sharedComponentSize.width = d.width;
                }
                if (d.height > sharedComponentSize.height) {
                    sharedComponentSize.height = d.height;
                }
            }
        }
        
        List<Child> currentChildren = new ArrayList<>();
        Dimension currentRowSize = new Dimension(0, 0);
        for (Component component : getVisibleComponents(target, layoutBottomToTop)) {
            final Dimension d;
            if (sharedComponentSize != null) {
                d = sharedComponentSize;
            } else {
                d = preferred ? component.getPreferredSize() : component.getMinimumSize();
            }

            // Can't add the component to current row. Start a new row.
            if (currentRowSize.width + d.width > maxWidth) {
                updateCurrentLayoutSize(currentLayoutSize, currentRowSize);
                rows.add(new Row(currentRowSize, currentChildren));
                
                currentChildren = new ArrayList<>();
                currentRowSize = new Dimension(0, 0);
            }

            // Add a horizontal gap for all components after the first
            if (currentRowSize.width != 0) {
                currentRowSize.width += minHorizontalGap;
            }
            
            currentChildren.add(new Child(component, new Rectangle(currentRowSize.width, currentLayoutSize.height, d.width, d.height)));
            currentRowSize.width += d.width;
            currentRowSize.height = Math.max(currentRowSize.height, d.height);
        }
        currentLayoutSize.width = Math.max(currentLayoutSize.width, currentRowSize.width);
        currentLayoutSize.height += currentRowSize.height;
        rows.add(new Row(currentRowSize, currentChildren));

        currentLayoutSize.width += horizontalInsetsAndGap;
        currentLayoutSize.height += insets.top + insets.bottom;

        // When using a scroll pane or the DecoratedLookAndFeel we need to
        // make sure the preferred size is less than the size of the
        // target containter so shrinking the container size works
        // correctly. Removing the horizontal gap is an easy way to do this.
        Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
        if (scrollPane != null && target.isValid()) {
            currentLayoutSize.width -= (minHorizontalGap + 1);
        }
        
        if (layoutBottomToTop) {
            reverseLayout(rows);
        }

        return new Layout(currentLayoutSize, rows);
    }

    private void reverseLayout(final List<Row> rows) {
        if (rows.isEmpty()) {
            return;
        }
        
        Collections.reverse(rows);
        int y = 0;
        int rowNum = 0;
        final int lastRowIndex = rows.size() - 1;
        for (Row row : rows) {
            Collections.reverse(row.children);
            int x = 0;
            int childNum = 0;
            final int lastChildIndex = row.children.size() - 1;
            for (Child child : row.children) {
                child.bounds.x = x;
                child.bounds.y = y;
                x += child.bounds.width;
                if (childNum != lastChildIndex) {
                    x += minHorizontalGap;
                }
                childNum++;
            }
            y += row.size.height;
            if (rowNum != lastRowIndex) {
                y += minVerticalGap;
            }
            rowNum++;
        }
    }

    private void updateCurrentLayoutSize(Dimension currentLayoutSize, Dimension newRowSize) {
        currentLayoutSize.width = Math.max(currentLayoutSize.width, newRowSize.width);
        currentLayoutSize.height += minVerticalGap;
        currentLayoutSize.height += newRowSize.height;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            final Dimension size = parent.getSize();
            Layout layout = determineLayout(parent, size.width, true);
            if (layout.dimension.width > size.width || layout.dimension.height > size.height) {
                layout = determineLayout(parent, size.width, false);
            }
            applyLayout(parent, size, layout);
        }
    }
    
    /**
     * Layout of the container. To be used by other layout managers using
     * {@code WrapLayout} as a fallback.
     */
    public void layoutContainer(Container parent, boolean preferred) {
        synchronized (parent.getTreeLock()) {
            final Dimension size = parent.getSize();
            final Layout layout = determineLayout(parent, size.width, preferred);
            applyLayout(parent, size, layout);
        }
    }
    
    private void applyLayout(Container parent, Dimension size, Layout layout) {
        final Point offsetAll = determineOffsetForCentering(size, layout.dimension);
        
        final int rowsSize = layout.rows.size();
        int rowNum = 0;
        for (Row row : layout.rows) {
            final int additionalGapWidthPerChild = determineAdditionalGapWidthPerChild(size, rowsSize, rowNum, row);
            final int totalAdditionalWidthFromAllChildren = (row.children.size() > 1) ? additionalGapWidthPerChild * (row.children.size() - 1) : 0;
            final int additionalWidth = size.width - row.size.width;
            int childNum = 0;
            final int offsetRowX = (size.width - row.size.width - totalAdditionalWidthFromAllChildren) / 2;
            for (Child child : row.children) {
                final Rectangle bounds = child.bounds;
                if (additionalGapWidthPerChild > 0) {
                    if (childNum != 0) {
                        bounds.x = bounds.x + additionalGapWidthPerChild * childNum;
                    }
                }
                if (horizontalAlignment == HorizontalAlignment.CENTER) {
                    bounds.x = bounds.x + offsetRowX;
                } else if (horizontalAlignment == HorizontalAlignment.LEFT) {
                    // Ignore.
                } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
                    bounds.x = bounds.x + additionalWidth;
                } else {
                    throw new IllegalStateException("Unknown horizontalAlignment: " + horizontalAlignment);
                }
                            
                bounds.y = bounds.y + offsetAll.y;
                
                child.component.setBounds(bounds);
                childNum++;
            }
            rowNum++;
        }
    }


    private int determineAdditionalGapWidthPerChild(Dimension size, final int rowsSize, int rowNum, Row row) {
        if (row.children.size() <= 1) {
            return 0;
        }
        if (!hasAutoHorizontalGap(rowsSize, rowNum)) {
            return 0;
        }
        return Math.min(maxHorizontalGap, (size.width - row.size.width) / (row.children.size() - 1));
    }


    private boolean hasAutoHorizontalGap(final int rowsSize, int rowNum) {
        return horizontalGap == HorizontalGap.AUTO
                || horizontalGap == HorizontalGap.AUTO_TOP && rowNum == 0
                || horizontalGap == HorizontalGap.AUTO_BOTTOM && rowNum == rowsSize - 1;
    }
    
    private List<Component> getVisibleComponents(Container parent, boolean reverseComponentOrder) {
        final List<Component> components = new ArrayList<>(parent.getComponentCount());
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) {
                continue;
            }
            components.add(c);
        }
        
        if (reverseComponentOrder) {
            Collections.reverse(components);
        }
        return components;
    }
    
    private final Point determineOffsetForCentering(Dimension displaySize, Dimension contentSize) {
        return new Point((displaySize.width - contentSize.width) / 2,
                (displaySize.height - contentSize.height) / 2);
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void removeLayoutComponent(Component comp) {}
    
    private static class Layout {
        private Dimension dimension;
        private List<Row> rows;
        
        private Layout(Dimension dimension, List<Row> rows) {
            this.dimension = dimension;
            this.rows = rows;
        }
        
        int getWidestColumnWidth() {
            return rows.stream().mapToInt(r -> r.size.width).max().orElse(0);
        }
    }
    
    private static class Row {
        private Dimension size;
        private List<Child> children;
        
        public Row(Dimension size, List<Child> children) {
            this.size = size;
            this.children = children;
        }
    }
    
    private static class Child {
        private Component component;
        private Rectangle bounds;
        
        public Child(Component component, Rectangle bounds) {
            this.component = component;
            this.bounds = bounds;
        }
    }
}