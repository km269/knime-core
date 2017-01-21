/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   Dec 5, 2016 (hornm): created
 */
package org.knime.core.gateway.codegen.types;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
@JsonPropertyOrder({"name", "description", "package", "methods"})
public class ServiceDef extends AbstractDef {

    private final String m_description;

    private String m_pkg;

    private String m_name;

    private List<ServiceMethod> m_methods;

    /**
     *
     */
    public ServiceDef(
        @JsonProperty("description") final String description,
        @JsonProperty("package") final String pkg,
        @JsonProperty("name") final String name,
        @JsonProperty("methods") final ServiceMethod... methods) {
        m_pkg = checkPackage(pkg);
        m_name = CheckUtils.checkArgumentNotNull(name);
        m_description = description;
        m_methods = Arrays.asList(CheckUtils.checkArgumentNotNull(methods));
    }

    static String checkPackage(final String pkg) {
        CheckUtils.checkArgumentNotNull(pkg);
        CheckUtils.checkArgument(pkg.matches("[a-z0-9]+(?:\\.[a-z0-9]+)*"),
            "Package '%s' invalid: must be like 'abc.def.geh'", pkg);
        return pkg;
    }

    /**
     * @return the pkg
     */
    @JsonProperty("package")
    public String getPackage() {
        return m_pkg;
    }

    @JsonProperty("name")
    public String getName() {
        return m_name;
    }

    /**
     * @return the description
     */
    @JsonProperty("description")
    public String getDescription() {
        return m_description;
    }

    @JsonProperty("methods")
    public List<ServiceMethod> getMethods() {
        return m_methods;
    }

    @JsonIgnore
    public List<String> getImports() {
        return m_methods.stream().flatMap(ServiceMethod::getImports).sorted().distinct().collect(Collectors.toList());
    }

}
