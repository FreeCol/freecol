/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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

package net.sf.freecol.client.gui.option;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.ListOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.OptionMap;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.common.option.SelectOption;
import net.sf.freecol.common.option.StringOption;

import net.miginfocom.swing.MigLayout;

/**
 * This class provides visualization for an {@link OptionMap}. In order to
 * enable values to be both seen and changed.
 */
public final class OptionMapUI extends JPanel implements OptionUpdater {

    private static final Logger logger = Logger.getLogger(OptionMapUI.class.getName());

    public static final int H_GAP = 10;

    private final List<OptionUpdater> optionUpdaters = new ArrayList<OptionUpdater>();
    
    private final HashMap<String, JComponent> optionUIs;

    private final JTabbedPane tb;


    /**
     * Creates a new <code>OptionMapUI</code> for the given
     * <code>OptionMap</code>. This is the same as using
     * {@link #OptionMapUI(OptionMap, boolean)} with
     * <code>editable == true</code>.
     * 
     * @param option The <code>OptionMap</code> to make a user interface for.
     */
    public OptionMapUI(OptionMap option) {
        this(option, true);
    }

    /**
     * Creates a new <code>OptionMapUI</code> for the given
     * <code>OptionMap</code>.
     * 
     * @param option The <code>OptionMap</code> to make a user interface for.
     */
    public OptionMapUI(OptionMap option, boolean editable) {
        super(new BorderLayout());

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new MigLayout("wrap 4", "[fill]related[fill]unrelated[fill]related[fill]"));
        northPanel.setOpaque(false);
        
        optionUIs = new HashMap<String, JComponent>();

        tb = new JTabbedPane(JTabbedPane.TOP);
        tb.setOpaque(false);

        for (OptionGroup o : option.getOptionGroups()) {
            OptionGroupUI c = new OptionGroupUI((OptionGroup) o, editable, 1, optionUIs);
            c.setOpaque(true);
            optionUpdaters.add(c);
            JScrollPane scroll = new JScrollPane(c, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            c.setBorder(BorderFactory.createEmptyBorder(H_GAP - 5, H_GAP, 0,
                                                        H_GAP));
            tb.addTab(o.getName(), null, scroll, o.getShortDescription());
        }

        if (tb.getTabCount() > 0) {
            add(tb, BorderLayout.CENTER);
        } else {
            add(northPanel, BorderLayout.CENTER);
        }

       setOpaque(false);
    }

    /**
     * Rollback to the original value.
     * 
     * This method gets called so that changes made to options with
     * {@link Option#isPreviewEnabled()} is rolled back
     * when an option dialoag has been cancelled.
     */
    public void rollback() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.rollback();
        }
    }
    
    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.unregister();
        }
    }

    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.updateOption();
        }
    }
    
    public JComponent getOptionUI(String key) {
        return optionUIs.get(key);
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.reset();
        }
    }
}
