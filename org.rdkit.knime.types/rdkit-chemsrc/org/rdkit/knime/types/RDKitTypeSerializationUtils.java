package org.rdkit.knime.types;

import java.io.IOException;

import org.RDKit.ChemicalReaction;
import org.RDKit.ROMol;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;

/**
 * Serialization methods used by KNIME for the old Python integrations (org.knime.python and org.knime.python2),
 * as well as the Columnar Backend and the current Python integration (org.knime.python3).
 * 
 * The methods live in a separate class because the "old" and "current" Python integrations are both optional and
 * should not depend on each other, so the shared functionality must live outside of both.
 * 
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public class RDKitTypeSerializationUtils { 
	//
	// Constants
	//
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RDKitTypeSerializationUtils.class);
	
	
	//
	// Public Methods
	//
	public static DataCell deserializeMolCell2(final byte[] bytes) throws IOException {
		DataCell cell;

		// Generate missing cell, if input is unavailable
		if (bytes == null || bytes.length == 0) {
			cell = DataType.getMissingCell();
		}

		// Generate an RDKit Mol Cell with canonicalized SMILES attached
		else {
			ROMol mol = null;
			try {
				mol = RDKitMolCell2.toROMol(bytes);
				cell = RDKitMolCellFactory.createRDKitAdapterCell(mol, null);
			}
			catch (final Exception exc) {
				LOGGER.debug(exc);

				// In case of an error throw an IOException
				String strMsg = exc.getMessage();
				if (strMsg == null || strMsg.trim().isEmpty()) {
					strMsg = "Unknown error";
				}
				throw new IOException("Unable to interpret RDKit Molecule: " + strMsg);

			}
			finally {
				if (mol != null) {
					mol.delete();
				}
			}
		}

		return cell;
	}
	
	public static byte[] serializeMolValue(RDKitMolValue value) throws IOException {
		byte[] arrBinaryMolecule = null;

		if (value != null) {
			// Shortcut for the normal case that we have a normal RDKit Mol Cell
			if (value instanceof RDKitMolCell2) {
				arrBinaryMolecule = ((RDKitMolCell2)value).getBinaryValue();
			}

			// Longer way if we have a different implementation (e.g. Adapter Cell), which is slower but always works
			else {
				ROMol mol = null;

				try {
					mol = value.readMoleculeValue();
					arrBinaryMolecule = RDKitMolCell2.toByteArray(mol);
				}
				finally {
					if (mol != null) {
						mol.delete();
					}
				}
			}
		}

		return arrBinaryMolecule;
	}
	
	public static byte[] serializeReactionValue(final RDKitReactionValue value) throws IOException {
		byte[] arrBinaryReaction = null;

		if (value != null) {
			final ChemicalReaction reaction = value.getReactionValue();
			arrBinaryReaction = RDKitReactionCell.toByteArray(reaction);
			// Note: Do not delete the reaction object here, because it is a reference to real cell content
		}

		return arrBinaryReaction;
	}
	
	public static DataCell deserializeReactionCell(final byte[] bytes) throws IOException {
		DataCell cell;

		// Generate missing cell, if input is unavailable
		if (bytes == null || bytes.length == 0) {
			cell = DataType.getMissingCell();
		}

		// Generate an RDKit Reaction Cell
		else {
			try {
				final ChemicalReaction reaction = RDKitReactionCell.toChemicalReaction(bytes);
				cell = new RDKitReactionCell(reaction);
			}
			catch (final Exception exc) {
				LOGGER.debug(exc);
				// In case of an error throw an IOException
				String strMsg = exc.getMessage();
				if (strMsg == null || strMsg.trim().isEmpty()) {
					strMsg = "Unknown error";
				}
				throw new IOException("Unable to interpret RDKit Reaction: " + strMsg);
			}
			// Note: Do not delete the reaction object here, because it is used in the cell
		}

		return cell;
	}
}
