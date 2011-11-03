/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;

/**
 * This class provides visualization for an {@link
 * net.sf.freecol.common.option.OptionGroup}. In order to enable values
 * to be both seen and changed.
 */
public final class OptionGroupUI extends JPanel implements OptionUpdater {

    private static final Logger logger = Logger.getLogger(OptionGroupUI.class.getName());

    public static final int H_GAP = 10;

    private final List<OptionUpdater> optionUpdaters = new ArrayList<OptionUpdater>();

    private final HashMap<String, OptionUI> optionUIs;

    private final JTabbedPane tb;

    private GUI gui;

    private FreeColClient freeColClient;


    /**
     * Creates a new <code>OptionGroupUI</code> for the given
     * <code>OptionGroup</code>. This is the same as using
     * {@link #OptionGroupUI(OptionGroup, boolean)} with
     * <code>editable == true</code>.
     *
     * @param option The <code>OptionGroup</code> to make a user interface for.
     */
    public OptionGroupUI(FreeColClient freeColClient, GUI gui, OptionGroup option) {
        this(freeColClient, gui, option, true);
    }

    /**
     * Creates a new <code>OptionGroupUI</code> for the given
     * <code>OptionGroup</code>.
     *
     * @param option The <code>OptionGroup</code> to make a user interface for
     * @param editable boolean whether user can modify the setting
     */
    public OptionGroupUI(FreeColClient freeColClient, GUI gui, OptionGroup option, boolean editable) {
        super(new BorderLayout());
        this.gui = gui;
        this.freeColClient = freeColClient;

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new MigLayout("wrap 4", "[fill]related[fill]unrelated[fill]related[fill]"));
        northPanel.setOpaque(false);

        optionUIs = new HashMap<String, OptionUI>();

        tb = new JTabbedPane(JTabbedPane.TOP);
        tb.setOpaque(false);

        Iterator<Option> it = option.iterator();
        while (it.hasNext()) {
            Option o = it.next();

            if (o instanceof OptionGroup) {
                OptionGroup group = (OptionGroup) o;
                JPanel groupPanel = new JPanel();
                groupPanel.setLayout(new MigLayout("wrap 4", "[fill]related[fill]unrelated[fill]related[fill]"));
                groupPanel.setOpaque(true);
                addOptionGroupUI(group, groupPanel, editable);
                JScrollPane scroll = new JScrollPane(groupPanel,
                                                     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                scroll.getVerticalScrollBar().setUnitIncrement(16);
                scroll.setBorder(BorderFactory.createEmptyBorder());
                groupPanel.setBorder(BorderFactory.createEmptyBorder(H_GAP - 5, H_GAP, 0, H_GAP));
                tb.addTab(Messages.getName((Option) group), null, scroll,
                          Messages.getShortDescription((Option) group));
            } else {
                addOptionUI(o, northPanel, editable);
            }
        }
        if (tb.getTabCount() > 0) {
            /** ignore options that do not belong to an OptionGroup, e.g. window sizes and positions
            if (northPanel.getComponentCount() > 0) {
                tb.addTab(" *** ", northPanel);
            }
            */
            add(tb, BorderLayout.CENTER);
        } else {
            add(northPanel, BorderLayout.CENTER);
        }

        setOpaque(false);
    }


    private void addOptionGroupUI(OptionGroup group, JPanel panel, boolean editable) {
        Iterator<Option> iterator = group.iterator();
        while (iterator.hasNext()) {
            Option o = iterator.next();
            if (o instanceof OptionGroup) {
                OptionGroup subgroup = (OptionGroup) o;
                panel.add(new JLabel(Messages.getName((Option) subgroup)),
                          "newline 20, span, split 2");
                panel.add(new JSeparator(), "growx");
                addOptionGroupUI(subgroup, panel, editable);
            } else {
                addOptionUI(o, panel, editable);
            }
        }
    }

    private void addOptionUI(Option option, JPanel panel, boolean editable) {
        OptionUI ui = OptionUI.getOptionUI(freeColClient, gui, option, editable);
        if (ui == null) {
            logger.warning("Unknown option type: " + option.toString());
        } else if (ui instanceof FreeColActionUI) {
            ((FreeColActionUI) ui).setOptionGroupUI(this);
        }
        JLabel label = ui.getLabel();
        if (label == null) {
            panel.add(ui.getComponent(), "newline, span");
        } else {
            if (label.getText().length() > 30) {
                panel.add(label, "newline, span 3");
            } else {
                panel.add(label);
            }
            panel.add(ui.getComponent());
        }
        if (editable) {
            optionUpdaters.add((OptionUpdater) ui);
        }
        if (!option.getId().equals(FreeColObject.NO_ID)) {
            optionUIs.put(option.getId(), ui);
        }
    }


    /**
     * Updates the value of the {@link net.sf.freecol.common.option.Option} this object keeps.
     */
    public void updateOption() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.updateOption();
        }
    }

    public OptionUI getOptionUI(String key) {
        return optionUIs.get(key);
    }

    /**
     * Removes the given <code>KeyStroke</code> from all of this
     * <code>OptionGroupUI</code>'s children.
     *
     * @param keyStroke The <code>KeyStroke</code> to be removed.
     */
    public void removeKeyStroke(KeyStroke keyStroke) {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            if (optionUpdater instanceof FreeColActionUI) {
                ((FreeColActionUI) optionUpdater).removeKeyStroke(keyStroke);
            }
        }
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.reset();
        }
    }

    @Override
    public String getUIClassID() {
        return "ReportPanelUI";
    }
}
