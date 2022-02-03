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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Oct 7, 2020 (dietzc): created
 */
package org.rdkit.knime.types;

import java.io.IOException;

import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryReadAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;

/**
 * The {@link ValueFactory} specifies how {@link RDKitMolValue}s are serialized
 * in KNIME's Columnar Backend, which is needed also for communication with the
 * Python (Labs) Scripting node and pure-Python nodes in KNIME.
 * 
 * Serialization into a binary blob works by using the
 * {@link RDKitMolSerializer}.
 * 
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public class RDKitMolCellValueFactory implements ValueFactory<VarBinaryReadAccess, VarBinaryWriteAccess> { // NOSONAR:
																											// cannot be
																											// removed

	private static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitMolCellValueFactory.class);
	/**
	 * Stateless instance of this {@link RDKitMolCellValueFactory}.
	 */
	public static final RDKitMolCellValueFactory INSTANCE = new RDKitMolCellValueFactory();

	@Override
	public DataSpec getSpec() {
		return VarBinaryDataSpec.INSTANCE;
	}

	private static final class RDKitMolCellWriteValue implements WriteValue<RDKitMolValue> {
		private final VarBinaryWriteAccess m_access;

		private RDKitMolCellWriteValue(final VarBinaryWriteAccess access) {
			m_access = access;
		}

		public void setValue(final RDKitMolValue value) {
			try {
				m_access.setByteArray(new RDKitMolSerializer().serialize(value));
			} catch (IOException e) {
				LOGGER.error("Error when serializing RDKitMolValue", e);
			}
		}
	}

	private static final class RDKitMolCellReadValue implements ReadValue, RDKitMolValue {
		private final VarBinaryReadAccess m_access;

		private RDKitMolCellReadValue(final VarBinaryReadAccess access) {
			m_access = access;
		}

		@Override
		public DataCell getDataCell() {
			try {
				return new RDKitMolDeserializer().deserialize(m_access.getByteArray(), null);
			} catch (IOException e) {
				LOGGER.error("Error when deserializing RDKitMolValue", e);
				return null;
			}
		}

		@Override
		public ROMol readMoleculeValue() {
			// Note that these methods of the RDKitMolValue interface currently always
			// cause a deserialization to happen. This is not very efficient, but necessary
			// as the ReadValue is re-used when iterating over the rows of a KNIME table.
			// An API to know whether a new value needs to be read is being developed.
			// However, this is not a pressing issue yet as the ReadValues are not used
			// directly yet but are passed to the BufferedDataTable using a single call to
			// getDataCell().
			return ((RDKitMolValue) getDataCell()).readMoleculeValue();
		}

		@Override
		public String getSmilesValue() {
			return ((RDKitMolValue) getDataCell()).getSmilesValue();
		}

		@Override
		public boolean isSmilesCanonical() {
			return ((RDKitMolValue) getDataCell()).isSmilesCanonical();
		}
	}

	@Override
	public ReadValue createReadValue(VarBinaryReadAccess access) {
		return new RDKitMolCellReadValue(access);
	}

	@Override
	public WriteValue<?> createWriteValue(VarBinaryWriteAccess access) {
		return new RDKitMolCellWriteValue(access);
	}
}
