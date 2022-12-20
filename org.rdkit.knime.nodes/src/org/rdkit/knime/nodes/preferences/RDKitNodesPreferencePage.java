/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2013
 * Novartis Institutes for BioMedical Research
 *
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
 */
package org.rdkit.knime.nodes.preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.RDKit.RDKFuncs;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.data.renderer.MultiLineStringValueRenderer;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.rdkit.knime.RDKitTypesPluginActivator;
import org.rdkit.knime.extensions.aggregration.RDKitMcsAggregationPreferencePage;
import org.rdkit.knime.nodes.AbstractRDKitCalculatorNodeModel;
import org.rdkit.knime.nodes.AbstractRDKitSplitterNodeModel;
import org.rdkit.knime.nodes.RDKitNodePlugin;
import org.rdkit.knime.nodes.TableViewSupport;
import org.rdkit.knime.properties.FingerprintSettingsHeaderPropertyHandler;
import org.rdkit.knime.util.EclipseUtils;
import org.rdkit.knime.util.PreferenceButton;

/**
 * This is the preference page for the RDKit chemistry type definition. It
 * allows the user to change preferred renderer for all types listed in
 * {@link RDKitTypesPluginActivator#getCustomizableTypeList()}.
 *
 * @author Greg Landrum
 * @author Manuel Schwarze
 */
public class RDKitNodesPreferencePage extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

	//
	// Constants
	//

	/** The logger instance. */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(
			RDKitNodesPreferencePage.class);

	/** The id of this preference page. */
	public static final String ID = "org.rdkit.knime.nodes.preferences";

	//
	// Globals
	//

	/**
	 * Flag to determine, that defaults have been initialized already to avoid double init
	 * after such default may have been overridden from the outside.
	 */
	private static boolean g_bDefaultInitializationDone = false;

	//
	// Constructors
	//

	/**
	 * Creates a new preference page.
	 */
	public RDKitNodesPreferencePage() {
		super(GRID);

		setImageDescriptor(new ImageDescriptor() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public ImageData getImageData() {
				return EclipseUtils.loadImageData(RDKitMcsAggregationPreferencePage.class,
						"/icons/category_rdkit.png");
			}
		});

		// We use the pref store of the UI plugin
		setPreferenceStore(RDKitNodePlugin.getDefault().getPreferenceStore());
		setDescription("This section contains sub sections to control preferences for the RDKit Nodes.\nThe nodes are using version "+RDKFuncs.getRdkitVersion()+" of the RDKit backend.");
	}

	/** {@inheritDoc} */
	@Override
	protected void createFieldEditors() {
      final PreferenceButton btnSyncNow = new PreferenceButton("Create .csv File with RDKit Nodes Introspection Information", getFieldEditorParent()) {
         @Override
         protected void onButtonClicked() {
            if (performOk()) {
               OutputStream out = null;
               String strFilename = System.getProperty("java.io.tmpdir") + File.separator + "RDKit_Nodes_Introspection_Information.csv";
               try {
                  LOGGER.info("Creating .csv File with RDKit Nodes Introspection Information ...");
                  out = new FileOutputStream(strFilename);
                  boolean bHeaderOutput = false;
                  IExtensionRegistry registry = Platform.getExtensionRegistry();
                  IExtensionPoint extensionPoint = registry.getExtensionPoint("org.knime.workbench.repository.nodes");
                  IExtension[] extensions = extensionPoint.getExtensions();
                  // For each extension ...
                  for (int i = 0; i < extensions.length; i++) {  
                      IExtension extension = extensions[i];
                      if (RDKitNodePlugin.PLUGIN_ID.equals(extension.getContributor().getName())) {
                         IConfigurationElement[] elements = extension.getConfigurationElements();
                         for (int j = 0; j < elements.length; j++) {
                            IConfigurationElement element = elements[j];
                            String id = "Unknown";
                            try {
                               if ("node".equals(element.getName())) {
                                  if (!bHeaderOutput) {
                                     bHeaderOutput = true;
                                     String header = "Name;Category;Deprecated;#Input;#Output;#Settings;"
                                           + "KNIME Type;RDKit Node Type;Super Class;Table View?;"
                                           + "Pre%;Proc%;Post%;"
                                           + "In Type 1;In Type 2;In Type 3;"
                                           + "Out Type 1;Out Type 2;Out Type 3;";
                                     LOGGER.info(header);
                                     out.write(header.getBytes());
                                     out.write("\n".getBytes());
                                  }
                                  id = element.getAttribute("id");
                                  boolean bDeprecated = "true".equals(element.getAttribute("deprecated"));
                                  String factoryClass = element.getAttribute("factory-class");
                                  String categoryPath = element.getAttribute("category-path");
                                  Class<?> factory = Class.forName(factoryClass);
                                  NodeFactory<?> nodeFactory = (NodeFactory<?>)factory.getDeclaredConstructor().newInstance();
                                  NodeModel nodeModel = nodeFactory.createNodeModel();
                                  InputPortRole[] inPortRoles = nodeModel.getInputPortRoles();
                                  OutputPortRole[] outPortRoles = nodeModel.getOutputPortRoles();
                                  int inputPorts = inPortRoles.length;
                                  int outputPorts = outPortRoles.length;
                                  
                                  StringBuilder sb = new StringBuilder(id.substring(id.lastIndexOf(".") + 1))
                                        .append(";")
                                        .append(categoryPath.substring(categoryPath.lastIndexOf("/") + 1))
                                        .append(";")
                                        .append(bDeprecated)
                                        .append(";")
                                        .append(inputPorts)
                                        .append(";")
                                        .append(outputPorts)
                                        .append(";");
                                  
                                  int iSettingCount = 0;
                                  for (final Field f : nodeModel.getClass().getDeclaredFields()) {
                                     final String strFieldName = f.getName();
                                     if (strFieldName.contains("model")) {
                                        iSettingCount++;
                                     }
                                  }
                                  sb.append(iSettingCount);
                                  sb.append(";");
   
                                  if (inputPorts > 0 && outputPorts > 0) {
                                     sb.append("Manipulator;");
                                  }
                                  else if (inputPorts == 0) {
                                     sb.append("Source;");
                                  }
                                  else if (outputPorts == 0) {
                                     sb.append("Sink;");
                                  }
                                  else {
                                     sb.append("Singleton;");
                                  }
                                  
                                  if (nodeModel instanceof AbstractRDKitCalculatorNodeModel) {
                                     sb.append("Calculator;").append("AbstractRDKitCalculatorNodeModel");
                                  }
                                  else if (nodeModel instanceof AbstractRDKitSplitterNodeModel) {
                                     sb.append("Splitter or Filter;").append("AbstractRDKitSplitterNodeModel");
                                  }
                                  else {
                                     sb.append("Modifier or Other;").append(nodeModel.getClass().getSuperclass().getSimpleName());
                                  }
                                  sb.append(";");
                                  if (nodeModel instanceof TableViewSupport) {
                                     sb.append("TableView");
                                  }
                                  sb.append(";");
                                  
                                  double preProcessing = 0.0d;
                                  for (final Method m : nodeModel.getClass().getDeclaredMethods()) {
                                     final String strMethodName = m.getName();
                                     if ("getPreProcessingPercentage".equals(strMethodName)) {
                                        boolean bPreAccess = m.isAccessible();
                                        m.setAccessible(true);
                                        preProcessing = (Double)m.invoke(nodeModel);
                                        m.setAccessible(bPreAccess);
                                        break;
                                     }
                                  }
                                  
                                  double postProcessing = 0.0d;
                                  for (final Method m : nodeModel.getClass().getDeclaredMethods()) {
                                     final String strMethodName = m.getName();
                                     if ("getPostProcessingPercentage".equals(strMethodName)) {
                                        boolean bPreAccess = m.isAccessible();
                                        m.setAccessible(true);
                                        postProcessing = (Double)m.invoke(nodeModel);
                                        m.setAccessible(bPreAccess);
                                        break;
                                     }
                                  }
                                  
                                  double processing = 1.0d - preProcessing - postProcessing;
                                  sb.append(preProcessing * 100);
                                  sb.append(";");
                                  sb.append(processing * 100);
                                  sb.append(";");
                                  sb.append(postProcessing * 100);
                                  sb.append(";");
                                  for (int k = 0; k < 3; k++) {
                                     if (inPortRoles != null && k < inPortRoles.length) {
                                        if (inPortRoles[k] != null) {                                           
                                           sb.append(inPortRoles[k].isDistributable() ? "D" : "");
                                           sb.append(inPortRoles[k].isStreamable() ? "S" : "");
                                           if (!inPortRoles[k].isDistributable() && !inPortRoles[k].isStreamable()) {
                                              sb.append("---");
                                           }
                                        }
                                        else {
                                           sb.append("---");
                                        }
                                     }
                                     sb.append(";");
                                  }
                                  for (int k = 0; k < 3; k++) {
                                     if (outPortRoles != null && k < outPortRoles.length) {
                                        if (outPortRoles[k] != null) {
                                           sb.append(outPortRoles[k].isDistributable() ? "D" : "---");
                                        }
                                        else {
                                           sb.append("---");
                                        }
                                     }
                                     sb.append(";");
                                  }
                                                                    
                                  String strRow = sb.toString();
                                  LOGGER.info(strRow);
                                  out.write(strRow.getBytes());
                                  out.write("\n".getBytes());
                               }
                            }
                            catch (Exception exc) {
                               LOGGER.debug("Unable to introspect node " + id, exc);
                            }
                         }
                      }
                  }
                  LOGGER.info("File " + strFilename + " was written successfully.");
               }
               catch (Exception exc) {
                  LOGGER.error("Unable to create file " + strFilename, exc);
               }
               finally {
                  try {
                     out.close();
                  }
                  catch (Exception exc) {
                     // Ignore
                  }
               }
            }
         }
      };
      addField(btnSyncNow);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final IWorkbench workbench) {
		// nothing to do
	}

	/**
	 * Gets the appropriate preference store and initializes its default values.
	 * This method must be called from the subclass of AbstractPreferenceInitializer,
	 * which needs to be configured in the plugin.xml file as extension point.
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public static synchronized void initializeDefaultPreferences() {
		if (!g_bDefaultInitializationDone) {
			g_bDefaultInitializationDone = true;

			try {
				// We use the preference store that is defined in the UI plug-in
				final RDKitNodePlugin plugin = RDKitNodePlugin.getDefault();

				if (plugin != null) {
					final IPreferenceStore prefStore = plugin.getPreferenceStore();

					// Define plug-in default values
					// Define the fingerprint header property handler's renderer as default
					prefStore.setDefault(
							FingerprintSettingsHeaderPropertyHandler.PREF_KEY_RENDERER,
							MultiLineStringValueRenderer.Factory.class.getName());
				}
			}
			catch (final Exception exc) {
				LOGGER.error("Default values could not be set for the RDKit Nodes preferences. Plug-In or Preference Store not found.");
			}
		}
	}

}
