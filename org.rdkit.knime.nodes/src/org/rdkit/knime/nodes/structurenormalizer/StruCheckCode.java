package org.rdkit.knime.nodes.structurenormalizer;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.NodeLogger;

/**
 * Defines Structure Checker Codes as used in the RDKit. This definition must
 * match the definition used in RDKit.
 * 
 * @author Manuel Schwarze
 */
public enum StruCheckCode {

	BAD_MOLECULE(1, "Unable to recognize a molecule"),
	ALIAS_CONVERSION_FAILED(2, "The atom alias conversion failed"),
	TRANSFORMED(4, "Structure has been transformed"),
	FRAGMENTS_FOUND(8, "Multiple fragments have been found"),
	EITHER_WARNING(16, "A wiggly bond has been removed"),
	STEREO_ERROR(32, "Stereochemistry is ambiguously defined"),
	DUBIOUS_STEREO_REMOVED(64, "A stereo bond has been removed"),
	ATOM_CLASH(128, "There are two atoms or bonds are too close to each other"),
	ATOM_CHECK_FAILED(256, "The atom environment is not correct"),
	SIZE_CHECK_FAILED(512, "The molecule is too big"),
	RECHARGED(1024, "Structure has been recharged"),
	STEREO_FORCED_BAD(2048, "Structure has failed: Bad stereochemistry"),
	STEREO_TRANSFORMED(4096, "Stereochemistry has been modified"),
	TEMPLATE_TRANSFORMED(8192, "Structure has been modified using a template");

	//
	// Constants
	//

	/** The logging instance. */
	private static NodeLogger LOGGER = NodeLogger.getLogger(StruCheckCode.class);

	/** Defines, which codes are error codes. */
	public static int ERROR_CODE_MASK = 
			(BAD_MOLECULE.getValue() |
			ALIAS_CONVERSION_FAILED.getValue() |
			STEREO_ERROR.getValue() |
			STEREO_FORCED_BAD.getValue() |
			ATOM_CLASH.getValue() |
			ATOM_CHECK_FAILED.getValue() |
			SIZE_CHECK_FAILED.getValue());

	/** Defines, which codes are transformation codes. */
	public static int TRANSFORMATION_CODE_MASK =
			(TRANSFORMED.getValue() |
			FRAGMENTS_FOUND.getValue() |
			EITHER_WARNING.getValue() |
			DUBIOUS_STEREO_REMOVED.getValue() |
			STEREO_TRANSFORMED.getValue() |
			TEMPLATE_TRANSFORMED.getValue() |
			RECHARGED.getValue());

	// Check that all defined codes are either in ERROR or TRANSFORMATION CODES masks
	static {
		for (final StruCheckCode code : values()) {
			if ((ERROR_CODE_MASK & code.getValue()) == 0 && (TRANSFORMATION_CODE_MASK & code.getValue()) == 0) {
				LOGGER.error("Implementation Error: The StruCheckCode '" + code + " (#" + code.getValue() + ") is neither defined in the ERROR_CODE_MASK nor in the TRANSFORMATION_CODE_MASK.");
			}
		}
	}

	//
	// Members
	//

	private int m_iValue;
	private String m_strMessage;

	private StruCheckCode(final int iValue, final String strMessage) {
		m_iValue = iValue;
		m_strMessage = strMessage;
	}

	//
	// Public Methods
	//

	/**
	 * Returns the value of this code.
	 * 
	 * @return Value.
	 */
	public int getValue() {
		return m_iValue;
	}

	/**
	 * Returns true, if the code is an error code.
	 * 
	 * @return Boolean.
	 */
	public boolean isError() {
		return (ERROR_CODE_MASK & m_iValue) == m_iValue;
	}

	/**
	 * Returns true, if the code is a transformation code (warning, no error).
	 * 
	 * @return Boolean.
	 */
	public boolean isTransformation() {
		return (TRANSFORMATION_CODE_MASK & m_iValue) == m_iValue;
	}

	/**
	 * Returns the message associated with the code.
	 * 
	 * @return
	 */
	public String getMessage() {
		return m_strMessage;
	}

	/**
	 * Returns the string representation of the code.
	 * 
	 * @return String representation.
	 */
	@Override
	public String toString() {
		return name();
	}

	//
	// Static Public Methods
	//

	/**
	 * Determines the bit mask for all codes that shall be treated as errors. Included
	 * will be all codes that are defined in {@link #ERROR_CODE_MASK} and all codes that
	 * are passed in as parameter.
	 * 
	 * @param arrAdditionalFailureCodes Additional codes that shall be treated as error.
	 * 
	 * @return Bit mask of all error codes.
	 */
	public static int getErrorCodeMask(final StruCheckCode... arrAdditionalFailureCodes) {
		int iBitMask = ERROR_CODE_MASK;

		for (final StruCheckCode code : arrAdditionalFailureCodes) {
			iBitMask |= code.getValue();
		}

		return iBitMask;
	}

	/**
	 * Determines the bit mask for all codes that shall not be treated as errors. Included
	 * will be all codes that are neither defined in {@link #ERROR_CODE_MASK} nor passed in as parameter.
	 * 
	 * @param arrAdditionalFailureCodes Additional codes that shall be treated as error.
	 * 
	 * @return Bit mask of all non error codes.
	 */
	public static int getNonErrorCodeMask(final StruCheckCode... arrAdditionalFailureCodes) {
		return (~(getErrorCodeMask(arrAdditionalFailureCodes)) & (ERROR_CODE_MASK | TRANSFORMATION_CODE_MASK));
	}

	/**
	 * Returns an array of codes based on the passed in flags and the passed in bit mask.
	 * Only codes are returned that are set in both values.
	 * 
	 * @param iFlags Flags with codes.
	 * @param iBitMask Bit mask with codes that act as a filter.
	 * 
	 * @return Array of codes, which can be empty, but never null.
	 */
	public static StruCheckCode[] getCodes(final int iFlags, final int iBitMask) {
		final List<StruCheckCode> listCodes = new ArrayList<StruCheckCode>();

		final int iFilteredFlags = (iFlags & iBitMask);
		for (final StruCheckCode code : values()) {
			final int iValue = code.getValue();
			if ((iFilteredFlags & iValue) == iValue) {
				listCodes.add(code);
			}
		}

		return listCodes.toArray(new StruCheckCode[listCodes.size()]);
	}
}
