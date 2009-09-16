/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   14.07.2009 (mb): created
 */
package org.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;



/** Button for a {@link FlowVariableModel}, launching a dialog which allows to
 * control the settings.
 *
 * This allows NodeDialogPane implementations to easily use Variables
 * for individual options.
 * 
 * @author Michael Berthold, University of Konstanz
 */
@SuppressWarnings("serial")
public class FlowVariableModelButton extends JButton
implements ChangeListener, ActionListener {

    /* remember underlying model (to track changes) */
    private final FlowVariableModel m_model;

    /**
     * Create new button based on a model.
     * @param fvm the underlying model
     * @throws NullPointerException If argument is null.
     */
    public FlowVariableModelButton(
            final FlowVariableModel fvm) {
        if (fvm == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_model = fvm;
        // add us as listener for changes to the WorkflowVariableModel
        fvm.addChangeListener(this);
        // add us as listener for actions on the underlying JButton
        this.addActionListener(this);
        // and make sure we start with the right button layout.
        stateChanged(null);
    }

    /** React to state changes in the underlying WorkflowVariableModel
     * and set tool tip accordingly.
     * 
     * @param evt event
     */
    @Override
    public void stateChanged(final ChangeEvent evt) {
        boolean enabled = m_model.isVariableReplacementEnabled();
        try {
            // try to load icon(s)
            ImageIcon icon;
            ClassLoader loader = this.getClass().getClassLoader(); 
            String packagePath = 
                this.getClass().getPackage().getName().replace('.', '/');
            String correctedPath = "/icon/"
                + (enabled ? "varbuttonON.png"
                            : "varbuttonOFF.png");
            icon = new ImageIcon(
                    loader.getResource(packagePath + correctedPath));
            this.setText("");
            this.setBorder(new LineBorder(Color.gray, 0));
            if (Boolean.getBoolean(KNIMEConstants.PROPERTY_EXPERT_MODE)) {
                this.setIcon(icon);                
            } else {
                // if in non expert mode hide icon by make it really small
                this.setIcon(new ImageIcon(icon.getImage()
                        .getScaledInstance(1, 1, Image.SCALE_DEFAULT)));
            }
        } catch (Exception e) {
            this.setText(enabled ? "v!" : "v?");
            return;
        }
        // compose tooltip with current var names (if available)
        StringBuffer tooltip = new StringBuffer("Flow variable: ");
        if (enabled) {
            tooltip.append("replacing with '" + m_model.getInputVariableName()
                    + "'");
        } else {
            tooltip.append("<no variable replacement>");
        }
        tooltip.append(", ");
        if (m_model.getOutputVariableName() != null) {
            tooltip.append("export as '"
                    + m_model.getOutputVariableName() + "'");
        } else {
            tooltip.append("<no export as variable>");
        }
        this.setToolTipText(tooltip.toString());
    }
    
    /** @return the model as passed in 
     * {@linkplain #FlowVariableModelButton(FlowVariableModel) constructor}.
     */
    public FlowVariableModel getFlowVariableModel() {
        return m_model;
    }

    /** React to clicks on the underlying button: open dialog which enables
     * the user to change the underlying settings.
     * 
     * @param e event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        // make sure dialog is modal with respect to the "nearest" frame
        Container c = SwingUtilities.getAncestorOfClass(Frame.class, this);
        Frame parentFrame = (Frame)c;
        FlowVarEditDialog ved = new FlowVarEditDialog(parentFrame);

        if (m_model.getInputVariableName() != null) {
            ved.setInputVariableName(m_model.getInputVariableName());
        }
        if (m_model.getOutputVariableName() != null) {
            ved.setOutputVariableName(m_model.getOutputVariableName());
        }
        ved.setLocationRelativeTo(this);
        ved.setVisible(true);
    }

    private class FlowVarEditDialog extends JDialog {
        
        FlowVarEditDialog(final Frame f) {
            // set title and make dialog modal
            super(f, "Variable Settings", true);
            // set icon of dialog frame
            ClassLoader loader = this.getClass().getClassLoader(); 
            String packagePath = 
                this.getClass().getPackage().getName().replace('.', '/');
            String correctedPath = "/icon/variable_dialog_active.png";
            ImageIcon icon = new ImageIcon(
                    loader.getResource(packagePath + correctedPath));
            this.setIconImage(icon.getImage());
            // finalize setup
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            initComponents();
            pack();
        }
        
        private JCheckBox m_enableInputVar;
        private JComboBox m_inputVar;
        private JCheckBox m_enableOutputVar;
        private JTextField m_outputVar;
        private JButton m_cancel;
        private JButton m_ok;
        
        private void initComponents() {
            Container cont = this.getContentPane();
            cont.setLayout(new BorderLayout());
            JPanel cp = new JPanel();
            cont.add(cp, BorderLayout.NORTH);
            cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
            cp.setAlignmentY(TOP_ALIGNMENT);
            // top part to use variable for specific settings
            JPanel panelTop = new JPanel();
            panelTop.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelTop.setBorder(new TitledBorder("Use Variable:"));
            panelTop.setLayout(new GridLayout(1, 2));
            m_enableInputVar = new JCheckBox();
            m_enableInputVar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent evt) {
                    m_inputVar.setEnabled(m_enableInputVar.isSelected());
                }
            });
            panelTop.add(m_enableInputVar);
            FlowVariable[] matchingVars = m_model.getMatchingVariables();
            if (matchingVars.length > 0) {
                m_inputVar = new JComboBox(matchingVars);
                m_inputVar.setRenderer(new FlowVariableListCellRenderer());
            } else {
                m_inputVar = new JComboBox(new String[] {"<no matching vars>"});
                m_inputVar.setEnabled(false);
            }
            m_inputVar.setEnabled(false);
            panelTop.add(m_inputVar);
            cp.add(panelTop);
            // middle part to create new variable based on specific settings
            JPanel panelMiddle = new JPanel();
            panelMiddle.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelMiddle.setBorder(new TitledBorder("Create Variable:"));
            panelMiddle.setLayout(new GridLayout(1, 2));
            m_enableOutputVar = new JCheckBox();
            m_enableOutputVar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent evt) {
                    m_outputVar.setEnabled(m_enableOutputVar.isSelected());
                }
            });
            panelMiddle.add(m_enableOutputVar);
            m_outputVar = new JTextField();
            m_outputVar.setEnabled(false);
            panelMiddle.add(m_outputVar);
            cp.add(panelMiddle);
            // pane for buttons
            JPanel panelBottom = new JPanel();
            panelBottom.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.X_AXIS));
            panelBottom.add(Box.createHorizontalGlue());
            m_ok = new JButton("OK");
            m_ok.setAlignmentY(Component.BOTTOM_ALIGNMENT);
            m_ok.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    // write values back to model
                    if (m_enableInputVar.isSelected()) {
                        m_model.setInputVariableName(
                       ((FlowVariable)m_inputVar.getSelectedItem()).getName());
                    } else {
                        m_model.setInputVariableName(null);
                    }
                    if (m_enableOutputVar.isSelected()) {
                        m_model.setOutputVariableName(
                                m_outputVar.getText());
                    } else {
                        m_model.setOutputVariableName(null);
                    }
                    setVisible(false);
                }
            });
            panelBottom.add(m_ok);
            panelBottom.add(Box.createRigidArea(new Dimension(10, 0)));
            m_cancel = new JButton("Cancel");
            m_cancel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
            m_cancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    // do nothing here!
                    setVisible(false);
                }
            });
            panelBottom.add(m_cancel);
            cp.add(Box.createRigidArea(new Dimension(0, 5)));
            cp.add(panelBottom);
            cp.add(Box.createVerticalGlue());
        }
        
        void setInputVariableName(final String s) {
            if (s == null) {
                m_inputVar.setEnabled(false);
                m_enableInputVar.setSelected(false);
                m_enableInputVar.setEnabled(true);
                if (m_model.getMatchingVariables().length == 0) {
                    // we have no matching variables
                    m_enableInputVar.setEnabled(false);
                    m_inputVar = new JComboBox(
                            new String[] {"<no matching vars>"});
                }
                return;
            }
            assert s != null;
            // try to find variable with corresponding name (and type):
            FlowVariable var = null;
            for (FlowVariable v : m_model.getMatchingVariables()) {
                if (v.getName().equals(s)) {
                    var = v;
                    break;
                }
            }
            if (var != null) {
                // found it: we can select it and enable button+combobox.
                m_inputVar.setSelectedItem(var);
                m_inputVar.setEnabled(true);
                m_enableInputVar.setSelected(true);
                m_enableInputVar.setEnabled(true);
            } else {
                // could not find it - two options:
                if (m_model.getMatchingVariables().length == 0) {
                    // 1) we have no matching variables
                    // ComboBox already has a fake entry, nothing needs to
                    // change - but to be sure:
                    m_inputVar = new JComboBox(
                            new String[] {"<no matching vars>"});
                    m_enableInputVar.setSelected(false);
                    m_enableInputVar.setEnabled(false);
                    m_inputVar.setEnabled(false);
                } else {
                    // 2) we do have other variables that match:
                    // add a non-selectable entry to
                    // the list so the user knows what's wrong. KNIME will
                    // complain during configure anyway and let her know
                    // that the variable does not exist (anymore).
                    m_inputVar.addItem(s);
                    m_inputVar.setRenderer(new CustomListCellRenderer(s));
                    m_inputVar.setSelectedItem(s);
                    m_inputVar.addActionListener(new ActionListener() {
                        private Object m_oldSelection = s;
                        public void actionPerformed(final ActionEvent evt) {
                            Object o = m_inputVar.getSelectedItem();
                            if (o.equals(s)) {
                                if (!o.equals(m_oldSelection)) {
                                    // only select the previous (hopefully
                                    // legal) selection if it was different!
                                    m_inputVar.setSelectedItem(m_oldSelection);
                                }
                            } else {
                                m_oldSelection = o;
                            }
                        }
                    });
                    m_enableInputVar.setSelected(true);
                    m_inputVar.setEnabled(true);
                }
            }
        }

        void setOutputVariableName(final String s) {
            if (s != null) {
                m_enableOutputVar.setSelected(true);
                m_outputVar.setEnabled(true);
                m_outputVar.setText(s);
            } else {
                m_enableOutputVar.setSelected(false);
                m_outputVar.setEnabled(false);
                m_outputVar.setText("");
            }
        }
        
    }

    /** Helper class to allow also the display of disabled list elements. */
    class CustomListCellRenderer extends FlowVariableListCellRenderer {
        private String m_toDisable;
        
        /** Create new render which disables given string.
         * 
         * @param s string to disable
         */
        CustomListCellRenderer(final String s) {
            m_toDisable = s;
        }

        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            Component comp = super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);
            if (value.toString().equals(m_toDisable)) {
                comp.setFocusable(false);
                comp.setEnabled(false);
                comp.setBackground(Color.red);
            }
            return comp;
        }
    }
}
