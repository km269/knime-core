/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on 01.08.2014 by koetter
 */
package org.knime.core.node.port.database.aggregation.function;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.port.database.aggregation.SimpleDBAggregationFunction;

/**
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public final class VarSampDBAggregationFunction extends SimpleDBAggregationFunction {

    private static volatile VarSampDBAggregationFunction instance;

    private VarSampDBAggregationFunction() {
        super("VAR_SAMP", "The function computes the sample variance, respectively, of the input values."
                + "The function evaluates all input rows matched by the query and is scaled by 1/(N-1)",
                DoubleCell.TYPE, DoubleValue.class);
    }

    /**
     * Returns the only instance of this class.
     * @return the only instance
     */
    public static VarSampDBAggregationFunction getInstance() {
        if (instance == null) {
            synchronized (VarSampDBAggregationFunction.class) {
                if (instance == null) {
                    instance = new VarSampDBAggregationFunction();
                }
            }
        }
        return instance;
    }
}