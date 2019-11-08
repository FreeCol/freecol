/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.event.EventListenerList;


/**
 * The blinking cursor on the map.
 */
public class TerrainCursor implements ActionListener  {

    public static final int OFF = 0;
    public static final int ON = 1;
    private static final int blinkDelay = 500; // Milliseconds

    /** Is the cursor active? */
    private boolean active;

    /** A timer for the blinking action. */
    private final Timer blinkTimer;

    /** The listeners watching for a blink. */
    private final EventListenerList listenerList = new EventListenerList();


    /**
     * Creates a new {@code TerrainCursor} instance.
     */
    public TerrainCursor() {
        this.active = true;
        this.blinkTimer = new Timer(blinkDelay, this);
    }

    /**
     * Is this TerrainCursor active?
     *
     * @return True if the cursor is active.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Sets the active state of the TerrainCursor.
     *
     * @param newState a {@code boolean} value
     */
    public void setActive(boolean newState) {
        this.active = newState;
    }

    public void startBlinking() {
        if (!this.blinkTimer.isRunning()) this.blinkTimer.start();
    }

    public void stopBlinking() {
        if (this.blinkTimer.isRunning()) this.blinkTimer.stop();
    }

    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        this.active = !this.active;
        int eventId = (this.active) ? ON : OFF;
        ActionEvent blinkEvent = new ActionEvent(this, eventId, "blink");
        for (ActionListener al : listenerList
                 .getListeners(ActionListener.class)) {
            al.actionPerformed(blinkEvent);
        }
    }
}
