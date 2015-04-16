package org.rdkit.knime.nodes.highlighting;

import java.awt.Color;

import org.RDKit.DrawColour;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.rdkit.knime.util.SettingsUtils;

//
// Inner classes
//

public class HighlightingDefinition {

	//
	// Enumerations
	//

	public enum Type {
		Atoms, Bonds
	};

	//
	// Constants
	//

	/** The logger instance. */
	protected static final NodeLogger LOGGER = NodeLogger
			.getLogger(HighlightingDefinition.class);

	//
	// Members
	//

	/** Flag to determine to use this highlighting definition. */
	private boolean m_bActive;

	/** Input column name. */
	private String m_strInputColumn;

	/** Type of highlighting. */
	private Type m_type;

	/** Color for highlighting. */
	private Color m_color;

	/** Flag to determine to also highlight neighbor bonds for an atom and neighbor atoms for a bond. */
	private boolean m_bNeighborhood;

	//
	// Constructor
	//

	/**
	 * Creates a new highlighting definition.
	 * 
	 * @param bActive
	 *            True, if this condition shall be checked, false otherwise.
	 * @param strInputColumn
	 *            The input column name. Can be be null.
	 * @param type
	 *            The index type found in the input column. Must not be
	 *            null.
	 * @param color
	 *            Highlighting color or null to use default. Can be null.
	 * @param bNeighborhood
	 *            Flag to determine highlighting of direct neighborhood.
	 *            This is a bond between two highlighted atoms or the atoms
	 *            around a highlighted bond.
	 */
	public HighlightingDefinition(final boolean bActive,
			final String strInputColumn, final Type type,
			final Color color, final boolean bNeighborhood) {
		// Pre-checks
		if (type == null) {
			throw new IllegalArgumentException("Type must not be null.");
		}

		m_bActive = bActive;
		m_strInputColumn = strInputColumn;
		m_type = type;
		m_color = color;
		m_bNeighborhood = bNeighborhood;
	}

	/**
	 * Creates a new highlighting definition by reading its settings from a
	 * config object from the specified index. If the name cannot be found,
	 * it will fail. If some other setting is not found it will fall back to
	 * use a default value.
	 * 
	 * @param config
	 *            Settings configurations. Must not be null.
	 * @param index
	 *            Index of settings to read from. Must be valid.
	 * 
	 * @throws InvalidSettingsException
	 *             Thrown, if the name of the definition was not found in
	 *             the config object under the specified index. Also thrown,
	 *             if null was passed in as config parameter.
	 */
	public HighlightingDefinition(final Config config, final int index)
			throws InvalidSettingsException {
		if (config == null) {
			throw new InvalidSettingsException(
					"No configuration found for Highlighting Definition.");
		}

		m_strInputColumn = config.getString("input_column_" + index, null);
		m_bActive = config.getBoolean("select_" + index, false);
		final String strType = config.getString("type_" + index, "Atom");
		m_type = SettingsUtils.getEnumValueFromString(Type.class, strType,
				Type.Atoms);
		final String strColor = config.getString("color_" + index, null);
		m_color = strColor == null ? null : interpretColor(strColor);
		m_bNeighborhood = config.getBoolean("neighborhood_" + index, false);
	}

	/**
	 * Creates a new highlighting definition by reading its settings from a
	 * config object from the specified index. If the name cannot be found,
	 * it will fail. If some other setting is not found it will fall back to
	 * use a default value.
	 * 
	 * @param config
	 *            Settings configurations. Must not be null.
	 * @param index
	 *            Index of settings to read from. Must be valid.
	 * 
	 * @throws InvalidSettingsException
	 *             Thrown, if the name of the definition was not found in
	 *             the config object under the specified index. Also thrown,
	 *             if null was passed in as config parameter.
	 */
	public HighlightingDefinition(final HighlightingDefinition orig) {
		m_strInputColumn = orig.m_strInputColumn;
		m_bActive = orig.m_bActive;
		m_type = orig.m_type;
		m_color = orig.m_color;
		m_bNeighborhood = orig.m_bNeighborhood;
	}

	//
	// Public Methods
	//

	/**
	 * Stores the highlighting definition settings in the specified config
	 * object.
	 * 
	 * @param config
	 *            Settings configurations. Must not be null.
	 * @param index
	 *            Index of settings to write to.
	 */
	public void saveSettings(final Config config, final int index) {
		if (config == null) {
			throw new IllegalArgumentException(
					"Configuration object must not be null.");
		}

		config.addBoolean("select_" + index, m_bActive);
		config.addString("input_column_" + index, m_strInputColumn);
		config.addString("type_" + index, m_type.name());
		config.addString("color_" + index, m_color == null ? null :
			"0x" + Integer.toHexString(m_color.getRGB() & 0xFFFFFF).toUpperCase());
		config.addBoolean("neighborhood_" + index, m_bNeighborhood);
	}

	/**
	 * Determines, if this condition is active.
	 * 
	 * @return True, if active, false otherwise.
	 */
	public boolean isActive() {
		return m_bActive;
	}

	/**
	 * Sets the flag to activate or deactivate this highlighting definition.
	 * 
	 * @param bActive
	 *            True to activate, false to deactivate.
	 */
	public void setActive(final boolean bActive) {
		m_bActive = bActive;
	}

	/**
	 * Returns the type of the items to be highlighted.
	 * 
	 * @return Type of highlighting.
	 */
	public Type getType() {
		return m_type;
	}

	/**
	 * Sets the type for this highlighting.
	 * 
	 * @param type
	 *            Type used for highlighting.
	 */
	public void setType(final Type type) {
		m_type = type;
	}

	/**
	 * Sets the input column that contains the indexes to be highlighted.
	 * 
	 * @param strInputColumn
	 *            Input column.
	 */
	public void setInputColumn(final String strInputColumn) {
		this.m_strInputColumn = strInputColumn;
	}

	/**
	 * Returns the input column that contains the indexes to be highlighted.
	 * 
	 * @return Input column.
	 */
	public String getInputColumn() {
		return m_strInputColumn;
	}

	/**
	 * Sets the highlighting color. Set to null to use default.
	 * 
	 * @param color
	 *            Highlighting color or null to use default.
	 */
	public void setColor(final Color color) {
		this.m_color = color;
	}

	/**
	 * Returns the highlighting color to be used.
	 * 
	 * @return Color or null to use default.
	 */
	public Color getColor() {
		return m_color;
	}

	/**
	 * Returns the highlighting color in RDKit style to be used.
	 * 
	 * @return RDkit Color object or null to use default.
	 */
	public DrawColour getRdkitColor() {
		return m_color == null ? null : new DrawColour(
				m_color.getRed() / 255.0f,
				m_color.getGreen() / 255.0f,
				m_color.getBlue() / 255.0f);
	}

	/**
	 * Sets the neighborhood mode for highlighting. If true, bonds between
	 * highlighted atoms will be highlighted as well, and atoms around
	 * highlighted bonds will be highlighted as well. If false, it will only
	 * highlight the defined items (atoms or bonds).
	 * 
	 * @param m_bNeighborhood
	 */
	public void setIncludeNeighborhood(final boolean m_bNeighborhood) {
		this.m_bNeighborhood = m_bNeighborhood;
	}

	/**
	 * Determines, if the neighborhood is included for highlighting.
	 * 
	 * @return If true, bonds between highlighted atoms will be highlighted
	 *         as well, and atoms around highlighted bonds will be
	 *         highlighted as well. If false, it will only highlight the
	 *         defined items (atoms or bonds).
	 */
	public boolean isNeighborhoodIncluded() {
		return m_bNeighborhood;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (m_bActive ? 1231 : 1237);
		result = prime * result + (m_bNeighborhood ? 1231 : 1237);
		result = prime * result
				+ ((m_color == null) ? 0 : m_color.hashCode());
		result = prime
				* result
				+ ((m_strInputColumn == null) ? 0 : m_strInputColumn
						.hashCode());
		result = prime * result
				+ ((m_type == null) ? 0 : m_type.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final HighlightingDefinition other = (HighlightingDefinition) obj;
		if (m_bActive != other.m_bActive)
			return false;
		if (m_bNeighborhood != other.m_bNeighborhood)
			return false;
		if (m_color == null) {
			if (other.m_color != null)
				return false;
		} else if (!m_color.equals(other.m_color))
			return false;
		if (m_strInputColumn == null) {
			if (other.m_strInputColumn != null)
				return false;
		} else if (!m_strInputColumn.equals(other.m_strInputColumn))
			return false;
		if (m_type != other.m_type)
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(
				"HighlightingDefinition { ");
		sb.append("input_column = ")
		.append(getInputColumn())
		.append(", type = ")
		.append(getType())
		.append(", color = ")
		.append(getColor())
		.append(", includeNeighborhood = "
				+ isNeighborhoodIncluded())
				.append(", isActive = " + isActive()).append(" }");

		return sb.toString();
	}

	//
	// Public Static Methods
	//

	/**
	 * Interprets the specified color string and returns the color.
	 * 
	 * @param strColor Color string. Can be null.
	 * 
	 * @return Color object. Null, if null was passed in or a default color was specified.
	 */
	public static Color interpretColor(String strColor) {
		Color col = null;

		if (strColor != null) {
			strColor = strColor.toLowerCase();

			// Check for default string
			if (!"null".equals(strColor) && !"default".equals(strColor)) {
				// Handle different color formats
				// 1. 0xRRGGBB in hex format
				if (strColor.startsWith("0x")) {
					col = Color.decode(strColor);
				}
				// 2. Three float or int values
				else if (strColor.indexOf(",") > 0){
					final String[] arrRGB = strColor.split(",");
					if (arrRGB != null && arrRGB.length == 3) {
						// 2a. Three float values
						if (arrRGB[0].indexOf(".") > 0 || arrRGB[1].indexOf(".") > 0 || arrRGB[2].indexOf(".") > 0) {
							col = new Color(convertFloatStringToInteger(arrRGB[0]),
									convertFloatStringToInteger(arrRGB[1]),
									convertFloatStringToInteger(arrRGB[2]));
						}
						// 2b. Three int values
						else {
							col = new Color(convertIntStringToInteger(arrRGB[0]),
									convertIntStringToInteger(arrRGB[1]),
									convertIntStringToInteger(arrRGB[2]));
						}
					}
					else {
						LOGGER.error("Encountered incorrect RGB values in color coding: " + strColor);
					}
				}
				// 3. Integer value
				else {
					try {
						col = new Color(Integer.parseInt(strColor));
					}
					catch (final NumberFormatException exc) {
						LOGGER.error("Encountered incorrect integer value in color coding: " + strColor);
					}
				}
			}
		}

		return col;
	}

	//
	// Private Static Methods
	//

	private static int convertFloatStringToInteger(final String strFloat) {
		int iRet = 0;

		if (strFloat != null) {
			try {
				float f = Float.parseFloat(strFloat);
				if (f < 0.0f) {
					f = 0.0f;
				}
				if (f > 1.0f) {
					f = 255.0f;
				}
				iRet = Math.round(f * 255.0f);
			}
			catch (final NumberFormatException exc) {
				LOGGER.error("Encountered incorrect float color code for R, G or B value: " + strFloat);
			}
		}

		return iRet;
	}

	private static int convertIntStringToInteger(final String strInt) {
		int iRet = 0;

		if (strInt != null) {
			try {
				iRet = Integer.parseInt(strInt);
				if (iRet < 0) {
					iRet = 0;
				}
				if (iRet > 255) {
					iRet = 255;
				}
			}
			catch (final NumberFormatException exc) {
				LOGGER.error("Encountered incorrect int color code for R, G or B value: " + strInt);
			}
		}

		return iRet;
	}
}
