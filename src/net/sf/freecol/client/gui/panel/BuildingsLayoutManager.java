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
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.JViewport;

import net.sf.freecol.client.gui.panel.ColonyPanel.BuildingsPanel;
import net.sf.freecol.client.gui.panel.ColonyPanel.BuildingsPanel.EmptyBuildingSite;

/**
 * A specific layout manager for {@code BuildingsPanel}.
 * 
 * This layout manager tries to lay out all the buildings in a randomized
 * pattern using the preferred size of the building (that is, the maximum
 * required size of the building for all levels).
 * 
 * A {@link WrapLayout} with the minimum building size is used as a
 * fallback if this layout manager fails to layout all the buildings
 * within the container's current size. 
 * 
 * @see BuildingsPanel
 */
public class BuildingsLayoutManager implements LayoutManager {

    /**
     * A seed that is used when laying out the component. The
     * same layout will be reached if no sizes (buildings or the
     * container) have been changed.
     */
    private long layoutSeed = 1;
    
    /**
     * Fallback rendering if there's insufficent room for rendering
     * all the buildings.
     */
    private WrapLayout wrapLayout = new WrapLayout();
    
    
    /**
     * Creates a new layout manager with a standard seed.
     */
    public BuildingsLayoutManager() {
        this(1);
    }
    
    /**
     * Creates a new layout manager.
     * 
     * @param layoutSeed A seed that is used when laying out the component. The
     *      same layout will be reached if no sizes (buildings or the container)
     *      have been changed.
     */
    public BuildingsLayoutManager(long layoutSeed) {
        this.layoutSeed = layoutSeed;
    }

    
    /**
     * Returns the preferred dimensions for this layout given the <i>visible</i>
     * components in the specified target container.
     * 
     * @param parent the component which needs to be laid out
     * @return the preferred dimensions to lay out the subcomponents of the
     *         specified container
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        final Dimension size = determineInitialSize(parent);

        if (randomizedPlacement(parent, size, true)) {
            // Randomized placement succeeded.
            return size;
        }
        
        // Randomized placement failed. Falling back to WrapLayout.
        setAllEmptyBuildingSiteVisibility(parent, false);
        return wrapLayout.layoutSize(parent, size.width, false);
    }
    
    private Dimension determineInitialSize(Container parent) {
        if (parent.getParent() != null && parent.getParent() instanceof JViewport) {                
            return parent.getParent().getSize();
        }
        return new Dimension(0, 0);
    }

    /**
     * Just returns a minimum size. The initial size is determined by the
     * parent of the given parent.
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(1, 1);
    }

    /**
     * Layout of all children of the given {@code parent}
     * 
     * @param parent The {@code Container} that should get its children
     *      positioned.
     */
    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {           
            Dimension size = parent.getSize();
            boolean skipRandomizedPlacement = false;
            if (parent.getParent() != null && parent.getParent() instanceof JViewport) {                
                final Dimension newSize = parent.getParent().getSize();
                if (!size.equals(newSize)) {
                    skipRandomizedPlacement = true;
                }
            }
            
            if (!skipRandomizedPlacement) {
                setAllEmptyBuildingSiteVisibility(parent, true);
                if (randomizedPlacement(parent, size, false)) {
                    return;
                }
            }
            
            // Randomized placement failed. Falling back to WrapLayout.
            setAllEmptyBuildingSiteVisibility(parent, false);            
            wrapLayout.layoutContainer(parent, false);
        }
    }
    
    private void setAllEmptyBuildingSiteVisibility(Container parent, boolean visible) {
        for (Component c : parent.getComponents()) {
            if (c instanceof EmptyBuildingSite) {
                c.setVisible(visible);
            }
        }
    }
    
    private boolean randomizedPlacement(Container parent, Dimension size, boolean dryRun) {
        final Random r = new Random(layoutSeed);
        
        final List<Entry> entries = getAllEntries(parent, size);
        if (isDefinitelyNotEnoughRoomForLayout(entries, size)) {
            return false;
        }
        sortWithLargestFirst(entries);
        
        final int MAX_TOTAL_TRIES = 24;
        final int MAX_FREE_PLACEMENT = 6;
        final int MAX_PLACE_ENTRY = 10000; 
        
        int padding = 16;
        for (int j=0; j<MAX_TOTAL_TRIES; j++) {
            final List<Rectangle> usedRectangles = new ArrayList<>(entries.size());
            final List<Entry> placeEntries; 
            
            if (j > MAX_FREE_PLACEMENT) {
                placeEntries = placeFirstFourEntriesInTheCorners(size, entries, usedRectangles);
            } else {
                placeEntries = entries;
            }
            
            int entriesRemaining = placeEntries.size();
            for (Entry entry : placeEntries) {
                boolean placed = false;
                for (int i=0; i<(MAX_PLACE_ENTRY / (entriesRemaining * entriesRemaining)); i++) {
                    final int x = r.nextInt(size.width - entry.bounds.width);
                    final int y = r.nextInt(size.height - entry.bounds.height);
                    final Rectangle bounds = new Rectangle(x, y, entry.bounds.width, entry.bounds.height);
                    if (!overlaps(bounds, usedRectangles)) {
                        usedRectangles.add(new Rectangle(bounds.x - padding/2, bounds.y - padding/2, bounds.width + padding, bounds.height + padding));
                        entry.bounds = bounds;
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    break;
                }
                entriesRemaining--;
            }

            if (entriesRemaining == 0) {
                if (!dryRun) {
                    for (Entry entry : entries) {
                        entry.component.setBounds(entry.bounds);
                    }
                }
                
                // We're done!
                return true;
            }
            
            // Reduce padding and try again.
            padding /= 2;
            if (padding < 0) {
                padding = 0;
            }
        }

        return false;
    }

    private List<Entry> placeFirstFourEntriesInTheCorners(Dimension size, List<Entry> entries, List<Rectangle> usedRectangles) {
        final Entry e0 = entries.get(0);
        final Entry e1 = entries.get(1);
        final Entry e2 = entries.get(2);
        final Entry e3 = entries.get(3);
        e0.bounds = new Rectangle(0, 0, e0.bounds.width, e0.bounds.height);
        usedRectangles.add(e0.bounds);
        e1.bounds = new Rectangle(0, size.height - e1.bounds.height, e1.bounds.width, e1.bounds.height);
        usedRectangles.add(e1.bounds);
        e2.bounds = new Rectangle(size.width - e1.bounds.width, 0, e2.bounds.width, e2.bounds.height);
        usedRectangles.add(e2.bounds);
        e3.bounds = new Rectangle(size.width - e3.bounds.width, size.height - e3.bounds.height, e3.bounds.width, e3.bounds.height);
        usedRectangles.add(e3.bounds);
        
        return entries.subList(4, entries.size());
    }

    private boolean isDefinitelyNotEnoughRoomForLayout(List<Entry> entries, Dimension size) {
        if (entries.isEmpty()) {
            return false;
        }
        final long minimumArea = entries.stream()
                .map(BuildingsLayoutManager::areaOf)
                .reduce((a, b) -> a + b).get();
        return minimumArea > ((long) size.width) * size.height;
    }
    
    /**
     * Finds all components that are children to the given parent.
     * 
     * @return The list of children (called entries).
     */
    private List<Entry> getAllEntries(Container parent, Dimension size) {
        final List<Entry> entries = new ArrayList<>(parent.getComponentCount());
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) {
                continue;
            }
            final Dimension componentSize = c.getPreferredSize();
            entries.add(new Entry(c, new Rectangle(0, 0, componentSize.width, componentSize.height)));
        }
        return entries;
    }

    private void sortWithLargestFirst(final List<Entry> entries) {
        Collections.sort(entries, (a, b) -> Long.compare(areaOf(b), areaOf(a)));
    }

    private static long areaOf(Entry a) {
        return ((long) a.bounds.width) * a.bounds.height;
    }
    
    private boolean overlaps(Rectangle rectangle, List<Rectangle> list) {
        for (Rectangle r : list) {
            if (rectangle.intersects(r)) {
                return true;
            }
        }
        return false;
    }
    
    final Point determineOffsetForCentering(Dimension displaySize, Dimension contentSize) {
        return new Point((displaySize.width - contentSize.width) / 2,
                (displaySize.height - contentSize.height) / 2);
    }
    
    final Rectangle reposition(Rectangle bounds, Point offset) {
        return new Rectangle(bounds.x + offset.x, bounds.y + offset.y, bounds.width, bounds.height);
    }
    
    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void removeLayoutComponent(Component comp) {}
    
    private static class Entry {
        private Component component;
        private Rectangle bounds;
        
        public Entry(Component component, Rectangle bounds) {
            this.component = component;
            this.bounds = bounds;
        }
    }
}
