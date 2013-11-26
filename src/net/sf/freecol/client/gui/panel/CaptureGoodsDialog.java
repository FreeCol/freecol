/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * Panel for choosing the goods to capture.
 */
public final class CaptureGoodsDialog extends FreeColDialog<List<Goods>> {

    private static final Logger logger = Logger.getLogger(CaptureGoodsDialog.class.getName());

    /** The maximum number of items to loot. */
    private final int maxCargo;

    /** The button to select all items. */
    private final JButton allButton;

    /** The button to select no items. */
    private final JButton noneButton;

    /** The list of goods to display. */
    private final JList goodsList = new JList();


    /**
     * Creates a new CaptureGoodsDialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param winner The <code>Unit</code> that is looting.
     * @param loot The <code>Goods</code> to loot.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public CaptureGoodsDialog(FreeColClient freeColClient, Unit winner,
                              List<Goods> loot) {
        super(freeColClient);

        this.maxCargo = winner.getSpaceLeft();

        String hdr = Messages.message("lootCargo.header");
        JLabel header = GUI.getDefaultHeader(hdr);
        header.setFont(GUI.MEDIUM_HEADER_FONT);

        String all = Messages.message("All");
        allButton = new JButton(all);
        allButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < goodsList.getModel().getSize()
                             && i < maxCargo; i++) {
                        GoodsItem gi = (GoodsItem)goodsList.getModel()
                            .getElementAt(i);
                        gi.setSelected(true);
                        updateComponents();
                    }
                }
            });
        allButton.setMnemonic('a');
        allButton.setActionCommand(all);
 
        String none = Messages.message("None");
        noneButton = new JButton(none);
        noneButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                        GoodsItem gi = (GoodsItem)goodsList.getModel()
                            .getElementAt(i);
                        gi.setSelected(false);
                        updateComponents();
                    }
                }
            });
        noneButton.setMnemonic('n');
        noneButton.setActionCommand(none);

        GoodsItem[] goods = new GoodsItem[loot.size()];
        for (int i = 0; i < loot.size(); i++) {
            goods[i] = new GoodsItem(loot.get(i));
        }
        goodsList.setListData(goods);
        goodsList.setCellRenderer(new CheckBoxRenderer());
        goodsList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent me) {
                    int index = goodsList.locationToIndex(me.getPoint());
                    if (index < 0) return;
                    GoodsItem item = (GoodsItem)goodsList.getModel()
                        .getElementAt(index);
                    if (item.isEnabled()) item.setSelected(!item.isSelected());
                    updateComponents();
                }
            });

        MigPanel panel = new MigPanel(new MigLayout("wrap 1", "[center]",
                                                    "[]20[]20[]"));
        panel.add(header);
        panel.add(allButton, "split 2");
        panel.add(noneButton);
        panel.add(goodsList);
        panel.setSize(panel.getPreferredSize());

        List<Goods> fake = null;
        List<ChoiceItem<List<Goods>>> c = choices();
        c.add(new ChoiceItem<List<Goods>>(Messages.message("ok"), fake)
            .okOption().defaultOption());
        initialize(DialogType.QUESTION, false, panel,
            getGUI().getImageLibrary().getImageIcon(winner, false), c);
    }


    /**
     * Update the components of the goods list.
     */
    private void updateComponents() {
        int selectedCount = 0;
        for (int i = 0; i < goodsList.getModel().getSize(); i++) {
            GoodsItem gi = (GoodsItem)goodsList.getModel().getElementAt(i);
            if (gi.isSelected()) selectedCount++;
        }

        if (selectedCount >= maxCargo) {
            allButton.setEnabled(false);
            for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                GoodsItem gi = (GoodsItem)goodsList.getModel().getElementAt(i);
                if (!gi.isSelected()) gi.setEnabled(false);
            }
        } else {
            allButton.setEnabled(true);
            for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                GoodsItem gi = (GoodsItem)goodsList.getModel().getElementAt(i);
                if (!gi.isSelected()) gi.setEnabled(true);
            }
        }

        goodsList.repaint();
    }

    /**
     * {@inheritDoc}
     */
    public List<Goods> getResponse() {
        Object value = getValue();
        List<Goods> gl = new ArrayList<Goods>();
        if (options.get(0).equals(value)) {
            for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                GoodsItem gi = (GoodsItem)goodsList.getModel().getElementAt(i);
                if (gi.isSelected()) gl.add(gi.getGoods());
            }
        }
        return gl;
    }            


    private class CheckBoxRenderer extends JCheckBox
        implements ListCellRenderer {

        public CheckBoxRenderer() {
            //setBackground(UIManager.getColor("List.textBackground"));
            //setForeground(UIManager.getColor("List.textForeground"));
        }

        public Component getListCellRendererComponent(JList listBox,
            Object obj, int currentindex, boolean isChecked,
            boolean hasFocus) {
            GoodsItem item = (GoodsItem) obj;
            setSelected(item.isSelected());
            setText(item.toString());
            setEnabled(item.isEnabled());
            return this;
        }
    }

    private class GoodsItem extends JCheckBox {
        private Goods good;

        public GoodsItem(Goods good) {
            this.good = good;
        }

        public Goods getGoods() {
            return good;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringTemplate template
                = StringTemplate.template("model.goods.goodsAmount")
                    .add("%goods%", good.getNameKey())
                    .addAmount("%amount%", good.getAmount());
            return Messages.message(template);
        }
    }
}
