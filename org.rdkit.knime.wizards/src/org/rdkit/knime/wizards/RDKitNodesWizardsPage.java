/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
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
package org.rdkit.knime.wizards;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This page enables the user to enter the information needed to create the
 * extension plugin project. The Wizard collects the values via a substitution
 * map, that is used to fill out the templates.
 *
 * @author Manuel Schwarze
 */
@SuppressWarnings("restriction")
public class RDKitNodesWizardsPage extends WizardPage implements Listener {

	//
	// Constants
	//

	/**
	 * An array with deprecated node types. Deprecated node type templates can still
	 * be present in the template folder, but will not show up anymore in the wizard
	 * for the creation of new nodes.
	 */
	public static final String[] DEPRECACTED_NODE_TYPES = new String[] { };

	/** Placeholder used in the template files. */
    public static final String SUBST_PROJECT_NAME = "__PROJECT_NAME__";

	/** Placeholder used in the template files. */
    public static final String SUBST_BASE_PACKAGE = "__BASE_PACKAGE__";

	/** Placeholder used in the template files. */
    public static final String SUBST_NODE_MENU_NAME = "__NODE_MENU_NAME__";

	/** Placeholder used in the template files. */
    public static final String SUBST_NODE_NAME = "__NODE_NAME__";

	/** Placeholder used in the template files. */
    public static final String SUBST_DESCRIPTION = "__DESCRIPTION__";

	/** Placeholder used in the template files. */
    public static final String SUBST_JAR_NAME = "__JAR_NAME__";

	/** Placeholder used in the template files. */
    public static final String SUBST_VENDOR_NAME = "__VENDOR_NAME__";

	/** Placeholder used in the template files. */
    public static final String SUBST_PRE_PROC_PERCENTAGE = "__PRE_PROC_PERCENTAGE__";

	/** Placeholder used in the template files. */
    public static final String SUBST_POST_PROC_PERCENTAGE = "__POST_PROC_PERCENTAGE__";

	/** Placeholder used in the template files. */
    public static final String SUBST_CURRENT_YEAR = "__CURRENT_YEAR__";

	/** Placeholder used in the template files. */
    public static final String SUBST_NODE_TYPE = "__NODE_TYPE__";

	/** Placeholder used in the template files. */
    public static final String SUBST_PARALLEL_PROCESSING = "__PARALLEL_PROCESSING__";

    /** The icon used as logo for the RDKit Node Wizard. */
    private static final ImageDescriptor ICON =
        AbstractUIPlugin.imageDescriptorFromPlugin(
        	RDKitNodesWizardsPlugin.ID, "icons/rdkit_wizard.png");

    /** The icon used for the info button to show the wizard help. */
    private static final Image IMAGE_INFO =
        AbstractUIPlugin.imageDescriptorFromPlugin(
        	RDKitNodesWizardsPlugin.ID, "icons/info.png").createImage();

    //
    // Members
    //

    /** Combobox showing known existing projects. */
    private Combo m_comboExistingProjects;

    /** Text field to define the node name. */
    private Text m_textNodeName;

    /** Text field to define the node package. */
    private Text m_textBasePackage;

    /** Text field to define the node author. */
    private Text m_textVendor;

    /** Text field to define the node description. */
    private Text m_textDescription;

    /** Combobox to define the node type. */
    private Combo m_comboNodeType;

    /** Text field to define the pre-processing percentage for the node. */
    private Text m_textPreProcPerc;

    /** Text field to define the post-processing percentage for the node. */
    private Text m_textPostProcPerc;

    /** Stores whatever the user had selected in the navigation tree before the wizard was started. */
    private TreeSelection m_selection;

    /** The browse button to select packages. */
    private Button m_packageBrowseButton;

    /**
     * Stores whatever was determined as current Java project based on the user's selection
     * in the navigation tree before the wizard was started.
     */
    private IJavaProject m_currentJavaProject;

    /**
     * Option flag to set, if parallel processing should be supported for the new node.
     */
    private Button m_allowParallelProcessing;

    /**
     * Option flag to set, if complex or simple code shall be generated for the new node.
     */
    private Button m_generateComplexCode;

    /**
     * Constructor for WizardPage.
     *
     * @param selection The initial selection
     */
    public RDKitNodesWizardsPage(final ISelection selection) {
        super("wizardPage");

        setTitle("Create new RDKit Node");
        setDescription("This wizard creates a framework for a new RDKit Node.");
        setImageDescriptor(ICON);

        if (selection instanceof TreeSelection) {
            m_selection = (TreeSelection)selection;
        }
    }

    /**
     * Generates the substitution map for values in the templates that
     * are based on wizard settings.
     *
     * @return The substitution map
     */
    public Properties getSubstitutionMap() {
        Properties map = new Properties();

        map.put(SUBST_PROJECT_NAME, getProjectName());
        map.put(SUBST_BASE_PACKAGE, getNodePackage());
        map.put(SUBST_NODE_NAME, getNodeClassName());
        map.put(SUBST_NODE_MENU_NAME, getNodeMenuName());
        map.put(SUBST_DESCRIPTION, getNodeDescription().replaceAll(
                "\\n", " * \\n"));
        map.put(SUBST_VENDOR_NAME, getNodeVendor());
        map.put(SUBST_JAR_NAME, getNodeName().toLowerCase() + ".jar");
        map.put(SUBST_PRE_PROC_PERCENTAGE, "" + getPreProcessingPercentage());
        map.put(SUBST_POST_PROC_PERCENTAGE, "" + getPostProcessingPercentage());
        map.put(SUBST_NODE_TYPE, "Manipulator"); // Constant setting
        map.put(SUBST_PARALLEL_PROCESSING, "" + isParallelProcessingSupported());
        return map;
    }

    /**
     * Shows the help dialog window.
     */
    public void showHelp() {
    	// Copy help files to temp directory
    	URL fileUrlTemp = null;

    	try {
    		fileUrlTemp = makeHelpFilesAvailable();
    	}
    	catch (IOException exc) {
    		// Ignored - handled later
    	}

    	final Shell parentShell = getShell();

    	if (fileUrlTemp != null) {
        	final URL fileUrl = fileUrlTemp;
        	Dialog dialog = new Dialog(parentShell) {
        	    @Override
        	    protected Control createDialogArea(final Composite parent) {
        	        Composite composite = (Composite) super.createDialogArea(parent);
        	        Browser browser = new Browser(composite, SWT.BORDER | SWT.FILL);
        	        browser.setUrl(fileUrl.toString());
        	        GridData data = new GridData(GridData.FILL_BOTH);
        	        browser.setLayoutData(data);
        	        return composite;
        	    }

        	    @Override
        		protected void createButtonsForButtonBar(final Composite parent) {
        			// create OK button
        			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
        					true);
        		}

        	    @Override
        	    protected Point getInitialSize() {
        	        return new Point(700, 700);
        	    }

        	    @Override
        	    protected Point getInitialLocation(final Point initialSize) {
        	    	Dimension dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
        	    	return new Point(dimScreen.width / 2 - initialSize.x / 2,
        	    			dimScreen.height / 2 - initialSize.y / 2);
        	    }

        	    @Override
        	    protected void configureShell(final Shell newShell) {
        	        super.configureShell(newShell);
        	        newShell.setText("RDKit Node Types");
        	    }
        	};

        	dialog.setBlockOnOpen(true);
        	dialog.open(); // Blocks
        }
        else {
        	MessageBox msgBox = new MessageBox(parentShell, SWT.ICON_ERROR | SWT.OK);
        	msgBox.setText("Error");
        	msgBox.setMessage("Sorry. The help file is not available.");
        	msgBox.open();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setFont(parent.getFont());

        initializeDialogUnits(parent);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
                IIDEHelpContextIds.NEW_PROJECT_WIZARD_PAGE);

        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        setPageComplete(validatePage());

        // Show description on opening
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);

        // Existing project specification group
        final Composite existProjectGroup = new Composite(composite, SWT.NONE);
        existProjectGroup.setLayout(new GridLayout(3, false));
        existProjectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Project name to select
        CLabel projectLabel = new CLabel(existProjectGroup, SWT.NONE | SWT.RIGHT);
		projectLabel.setMargins(0, 0, 15, 0);
        projectLabel.setText("Select an existing project:");
		projectLabel.setFont(existProjectGroup.getFont());
		m_comboExistingProjects = createProjectsCombo(existProjectGroup);
		m_comboExistingProjects.setFont(composite.getFont());
		m_comboExistingProjects.addListener(SWT.Modify, new Listener() {

			/**
			 * Revalidates all settings when the user changes the project name.
			 * @param e Event.
			 */
			@Override
			public void handleEvent(final Event e) {
				m_currentJavaProject = JavaModelManager.getJavaModelManager()
						.getJavaModel()
						.getJavaProject(m_comboExistingProjects.getText());
				boolean valid = validatePage();
				setPageComplete(valid);
			}
		});

		// Group for KNIME settings
		Group settingsGroup = new Group(composite, SWT.NONE);
		settingsGroup.setText("RDKit Node Settings");
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginTop = 5;
		settingsGroup.setLayout(gridLayout);
		settingsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Node name as base name for the classes to generate
        CLabel label = new CLabel(settingsGroup, SWT.NONE | SWT.RIGHT);
        label.setAlignment(SWT.RIGHT);
        label.setText("Node name: ");
        label.setToolTipText("Generated classes will have 'NodeModel', 'NodeDialog', etc. appended to this name.");
        label.setFont(composite.getFont());
        m_textNodeName = new Text(settingsGroup, SWT.BORDER);
        m_textNodeName.setFont(composite.getFont());
        m_textNodeName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_textNodeName.addListener(SWT.Modify, this);
        m_textNodeName.addListener(SWT.Modify, new Listener() {

			/**
			 * Calculates package, class and menu name, when the user changes the node name.
			 * @param e Event.
			 */
			@Override
			public void handleEvent(final Event e) {
				String strCurrentPackageName = m_textBasePackage.getText();
				int iIndex = strCurrentPackageName.lastIndexOf(".");
				String strNewPackageName = strCurrentPackageName.substring(0, iIndex + 1) +
					getNodeClassName(false).toLowerCase();
				m_textBasePackage.setText(strNewPackageName);
				m_textNodeName.setToolTipText("Class Name: " + getNodeClassName() + "\n" +
						"Menu Name: " + getNodeMenuName());
			}
		});

        // Base package
        label = new CLabel(settingsGroup, SWT.NONE | SWT.RIGHT);
        label.setText("Node package name: ");
        label.setFont(composite.getFont());
        Composite compo = new Composite(settingsGroup, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        compo.setLayout(layout);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalIndent = 0;
        compo.setLayoutData(data);
        m_textBasePackage = new Text(compo, SWT.BORDER);
        m_textBasePackage.setText("org.rdkit.knime.nodes.<new name>");
        m_textBasePackage.setFont(composite.getFont());
        m_textBasePackage.setLayoutData( new GridData(GridData.FILL_HORIZONTAL));
        m_textBasePackage.addListener(SWT.Modify, this);

        String strSelectedPackage = getSelectedPackage();
        if (strSelectedPackage != null && !strSelectedPackage.isEmpty()) {
            m_textBasePackage.setText(strSelectedPackage);
        }

        m_packageBrowseButton = new Button(compo, SWT.PUSH);
        m_packageBrowseButton.setText("Browse...");
        m_packageBrowseButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            /**
             * Let's the user choose a project from a pop-up dialog.
             * @param e Selection Event when Browse button is clicked.
             */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (m_currentJavaProject != null) {
                    try {
						SelectionDialog dialog = JavaUI
								.createPackageDialog(
										getShell(),
										m_currentJavaProject,
										IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
						dialog.open();
						Object[] results = dialog.getResult();
						if (results != null && results.length >= 1) {
							if (results[0] instanceof IPackageFragment) {
								m_textBasePackage
										.setText(((IPackageFragment) results[0])
												.getElementName());
							}
						}
					}
					catch (Exception ex) {
						// Empty by purpose - do nothing
                    }
                }
            }
        });

        // Vendor name
        label = new CLabel(settingsGroup, SWT.NONE | SWT.RIGHT);
        label.setMargins(0, 10, 10, 0);
        label.setText("Node author (your name): ");
        label.setToolTipText("This name will appear in the source files as author information.");
        label.setFont(composite.getFont());
        m_textVendor = new Text(settingsGroup, SWT.BORDER);
        m_textVendor.setText(System.getProperty("user.name", "NIBR"));
        m_textVendor.setFont(composite.getFont());
        m_textVendor.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_textVendor.addListener(SWT.Modify, this);

        // Node Description
        label = new CLabel(settingsGroup, SWT.NONE | SWT.RIGHT);
        label.setMargins(0, 10, 10, 0);
        label.setText("Node description text: ");
        m_textDescription =
                new Text(settingsGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP
                        | SWT.V_SCROLL);
        data = new GridData(GridData.FILL_BOTH);
        data.minimumHeight = 60;
        m_textDescription.setFont(composite.getFont());
        m_textDescription.setLayoutData(data);
        m_textDescription.addListener(SWT.Modify, this);

        // Node type
        label = new CLabel(settingsGroup, SWT.NONE | SWT.RIGHT);
        label.setMargins(0, 10, 10, 0);
        label.setText("Node type:");

        compo = new Composite(settingsGroup, SWT.NONE);
        layout = new GridLayout(2, false);
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        compo.setLayout(layout);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalIndent = 0;
        compo.setLayoutData(data);
        m_comboNodeType = createCategoryCombo(compo);
        m_comboNodeType.setFont(composite.getFont());
        m_comboNodeType.addListener(SWT.Modify, this);

        // Help button to get Node Type Descriptions
        Button btnHelp = new Button(compo, SWT.PUSH);
        btnHelp.setImage(IMAGE_INFO);
        btnHelp.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            /**
             * Let's the user choose a project from a pop-up dialog.
             * @param e Selection Event when Browse button is clicked.
             */
            @Override
            public void widgetSelected(final SelectionEvent e) {
            	showHelp();
            };
        });

        // Parallel processing
        label = new CLabel(settingsGroup, SWT.NONE | SWT.RIGHT);
        label.setMargins(0, 10, 10, 0);
        label.setText("Multi-Threading: ");
        label.setFont(composite.getFont());
        m_allowParallelProcessing = new Button(settingsGroup, SWT.CHECK);
        m_allowParallelProcessing.setText("Allow parallel processing, if possible");
        m_allowParallelProcessing.setFont(settingsGroup.getFont());
        m_allowParallelProcessing.setLayoutData(new GridData(GridData.FILL_BOTH));
        m_allowParallelProcessing.setSelection(true);

        // Node complexity
        label = new CLabel(settingsGroup, SWT.NONE | SWT.RIGHT);
        label.setMargins(0, 10, 10, 0);
        label.setText("Complexity: ");
        label.setFont(composite.getFont());
        m_generateComplexCode = new Button(settingsGroup, SWT.CHECK);
        m_generateComplexCode.setText("Generate complex code");
        m_generateComplexCode.setFont(settingsGroup.getFont());
        m_generateComplexCode.setLayoutData(new GridData(GridData.FILL_BOTH));
        m_generateComplexCode.setSelection(false);
        m_generateComplexCode.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
	            boolean bComplex = m_generateComplexCode.getSelection();
	            m_textPreProcPerc.setEnabled(bComplex);
	            m_textPostProcPerc.setEnabled(bComplex);
            }
        });

        // Progress percentages for pre processing
        label = new CLabel(settingsGroup, SWT.NONE | SWT.RIGHT);
        label.setMargins(0, 0, 40, 0);
        label.setText("Pre-processing activities: ");
        label.setFont(composite.getFont());
        compo = new Composite(settingsGroup, SWT.NONE);
        layout = new GridLayout(2, false);
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        compo.setLayout(layout);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalIndent = 0;
        compo.setLayoutData(data);
        m_textPreProcPerc = new Text(compo, SWT.BORDER | SWT.RIGHT);
        m_textPreProcPerc.setText("0");
        m_textPreProcPerc.setFont(composite.getFont());
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.minimumWidth = 80;
        m_textPreProcPerc.setLayoutData(data);
        m_textPreProcPerc.addListener(SWT.Modify, this);
        label = new CLabel(compo, SWT.NONE);
        label.setMargins(0, 0, 0, 0);
        label.setText("% of total");
        label.setFont(composite.getFont());
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Progress percentages for post processing
        label = new CLabel(settingsGroup, SWT.NONE | SWT.RIGHT);
        label.setMargins(0, 0, 40, 0);
        label.setText("Post-processing activities: ");
        label.setFont(composite.getFont());
        compo = new Composite(settingsGroup, SWT.NONE);
        layout = new GridLayout(2, false);
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        compo.setLayout(layout);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalIndent = 0;
        compo.setLayoutData(data);
        m_textPostProcPerc = new Text(compo, SWT.BORDER | SWT.RIGHT);
        m_textPostProcPerc.setText("0");
        m_textPostProcPerc.setFont(composite.getFont());
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.minimumWidth = 80;
        m_textPostProcPerc.setLayoutData(data);
        m_textPostProcPerc.addListener(SWT.Modify, this);
        label = new CLabel(compo, SWT.NONE);
        label.setMargins(0, 0, 0, 0);
        label.setText("% of total");
        label.setFont(composite.getFont());
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Initial complex settings enabling/disabling
        boolean bComplex = m_generateComplexCode.getSelection();
        m_textPreProcPerc.setEnabled(bComplex);
        m_textPostProcPerc.setEnabled(bComplex);
    }

    /**
     * Tries to create the specified directory, if it does not exist yet.
     *
     * @param dir Directory to be created.
     *
     * @throws IOException Thrown, if the directory does not exist and could not be created.
     */
    private void prepareDirectory(final File dir) throws IOException {
    	if (dir.exists()) {
    		if (!dir.isDirectory()) {
    			throw new IOException("'" + dir + "' is not a directory. Cannot use it.");
    		}
    	}
    	else if (!dir.mkdirs()) {
    		throw new IOException("'" + dir + "' or one of its parent directories could not be directory. Cannot use it.");
    	}
    }

    /**
     * Tries to create a copy of the help files.
     *
     * @return Copies all help files into a temporary directory and returns the
     * path to them.
     *
     * @throws IOException Thrown, if help files could not be copied.
     */
    @SuppressWarnings("rawtypes")
	private URL makeHelpFilesAvailable() throws IOException {
    	URL url = null;
    	String strTempDir = System.getProperty("java.io.tmpdir");

    	if (strTempDir != null) {
    		// Create temp sub directory
    		File dirTemp = new File(strTempDir, "rdkitNodeTypes");
    		prepareDirectory(dirTemp);

    		// Copy all files from the wizards help directory
    		Enumeration e =
    			RDKitNodesWizardsPlugin.getDefault().getBundle().getEntryPaths("help/");

    		if (e != null) {
    			while (e.hasMoreElements()) {
	    			String strEntry = e.nextElement().toString();
	    			// Don't copy over any .svn stuff - basically we forbid all . files
	    			if (!strEntry.startsWith(".")) {
		    			URL fileOrig = RDKitNodesWizardsPlugin.getDefault().getBundle().getResource(strEntry);
		    			// Don't copy over any directories - we stick to a flat structure to keep it simple
		    			try {
		    				FileUtils.copyURLToFile(fileOrig, new File(dirTemp, strEntry.substring(strEntry.indexOf("/"))));
		    			}
		    			catch (Exception exc) {
		    				// Ignored
		    			}
	    			}
	    		}
    		}
			else {
				throw new IOException("No help resource files of found.");
			}

    		File fileStart = new File(dirTemp, "index.html");
    		if (fileStart.canRead()) {
    			url = fileStart.toURI().toURL();
    		}
    		else {
    			throw new IOException("The index.html file of the help cannot be read.");
    		}
    	}
    	else {
    		throw new IOException("No temp directory found to copy help resources.");
    	}

    	return url;
    }

    /**
     * Determines based on the user selection in the Eclipse navigation pane what
     * package the user has selected.
     *
     * @return Selected package or empty string, if unknown.
     */
    private String getSelectedPackage() {
        if (m_selection == null || m_selection.isEmpty()) {
            return "";
        }

        Object o = m_selection.getFirstElement();
        if (o instanceof IJavaElement) {
            if (o instanceof IPackageFragment) {
                return ((IPackageFragment)o).getElementName();
            } else {
                IJavaElement je = (IJavaElement)o;
                do {
                	je = je.getParent();
                }
                while (je != null && !(je instanceof IPackageFragment));

                return (je == null ? "" : je.getElementName());
            }
        }

        return "";
    }

    /**
     * Creates the combo box with the possible node types.
     *
     * @param parent The parent composite of the combo box.
     *
     * @return The created combo box
     */
    private Combo createCategoryCombo(final Composite parent) {

    	// Read all .template files from templates package and derive the template names
    	@SuppressWarnings("rawtypes")
		Enumeration enumResourcePath = RDKitNodesWizardsPlugin.getDefault().getBundle().
    		findEntries("/", "*.template", true);

    	Set<String> setTemplates = new HashSet<String>();

    	if (enumResourcePath != null) {
	    	while (enumResourcePath.hasMoreElements()) {
	    		String strPath = enumResourcePath.nextElement().toString();
	    		int index = strPath.indexOf("/templates/");
	    		if (index != -1) {
	    			int indexStart = index + 11;
	    			int indexEnd = strPath.indexOf("/", indexStart);
	    			if (indexEnd != -1) {
		    			String strTemplateName = strPath.substring(indexStart, indexEnd);
		    			if (!"complex".equals(strTemplateName)) {
		    				setTemplates.add(strTemplateName);
		    			}
	    			}
	    		}
	    	}
    	}

    	// Get a sorted list of templates
    	List<String> listTemplates = sort(setTemplates);
        for (String strType : DEPRECACTED_NODE_TYPES) {
        	listTemplates.remove(strType);
        }

        Combo typeCombo = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);

        for (String strType : listTemplates) {
        	typeCombo.add(strType);
        }

        typeCombo.select(0);

        return typeCombo;
    }

    /**
     * Creates the combo box with the possible node types. Uses the information
     * from the core factory defining the types.
     *
     * @param parent the parent composite of the combo box
     *
     * @return the created combo box
     */
    private Combo createProjectsCombo(final Composite parent) {
        Combo projectsCombo = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);

        int iDefault = -1;
        String strDefaultProject = "org.rdkit.knime.nodes";
        IJavaProject defaultProject = null;

        int i = 0;
        for (IProject project : RDKitNodesWizards.getProjectsInWorkspace()) {
            // unknown is just an internal type

        	String projectName = project.getName();

        	if (strDefaultProject.equals(projectName)) {
        		iDefault = i;
        		if (project instanceof IJavaElement) {
        			defaultProject = ((IJavaElement)project).getJavaProject();
        		} else {
        			defaultProject = JavaModelManager.getJavaModelManager()
                    	.getJavaModel().getJavaProject(project.getProject());
        		}
        	}

            projectsCombo.add(projectName);

            // Set default selection
            if (m_selection != null && !m_selection.isEmpty()) {
                IProject toCompare;
                Object o = m_selection.getFirstElement();
                if (o instanceof IJavaElement) {
                    m_currentJavaProject = ((IJavaElement)o).getJavaProject();
                    toCompare = m_currentJavaProject.getProject();
                } else if (o instanceof IResource) {
                    toCompare = ((IResource)o).getProject();
                    m_currentJavaProject =
                            JavaModelManager.getJavaModelManager()
                                    .getJavaModel().getJavaProject(toCompare);
                } else {
                    continue;
                }

                if (toCompare.equals(project)) {
                    projectsCombo.select(i);
                }
            }

            i++;
        }

        // If nothing is selected and our default was found, then select it
        if (projectsCombo.getSelectionIndex() == -1 && iDefault != -1) {
        	projectsCombo.select(iDefault);
        }

        if (m_currentJavaProject == null && defaultProject != null) {
        	m_currentJavaProject = defaultProject;
        }

        return projectsCombo;
    }

    /**
     * This checks the text fields after a modify event and sets the
     * error message if necessary. This calls <code>validatePage</code> to
     * actually validate the fields.
     *
     * @param event Event that triggers page validation.
     */
    @Override
    public void handleEvent(final Event event) {
        if (event.type != SWT.Modify) {
            return;
        }
        boolean valid = validatePage();
        setPageComplete(valid);
    }

    /**
     * Validates the page, e.g. checks whether the text fields contain valid
     * values.
     *
     * @return Returns true, if all information on page is correct. False otherwise.
     */
    protected boolean validatePage() {
    	// Check existing project setting
        if (getProjectName().trim().equals("")) { //$NON-NLS-1$
            setErrorMessage(null);
            setMessage("Please select an existing project.");
            return false;
        }

        // Check the node name
        String nodeName = m_textNodeName.getText();
        if (nodeName.trim().isEmpty()) {
            setErrorMessage(null);
            setMessage("Please provide a valid node name.");
            return false;
        }
        if ((!Character.isLetter(nodeName.charAt(0)))
                || (nodeName.charAt(0) != nodeName.toUpperCase().charAt(0))) {
            setErrorMessage("The node name must start with an uppercase letter.");
            return false;
        }

        String strClassName = getNodeClassName();
        for (int i = 0; i < strClassName.length(); i++) {
            char c = strClassName.charAt(i);
            if (!(i == 0 && Character.isJavaIdentifierStart(c)) &&
            	!(i > 0 && Character.isJavaIdentifierPart(c))) {
                setErrorMessage("The class name '" + strClassName + "' is invalid.");
                return false;
            }
        }

        // Check package name
        String basePackage = m_textBasePackage.getText();
        if (basePackage.length() == 0) {
            setErrorMessage(null);
            setMessage("Please provide a package name.");
            return false;
        }
        for (int i = 0; i < basePackage.length(); i++) {
            char c = basePackage.charAt(i);
            if (!(Character.isLowerCase(c) || Character.isDigit(c) || c == '.' || c == '_')) {
                setErrorMessage("The package name '" + basePackage
                        + "' is invalid.");
                return false;
            }
        }

        // Check for existing classes (naming conflict?)
        IProject project =
        		RDKitNodesWizards
                        .getProjectForName(getProjectName());
        String path =
                "src/" + m_textBasePackage.getText().trim().replace('.', '/')
                        + "/" + nodeName;
        IFile file = project.getFile(new Path(path + "NodeModel.java"));
        if (file.exists()) {
            setErrorMessage("A node with the given name exists already. Please provide another name or package.");
            return false;
        }

        // Check percentages for pre- and post processing
        try {
        	double dPrePerc = getPreProcessingPercentage();
        	double dPostPerc = getPostProcessingPercentage();

        	if (dPrePerc + dPostPerc > 1.0d) {
                setErrorMessage("The total of pre and post processing activities cannot be greater than 100%.");
            	return false;
        	}
        }
        catch (NumberFormatException exc) {
            setErrorMessage("Bad number format: " + exc.getMessage());
        	return false;
        }

        // Everything is ok so far
        setErrorMessage(null);
        setMessage(null);
        return true;
    }

    /**
     * Returns the selected project name.
     *
     * @return Selected project name. Empty string, if undefined.
     */
    public String getProjectName() {
        return m_comboExistingProjects == null ? "" : m_comboExistingProjects.getText().trim();
    }

    /**
     * Returns the specified node name (stump).
     *
     * @return Specified node name. Empty string, if undefined.
     */
    public String getNodeName() {
        return m_textNodeName == null ? "" : m_textNodeName.getText().trim();
    }

    /**
     * Returns the specified node name to be used as class name (stump without any spaces).
     * Prepends RDKit.
     *
     * @return Specified node name. Empty string, if undefined.
     */
    public String getNodeClassName() {
    	return getNodeClassName(true);
    }

    /**
     * Returns the specified node name to be used as class name (stump without any spaces).
     *
     * @param bPrependRdkit Set to true to prepend "RDKit ".
     *
     * @return Specified node name. Empty string, if undefined.
     */
     private String getNodeClassName(final boolean bPrependRdkit) {
    	String strEnteredName = getNodeName().trim();
    	StringBuilder sbClassName = new StringBuilder();
    	boolean bMakeUpperCase = true;

    	for (char ch : strEnteredName.toCharArray()) {
    		if (sbClassName.length() == 0 && Character.isJavaIdentifierStart(ch)) {
    			// Make the first character upper case anyway
    			sbClassName.append(Character.toUpperCase(ch));
    			bMakeUpperCase = false;
    		}
    		else if (sbClassName.length() > 0 && Character.isJavaIdentifierPart(ch)) {
    			// Make a character upper case only, if a character was found before that was eliminated
    			if (bMakeUpperCase) {
    				sbClassName.append(Character.toUpperCase(ch));
    				bMakeUpperCase = false;
    			}
    			else {
    				sbClassName.append(ch);
    			}
    		}
    		else {
    			// Eliminate and make next character upper case
    			bMakeUpperCase = true;
    		}
    	}

    	String strClassName = sbClassName.toString();

    	// Prepend RDKit to the class identifier
    	if (bPrependRdkit && !strClassName.toLowerCase().startsWith("rdkit")) {
    		strClassName = "RDKit" + strClassName;
    	}

        return strClassName;
    }

    /**
     * Returns the specified node name to be used as menu name (name with spaces inside).
     *
     * @return Specified node name. Empty string, if undefined.
     */
    public String getNodeMenuName() {
    	String strEnteredName = getNodeName().trim();
    	String strMenuName = null;

    	// Only work on the name, if the user entered an identifier (which would not contain spaces)
    	if (strEnteredName.indexOf(" ") == -1) {
    		strMenuName = RDKitNodesWizardsPage.generateFriendlyName(strEnteredName);
    	}
    	else {
    		strMenuName = strEnteredName;
    	}

    	// Prepend RDKit to the menu name
    	if (!strMenuName.toLowerCase().startsWith("rdkit ")) {
    		strMenuName = "RDKit " + strMenuName;
    	}

        return strMenuName;
    }

    /**
     * Returns the specified node target package.
     *
     * @return Specified node target package. Empty string, if undefined.
     */
    public String getNodePackage() {
        return m_textBasePackage == null ? "" : m_textBasePackage.getText().trim();
    }

    /**
     * Returns the specified node type.
     *
     * @return Specified node type. Empty string, if undefined.
     */
    public String getNodeType() {
        return m_comboNodeType == null ? "" : m_comboNodeType.getText().trim();
    }

    /**
     * Returns the specified node description.
     *
     * @return Specified node description. Empty string, if undefined.
     */
    public String getNodeDescription() {
        return m_textDescription == null ? "" : m_textDescription.getText().trim();
    }

    /**
     * Returns the specified node vendor.
     *
     * @return Specified node vendor. Empty string, if undefined.
     */
    public String getNodeVendor() {
        return m_textVendor == null ? "" : m_textVendor.getText().trim();
    }

    /**
     * Returns the entered percentage value as double (devided by 100) for
     * pre processing actions.
     *
     * @return Value between 0 and 1.
     *
     * @throws NumberFormatException Thrown, if the user entered an invalid value in the
     * 		text field for the pre-processing percentage.
     */
    public double getPreProcessingPercentage() throws NumberFormatException {
    	double dPerc = 0.0d;

    	if (m_textPreProcPerc != null && m_textPreProcPerc.getEnabled()) {
   			dPerc = Double.parseDouble(m_textPreProcPerc.getText().trim()) / 100.0d;
   			if (dPerc < 0) {
   				throw new NumberFormatException("Percentage of pre-processing activities must be greater than or equal to 0%.");
   			}
   			if (dPerc > 1) {
   				throw new NumberFormatException("Percentage of pre-processing activities must be lower than or equal to 100%.");
   			}
    	}

    	return dPerc;
    }

    /**
     * Returns the entered percentage value as double (devided by 100) for
     * pre processing actions.
     *
     * @return Value between 0 and 1.
     *
     * @throws NumberFormatException Thrown, if the user entered an invalid value in the
     * 		text field for the post-processing percentage.
     */
    public double getPostProcessingPercentage() throws NumberFormatException {
    	double dPerc = 0.0d;

    	if (m_textPostProcPerc != null && m_textPostProcPerc.getEnabled()) {
   			dPerc = Double.parseDouble(m_textPostProcPerc.getText().trim()) / 100.0d;
   			if (dPerc < 0) {
   				throw new NumberFormatException("Percentage of post-processing activities must be greater than or equal to 0%.");
   			}
   			if (dPerc > 1) {
   				throw new NumberFormatException("Percentage of post-processing activities must be lower than or equal to 100%.");
   			}
    	}

    	return dPerc;
    }

    /**
     * Returns if the developer allows the code to be optimized for parallel processing.
     *
     * @return Flag for parallel processing support. False, if undefined.
     */
    public boolean isParallelProcessingSupported() {
        return m_allowParallelProcessing == null ? false : m_allowParallelProcessing.getSelection();
    }

    /**
     * Returns if the developer wants to generate more complex code for the new node.
     *
     * @return Flag for more complex code. False, if undefined.
     */
    public boolean isGenerateComplexCode() {
        return m_generateComplexCode == null ? false : m_generateComplexCode.getSelection();
    }

    //
    // Private Static Methods
    //

	/**
	 * Sorts the specified string list. O(N) is n log(n).
	 *
	 * @param set String set containing the elements to be sorted. Can be
	 *            <code>null</code>.
	 *
	 * @return Sorted string list or <code>null</code> if <code>null</code> was
	 *         passed in.
	 */
	private static List<String> sort(final Set<String> set) {
		List<String> listSorted = null;

		if (set != null) {
			listSorted = new ArrayList<String>();
			for (String str : set){
				listSorted.add(getIndexForSortedInsert(listSorted, str), str);
			}
		}

		return listSorted;
	}

	/**
	 * Determines for a sorted list and an object, what index the string
	 * could be inserted. Note: This method just returns the index, but does not
	 * insert the string.
	 *
	 * @param listSorted
	 *            Sorted string list. Can be <code>null</code>.
	 * @param item
	 *            String to be inserted. Can be <code>null</code>.
	 *
	 * @return Index for string insertion.
	 */
	private static int getIndexForSortedInsert(final List<String> listSorted, final String item) {
		if (listSorted == null || item == null) {
			return 0;
		}

		int low = 0;
		int high = listSorted.size() - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			String midVal = listSorted.get(mid);
			int cmp = midVal.compareToIgnoreCase(item);

			if (cmp < 0) {
				low = mid + 1;
			}
			else if (cmp > 0) {
				high = mid - 1;
			}
			else {
				return mid; // key found
			}
		}

		return low; // key not found.
	}

	/**
	 * Generates a friendly and easily readable name based on the passed in name.
	 * Replaces for instance underscores with spaces, corrects lower and upper
	 * cases when necessary.
	 *
	 * @param name A name of something. Can be null.
	 *
	 * @return A friendly name. Is empty when null was passed in.
	 */
	public static String generateFriendlyName(final String name) {
		String workName = "";

		if (name != null) {
			workName = name.trim();

			// Don't touch, if length is <= 2 (usually a variable like x, y, z)
			if (workName.length() > 2) {
				workName = workName.replaceAll("_", " ");
				workName = workName.replaceAll("\\s", " ");

				final StringBuilder sbFriendlyName = new StringBuilder();

				int iCountUpperCase = 0;
				boolean bWasSpace = false;
				char[] chars = workName.toCharArray();
				chars[0] = Character.toUpperCase(chars[0]);

				for (char c : chars) {
					final boolean bIsUpperCase = Character.isUpperCase(c);
					final boolean bIsLowerCase = Character.isLowerCase(c);

					if (bIsUpperCase && iCountUpperCase == 0) {
						sbFriendlyName.append(" ");
					}

					if (bIsLowerCase && iCountUpperCase > 1) {
						sbFriendlyName.insert(sbFriendlyName.length() - 1, " ");
					}

					if (bWasSpace) {
						c = Character.toUpperCase(c);
					}

					sbFriendlyName.append(c);

					if (bIsUpperCase) {
						iCountUpperCase++;
					} else {
						iCountUpperCase = 0;
					}

					bWasSpace = (c == ' ');
				}

				workName = sbFriendlyName.toString().trim();

				// If more than one whitespace was added in the middle, reduce
				// multiple to single space
				workName = workName.trim().replaceAll("\\s+", " ");
			}
		}

		return workName;
	}
}
