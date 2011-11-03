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
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * Panel for choosing the goods to capture.
 */
public final class CaptureGoodsDialog extends FreeColDialog<List<Goods>>
    implements ActionListener {

    private static final Logger logger = Logger.getLogger(CaptureGoodsDialog.class.getName());

    static final String ALL = "All";
    static final String NONE = "None";

    private JButton allButton;
    private JButton noneButton;

    private JList goodsList;
    private int maxCargo;

    /**
     * Creates a new CaptureGoodsDialog.
     * @param freeColClient 
     *
     * @param parent The parent <code>Canvas</code>.
     * @param winner The <code>Unit</code> that is looting.
     * @param loot The <code>Goods</code> to loot.
     */
    public CaptureGoodsDialog(FreeColClient freeColClient, Canvas parent, Unit winner, List<Goods> loot) {
        super(freeColClient, parent);
        
        maxCargo = winner.getSpaceLeft();

        setLayout(new MigLayout("wrap 1", "[center]", "[]20[]20[]"));

        JLabel header = new JLabel(Messages.message("lootCargo.header"));
        header.setFont(mediumHeaderFont);
        add(header);

        goodsList = new JList();
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
                    GoodsItem item = (GoodsItem) goodsList.getModel().getElementAt(index);
                    if (item.isEnabled()) item.setSelected(!item.isSelected());
                    updateComponents();
                }
            });
        add(goodsList);

        allButton = new JButton(Messages.message(ALL));
        allButton.addActionListener(this);
        enterPressesWhenFocused(allButton);
        allButton.setMnemonic('a');
        allButton.setActionCommand(ALL);

        noneButton = new JButton(Messages.message(NONE));
        noneButton.addActionListener(this);
        enterPressesWhenFocused(noneButton);
        noneButton.setMnemonic('n');
        noneButton.setActionCommand(NONE);

        add(allButton, "span, split 3");
        add(noneButton, "tag cancel");
        add(okButton, "tag ok");

        setSize(getPreferredSize());
    }

    private void updateComponents() {
        int selectedCount = 0;
        for (int i = 0; i < goodsList.getModel().getSize(); i++) {
            GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
            if (gi.isSelected()) selectedCount++;
        }

        if (selectedCount >= maxCargo) {
            allButton.setEnabled(false);
            for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
                if (!gi.isSelected()) gi.setEnabled(false);
            }
        } else {
            allButton.setEnabled(true);
            for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
                if (!gi.isSelected()) gi.setEnabled(true);
            }
        }

        goodsList.repaint();
    }

    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests. The response is an ArrayList of Goods.
     *
     * @param e The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (ALL.equals(command)) {
            for (int i = 0; i < goodsList.getModel().getSize() && i < maxCargo;
                 i++) {
                GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
                gi.setSelected(true);
                updateComponents();
            }
        } else if (NONE.equals(command)) {
            for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
                gi.setSelected(false);
                updateComponents();
            }
        } else if (OK.equals(command)) {
            ArrayList<Goods> list = new ArrayList<Goods>();
            for (int i = 0; i < goodsList.getModel().getSize(); i++) {
                GoodsItem gi = (GoodsItem) goodsList.getModel().getElementAt(i);
                if (gi.isSelected()) list.add(gi.getGoods());
            }
            setResponse(list);
        } else {
            logger.warning("Invalid action command: " + command);
        }
    }

    private class CheckBoxRenderer extends JCheckBox
        implements ListCellRenderer {

        public CheckBoxRenderer() {
            //setBackground(UIManager.getColor("List.textBackground"));
            //setForeground(UIManager.getColor("List.textForeground"));
        }

        public Component getListCellRendererComponent(JList listBox, Object obj,
                                                      int currentindex,
                                                      boolean isChecked,
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
