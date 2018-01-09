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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.expressions.util.ExpressionParser;
import org.knime.expressions.util.SingleExpressionCellFactory;

/**
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class DeriveFieldNodeModel extends NodeModel {

	private DeriveFieldNodeConfiguration m_configuration;

	/**
	 * Empty constructor.
	 */
	public DeriveFieldNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (m_configuration == null) {
			throw new IllegalStateException("No configuration to determine table specs available.");
		}

		ColumnRearranger rearranger = createColumnRearranger(inSpecs[0], null);

		return new DataTableSpec[] { rearranger.createSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		ColumnRearranger rearranger = createColumnRearranger(inData[0].getSpec(), exec);

		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], rearranger, exec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// No internals to load
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// No internals to save
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		if (m_configuration != null) {
			m_configuration.saveSettingsTo(settings);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_configuration = new DeriveFieldNodeConfiguration();
		m_configuration.loadSettingsInModel(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO Auto-generated method stub

	}

	private final class DeriveFieldCellFactory extends AbstractCellFactory {

		@Override
		public DataCell[] getCells(DataRow row) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private ColumnRearranger createColumnRearranger(DataTableSpec inSpec, ExecutionContext exec) {
		ColumnRearranger rearranger = new ColumnRearranger(inSpec);

		String[][] expressions = m_configuration.getExpressionTable();
		DataType[] types = m_configuration.getDataTypes();

		DataTableSpecCreator creator = new DataTableSpecCreator(inSpec);

		/*
		 * Used to replace columns and adding new columns in such a way that the column
		 * names are still unique.
		 */
		UniqueNameGenerator replaceGenerator = new UniqueNameGenerator((Set<String>) null);
		UniqueNameGenerator nameGenerator = new UniqueNameGenerator(inSpec);

		HashMap<String, Integer> columnIndexMap = new HashMap<>(inSpec.getColumnNames().length);
		for (String columnName : inSpec.getColumnNames()) {
			columnIndexMap.put(columnName, inSpec.findColumnIndex(columnName));
		}

		/*
		 * Iterate over all column names that are generated/replaced in the node and
		 * append/replace them in the current spec.
		 */
		for (int i = 0; i < expressions[0].length; i++) {
			if (columnIndexMap.containsKey(expressions[0][i])) {
				int colIndex = columnIndexMap.get(expressions[0][i]);

				rearranger.remove(colIndex);
				rearranger.insertAt(colIndex,
						new SingleExpressionCellFactory(replaceGenerator.newColumn(expressions[0][i], types[i]),
								expressions[1][i], columnIndexMap, types[i], exec));
			} else {
				rearranger
						.append(new SingleExpressionCellFactory(replaceGenerator.newColumn(expressions[0][i], types[i]),
								expressions[1][i], columnIndexMap, types[i], exec));
			}
		}

		return rearranger;
	}

}
