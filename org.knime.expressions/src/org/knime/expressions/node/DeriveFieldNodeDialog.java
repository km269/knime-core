/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   31.12.2017 (Moritz): created
 */
package org.knime.expressions.node;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class DeriveFieldNodeDialog extends NodeDialogPane {

	private final JTable m_table;
	private DefaultTableModel m_tableModel;

	private final String[] m_columnNames = new String[] { "Output Column", "Expression" };

	private final JButton m_addButton;
	private final JButton m_editButton;
	private final JButton m_removeButton;
	private final JButton m_removeAllButton;
	private final JButton m_moveUpButton;
	private final JButton m_moveDownButton;
	private final JButton m_copyButton;

	/* Listener to disable and enable buttons according to empty table. */
	private final TableModelListener m_tableListener = new TableModelListener() {

		@Override
		public void tableChanged(TableModelEvent e) {
			m_editButton.setEnabled(true);
			m_removeAllButton.setEnabled(true);
			m_removeButton.setEnabled(true);
			m_copyButton.setEnabled(true);
			m_moveDownButton.setEnabled(true);
			m_moveUpButton.setEnabled(true);

			if (m_tableModel.getRowCount() == 0) {
				m_editButton.setEnabled(false);
				m_moveDownButton.setEnabled(false);
				m_moveUpButton.setEnabled(false);
				m_removeAllButton.setEnabled(false);
				m_removeButton.setEnabled(false);
				m_copyButton.setEnabled(false);
			} else if (m_tableModel.getRowCount() == 1) {
				m_moveDownButton.setEnabled(false);
				m_moveUpButton.setEnabled(false);
			} else if (m_table.getSelectedRow() == 0) {
				m_moveUpButton.setEnabled(false);
			} else if (m_table.getSelectedRow() == m_table.getRowCount() - 1) {
				m_moveDownButton.setEnabled(false);
			}
		}
	};
	
	/* ActionListener used for the buttons. */
	private final ActionListener m_buttonListener = new ActionListener() {
		private int m_rowCounter = 0;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(m_addButton)) {
				/* Add row at end and select it. */
				m_tableModel.addRow(new String[] { "Col" + m_rowCounter++, "0" });
				m_table.getSelectionModel().setSelectionInterval(m_table.getRowCount() - 1,
						m_table.getRowCount() - 1);
			} else if (e.getSource().equals(m_editButton)) {
				/* TODO: open dialog */
			} else if (e.getSource().equals(m_copyButton)) {
				/* Copy selected row and insert it after the selected one. */
				m_tableModel.getValueAt(m_table.getSelectedRow(), 0);
				Object[] row = new Object[2];
				row[0] = m_tableModel.getValueAt(m_table.getSelectedRow(), 0);
				row[1] = m_tableModel.getValueAt(m_table.getSelectedRow(), 1);

				m_tableModel.insertRow(m_table.getSelectedRow() + 1, row);
			} else if (e.getSource().equals(m_removeButton)) {
				/* Remove the selected row and select the previous row. */
				int index = m_table.getSelectedRow();
				m_tableModel.removeRow(index);

				index = index - 1 < 0 ? 0 : index - 1;
				m_table.getSelectionModel().setSelectionInterval(index, index);
			} else if (e.getSource().equals(m_removeAllButton)) {
				/* Remove all rows by simply creating a new table model. */
				m_tableModel = new DefaultTableModel();
				m_tableModel.setColumnIdentifiers(m_columnNames);
				m_table.setModel(m_tableModel);

				m_tableModel.addTableModelListener(m_tableListener);
				m_tableListener.tableChanged(null);

				m_rowCounter = 0;
			} else if (e.getSource().equals(m_moveUpButton)) {
				/* Move the selected row up. */
				m_tableModel.moveRow(m_table.getSelectedRow(), m_table.getSelectedRow(),
						m_table.getSelectedRow() - 1);
				m_table.getSelectionModel().setSelectionInterval(m_table.getSelectedRow() - 1,
						m_table.getSelectedRow() - 1);
			} else if (e.getSource().equals(m_moveDownButton)) {
				/* Move the selected row down. */
				m_tableModel.moveRow(m_table.getSelectedRow(), m_table.getSelectedRow(),
						m_table.getSelectedRow() + 1);
				m_table.getSelectionModel().setSelectionInterval(m_table.getSelectedRow() + 1,
						m_table.getSelectedRow() + 1);
			}
		}
	};

	public DeriveFieldNodeDialog() {
		m_tableModel = new DefaultTableModel();
		m_tableModel.setColumnIdentifiers(m_columnNames);
		m_table = new JTable(m_tableModel);
		m_table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tableModel.addTableModelListener(m_tableListener);
		m_table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (m_table.getSelectedRow() == 0 && m_table.getRowCount() > 1) {
					m_moveUpButton.setEnabled(false);
					m_moveDownButton.setEnabled(true);
				} else if (m_table.getSelectedRow() == m_table.getRowCount() - 1 && m_table.getRowCount() > 1) {
					m_moveDownButton.setEnabled(false);
					m_moveUpButton.setEnabled(true);
				} else if (m_table.getRowCount() > 1) {
					m_moveUpButton.setEnabled(true);
					m_moveDownButton.setEnabled(true);
				}
			}
		});

		m_addButton = new JButton("Add");
		m_editButton = new JButton("Edit...");
		m_removeButton = new JButton("Remove");
		m_removeAllButton = new JButton("Remove All");
		m_moveUpButton = new JButton("Move Up");
		m_moveDownButton = new JButton("Move Down");
		m_copyButton = new JButton("Copy");

		m_editButton.setEnabled(false);
		m_moveDownButton.setEnabled(false);
		m_moveUpButton.setEnabled(false);
		m_removeAllButton.setEnabled(false);
		m_removeButton.setEnabled(false);
		m_copyButton.setEnabled(false);

		m_addButton.addActionListener(m_buttonListener);
		m_editButton.addActionListener(m_buttonListener);
		m_removeButton.addActionListener(m_buttonListener);
		m_removeAllButton.addActionListener(m_buttonListener);
		m_moveUpButton.addActionListener(m_buttonListener);
		m_moveDownButton.addActionListener(m_buttonListener);
		m_copyButton.addActionListener(m_buttonListener);

		initLayout();
	}

	private void initLayout() {
		/* Panel consists of two parts: the table containing the expression + column, and the buttons. */
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraint = new GridBagConstraints();

		constraint.gridx = 0;
		constraint.gridy = 0;
		constraint.fill = GridBagConstraints.BOTH;

		mainPanel.add(new JScrollPane(m_table), constraint);

		/* Use simple GridLayout as the buttons are in one line. */
		GridLayout grid = new GridLayout(7, 1);
		grid.setVgap(5);
		JPanel subPanel = new JPanel(grid);

		subPanel.add(m_addButton);
		subPanel.add(m_editButton);
		subPanel.add(m_copyButton);
		subPanel.add(m_removeButton);
		subPanel.add(m_removeAllButton);
		subPanel.add(m_moveUpButton);
		subPanel.add(m_moveDownButton);

		constraint.gridx++;
		constraint.insets = new Insets(5, 5, 5, 5);
		constraint.fill = GridBagConstraints.NONE;
		constraint.anchor = GridBagConstraints.PAGE_START;

		mainPanel.add(subPanel, constraint);

		this.addTab("Expression", mainPanel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {
	}

}
