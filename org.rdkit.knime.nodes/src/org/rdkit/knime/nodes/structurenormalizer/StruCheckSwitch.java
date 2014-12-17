package org.rdkit.knime.nodes.structurenormalizer;

public enum StruCheckSwitch {
	cc("Check for collisions", "Check for collisions of atoms with other atoms or bonds"),
	cs("Check stereo conventions", "Check stereo conventions"),
	da("Convert atom text strings to properties", "Convert atom text strings to properties"),
	dg("Convert ISIS groups to S-Groups", "Convert ISIS groups to S-Groups"),
	ds("Convert CPSS STEXT to data fields", "Convert CPSS STEXT to data fields"),
	dw("Squeeze whitespace out of identifiers", "Squeeze whitespace out of identifiers"),
	dz("Strip most of the trailing zeros", "Strip most of the trailing zeros"),
	tm("Split off minor fragments", "Split off minor fragments and keep only largest one"),
	;
	
	//
	// Members
	//

	/** The short description. */
	private String m_strShortDescription;
	
	/** The long description. */
	private String m_strLongDescription;

	//
	// Constructor
	//
	
	/**
	 * Creates a new switch representation.
	 *  
	 * @param strShortDescription Short description.
	 * @param strLongDescription Long description.
	 */
	private StruCheckSwitch(final String strShortDescription, final String strLongDescription) {
		m_strShortDescription = strShortDescription;
		m_strLongDescription = strLongDescription;
	}
	
	//
	// Public Methods
	//
	
	/**
	 * Returns the short description of a switch.
	 * 
	 * @return Short description.
	 */
	public String getShortDescription() {
		return m_strShortDescription;
	}
	
	/**
	 * Returns the long description of a switch.
	 * 
	 * @return Long description.
	 */
	public String getLongDescription() {
		return m_strLongDescription;
	}

	/**
	 * Returns the string representation of the code, the short description.
	 * 
	 * @return String representation.
	 */
	@Override
	public String toString() {
		return getShortDescription();
	}
	
	//
	// Static Public Methods
	//
	
	/**
	 * Generates a string with all passed in switches. Each switch will be
	 * put on a separate line and will start with a minus.
	 * 
	 * @param switches Switches to be put in return string. Can be null.
	 * 
	 * @return Always not null. Can be empty.
	 */
	public static String generateSwitches(StruCheckSwitch... switches) {
		StringBuilder sb = new StringBuilder();
		
		for (StruCheckSwitch switchCode : switches) {
			sb.append("-").append(switchCode.name()).append("\n");
		}
		
		return sb.toString();
	}
}
