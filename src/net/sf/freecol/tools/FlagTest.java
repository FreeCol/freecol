/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.gui.panel.ConfirmDeclarationDialog;
import net.sf.freecol.client.gui.panel.ConfirmDeclarationDialog.ColorButton;
import net.sf.freecol.client.gui.panel.Flag;
import net.sf.freecol.client.gui.panel.Flag.Background;
import net.sf.freecol.client.gui.panel.Flag.Decoration;
import net.sf.freecol.client.gui.panel.Flag.UnionPosition;
import net.sf.freecol.client.gui.panel.Flag.UnionShape;


public class FlagTest extends JFrame implements ActionListener, ItemListener {


    private final Flag[] FLAGS = {
        ConfirmDeclarationDialog.ENGLISH_FLAG,
        ConfirmDeclarationDialog.SPANISH_FLAG,
        ConfirmDeclarationDialog.FRENCH_FLAG,
        ConfirmDeclarationDialog.DUTCH_FLAG,
        ConfirmDeclarationDialog.PORTUGUESE_FLAG,
        ConfirmDeclarationDialog.SWEDISH_FLAG,
        ConfirmDeclarationDialog.DANISH_FLAG,
        ConfirmDeclarationDialog.RUSSIAN_FLAG,
        null // custom
    };

    private static final String[] FLAG_NAMES = {
        "England", "Spain", "France", "Netherlands",
        "Portugal", "Sweden", "Denmark", "Russia", "Custom"
    };

    private Flag flag;

    private final JComboBox<String> flags
        = new JComboBox<>(FLAG_NAMES);
    private final JComboBox<Decoration> decoration
        = new JComboBox<>(Decoration.values());
    private final JComboBox<Background> background
        = new JComboBox<>(Background.values());
    private final JComboBox<UnionPosition> union
        = new JComboBox<>(UnionPosition.values());
    private final JComboBox<UnionShape> unionShape
        = new JComboBox<>(UnionShape.values());
    private final JComboBox<String> stars
        = new JComboBox<>(getNumbers(50));
    private final JComboBox<String> stripes
        = new JComboBox<>(getNumbers(13));

    private final ColorButton unionColor = new ColorButton(Color.BLUE);
    private final ColorButton starColor = new ColorButton(Color.WHITE);
    private final ColorButton decorationColor = new ColorButton(Color.WHITE);
    private final ColorButton[] backgroundColors = {
        new ColorButton(null), new ColorButton(null), new ColorButton(null),
        new ColorButton(null), new ColorButton(null), new ColorButton(null)
    };

    private final Component[] customComponents = {
        background, decoration, union, unionShape,
        stripes, unionColor, starColor, decorationColor,
        backgroundColors[0], backgroundColors[1],
        backgroundColors[2], backgroundColors[3],
        backgroundColors[4], backgroundColors[5]
    };

    final JLabel label = new JLabel();


    public FlagTest() {
        super("FlagTest");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new MigLayout("wrap 2", "[][fill]"));
        flags.addItemListener(this);
        add(new JLabel("predefined flags"));
        add(flags);

        background.addItemListener(this);
        add(new JLabel("background"));
        add(background);

        decoration.addItemListener(this);
        add(new JLabel("decoration"));
        add(decoration);

        union.addItemListener(this);
        add(new JLabel("union position"));
        add(union);

        unionShape.addItemListener(this);
        add(new JLabel("union shape"));
        add(unionShape);

        stars.setSelectedIndex(12);
        stars.addItemListener(this);
        add(new JLabel("number of stars"));
        add(stars);

        stripes.setSelectedIndex(12);
        stripes.addItemListener(this);
        add(new JLabel("number of stripes"));
        add(stripes);

        unionColor.addActionListener(this);
        add(new JLabel("union color"));
        add(unionColor);

        decorationColor.addActionListener(this);
        add(new JLabel("decoration color"));
        add(decorationColor);

        starColor.addActionListener(this);
        add(new JLabel("star color"));
        add(starColor);

        add(new JLabel("background colors"));
        for (int index = 0; index < backgroundColors.length; index++) {
            ColorButton button = backgroundColors[index];
            button.addActionListener(this);
            if (index == 0) {
                add(button, "split 3");
            } else if (index % 3 == 0) {
                add(button, "skip, split 3");
            } else {
                add(button);
            }
        }

        add(label, "width 200, height 100");

        itemStateChanged(null);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        Background newBackground = (Background) background.getSelectedItem();
        Decoration newDecoration = (Decoration) decoration.getSelectedItem();
        UnionPosition newPosition = (UnionPosition) union.getSelectedItem();
        UnionShape newShape = (UnionShape) unionShape.getSelectedItem();
        Flag newFlag = FLAGS[flags.getSelectedIndex()];
        if (e == null || e.getSource() == flags) {
            if (newFlag == null) {
                // custom
                enable(customComponents, true);
            } else {
                enable(customComponents, false);
                flag = newFlag;
                unionColor.setColor(flag.getUnionColor());
                starColor.setColor(flag.getStarColor());
                decorationColor.setColor(flag.getDecorationColor());
                List<Color> colors = flag.getBackgroundColors();
                for (int index = 0; index < backgroundColors.length; index++) {
                    Color color = (index < colors.size())
                        ? colors.get(index) : null;
                    backgroundColors[index].setColor(color);
                }
            }
        } else {
            if (newFlag == null) {
                if (e.getSource() == decoration) {
                    UnionPosition oldPosition = (UnionPosition) union.getSelectedItem();
                    union.removeAllItems();
                    for (UnionPosition position : newDecoration.unionPositions) {
                        union.addItem(position);
                    }
                    union.setSelectedItem(oldPosition);
                }
                flag = new Flag(newBackground, newDecoration, newPosition, newShape);
                flag.setStripes(stripes.getSelectedIndex() + 1);
                setColors();
            }
            flag.setStars(stars.getSelectedIndex() + 1);
        }
        stripes.setEnabled(newBackground == Background.PALES
                           || newBackground == Background.FESSES);

        label.setIcon(new ImageIcon(flag.getImage()));
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        ColorButton button = (ColorButton)ae.getSource();
        Color color = JColorChooser
            .showDialog(FlagTest.this,
                        label.getText(),
                        button.getBackground());
        button.setColor(color);
        setColors();
        label.setIcon(new ImageIcon(flag.getImage()));
    }

    private void setColors() {
        flag.setUnionColor(unionColor.getColor());
        flag.setStarColor(starColor.getColor());
        flag.setDecorationColor(decorationColor.getColor());
        List<Color> colors = new ArrayList<>();
        for (ColorButton button : backgroundColors) {
            Color color = button.getColor();
            if (color != null) {
                colors.add(color);
            }
            flag.setBackgroundColors(colors);
        }
    }

    public final String[] getNumbers(int count) {
        String[] result = new String[count];
        for (int index = 0; index < count; index++) {
            result[index] = Integer.toString(index + 1);
        }
        return result;
    }

    private void enable(Component[] components, boolean value) {
        for (Component component : components) {
            component.setEnabled(value);
        }
    }


    public static void main(String[] args) {
        FlagTest frame = new FlagTest();

        // display the window
        frame.pack();
        frame.setVisible(true);
    }
}
