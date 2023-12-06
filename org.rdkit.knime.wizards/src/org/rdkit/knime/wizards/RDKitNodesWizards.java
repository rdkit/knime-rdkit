/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2012-2023
 * Novartis Pharma AG, Switzerland
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Wizard for creating a new RDKit Node, containing a "stub implementation"
 * of NodeModel and Dialog.
 *
 * @author Manuel Schwarze
 * @author based on Work from Florian Georg, University of Konstanz
 * @author based on Work from Christoph Sieb, University of Konstanz
 */
@SuppressWarnings("restriction")
public class RDKitNodesWizards extends Wizard implements INewWizard {

	//
	// Constants
	//

	/** End of line character. */
    private static final String EOL = System.getProperty("line.separator");

    //
    // Members
    //

    /** The wizard page with all GUI elements. */
    private RDKitNodesWizardsPage m_page;

    /**
     * The current selection in the navigation tree. Based on this we can find out
     * what project is currently open.
     */
    private ISelection m_selection;

    //
    // Constructor
    //

    /**
     * Creates a new wizard to construct RDKit Nodes.
     */
    public RDKitNodesWizards() {
        super();

        setNeedsProgressMonitor(true);
    }

    //
    // Public Methods
    //

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench,
            final IStructuredSelection selection) {
        m_selection = selection;
    }

    /**
     * Creates and adds the new page to the wizard.
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        m_page = new RDKitNodesWizardsPage(m_selection);
        addPage(m_page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        final String projectName = m_page.getProjectName();
        final String nodeType = m_page.getNodeType();
        final Properties substitutions = m_page.getSubstitutionMap();
        final boolean bComplex = m_page.isGenerateComplexCode();

        IRunnableWithProgress runMe = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(projectName, nodeType, substitutions, bComplex, monitor);
                }
                catch (CoreException e) {
                    throw new InvocationTargetException(e);
                }
                finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(false, false, runMe);
        }
        catch (InterruptedException e) {
            MessageDialog.openError(getShell(), "Error", e.getMessage());
            logError(e);
            return false;
        }
        catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException
                    .getMessage());
            logError(e);
            return false;
        }
        return true;
    }

    /**
     * Logs an error.
     *
     * @param e the exception
     */
    private void logError(final Exception e) {
        RDKitNodesWizardsPlugin.getDefault().getLog().log(
        	new Status(IStatus.ERROR, RDKitNodesWizardsPlugin.ID, 0, e.getMessage(), e));
    }

    /**
     * Determine if the project with the given name is in the current workspace.
     *
     * @param projectName String the project name to check
     * @return boolean true if the project with the given name is in this
     *         workspace
     */
    static boolean isProjectInWorkspace(final String projectName) {
        IProject project = getProjectForName(projectName);
        if (project != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the project for the given project name. <code>null</code> if the
     * project is not in the workspace.
     *
     * @param projectName String the project name to check
     * @return {@link IProject} if the project with the given name is in this
     *         workspace, <code>null</code> otherwise
     */
    static IProject getProjectForName(final String projectName) {
        if (projectName == null) {
            return null;
        }
        IProject[] workspaceProjects = getProjectsInWorkspace();
        for (int i = 0; i < workspaceProjects.length; i++) {
            if (projectName.equals(workspaceProjects[i].getName())) {
                return workspaceProjects[i];
            }
        }
        return null;
    }

    /**
     * Retrieve all the projects in the current workspace.
     *
     * @return IProject[] array of IProject in the current workspace
     */
    static IProject[] getProjectsInWorkspace() {
        return IDEWorkbenchPlugin.getPluginWorkspace().getRoot().getProjects();

    }

    /**
     * The worker method. It will create all necessary files for the wizard's
     * configurations.
     *
     * @param projectName Name of the project.
     * @param nodeType Type of node, which influences what template to use.
     * @param substitutions Map with substitutions to be applied to the temple file.
     * @param bComplex True, if complex template shall be used.
     * @param monitor Progress monitor.
     *
     * @throws CoreException Thrown, if classes or resources could not be created.
     */
    private void doFinish(final String projectName, final String nodeType,
            final Properties substitutions, final boolean bComplex,
            final IProgressMonitor monitor) throws CoreException {

    	// Set the current year in the substitutions
        Calendar cal = new GregorianCalendar();
        substitutions.setProperty(RDKitNodesWizardsPage.SUBST_CURRENT_YEAR,
                Integer.toString(cal.get(Calendar.YEAR)));

        String packageName =
                substitutions.getProperty(
                		RDKitNodesWizardsPage.SUBST_BASE_PACKAGE,
                        "knime.dummy");
        String nodeName =
                substitutions.getProperty(
                		RDKitNodesWizardsPage.SUBST_NODE_NAME, "Dummy");

        // Find reference to the selected project
        IContainer container = getProjectForName(projectName);

        // 1. Extend the plugin.xml with the new node extension entry
        addNodeExtensionToPlugin((IProject)container, packageName + "."
                + nodeName + "NodeFactory");

        // 2. Add line for node extention to the manifest file
        addNodeExtensionToManifest((IProject)container, packageName);

        // 3. create src/bin folders
        final IFolder srcContainer = container.getFolder(new Path("src"));
        final IFolder binContainer = container.getFolder(new Path("bin"));
        if (!srcContainer.exists()) {
            monitor.beginTask("Creating src folder ....", 2);
            srcContainer.create(true, true, monitor);
        }
        if (!binContainer.exists()) {
            monitor.beginTask("Creating bin folder ....", 2);
            binContainer.create(true, true, monitor);
        }

        monitor.worked(2);

        // 4. create package (folders)
        String[] pathSegments = packageName.split("\\.");
        monitor.beginTask("Creating package structure ....",
                pathSegments.length);
        IFolder packageContainer = container.getFolder(new Path("src"));
        for (int i = 0; i < pathSegments.length; i++) {
            packageContainer =
                    packageContainer.getFolder(new Path(pathSegments[i]));
            if (!packageContainer.exists()) {
                packageContainer.create(true, true, monitor);
            }
            monitor.worked(1);
        }

        // 5. create node factory
        monitor.beginTask("Creating node factory ....", 1);
        createFile(nodeName + "NodeFactory.java",
        		getTemplatePath(nodeType, bComplex, "NodeFactory.template"),
                substitutions, monitor, packageContainer);

        monitor.worked(1);

        // 6. create node model
        monitor.beginTask("Creating node model ....", 1);
        final IFile nodeModelFile = createFile(nodeName + "NodeModel.java",
        		getTemplatePath(nodeType, bComplex, "NodeModel.template"),
                substitutions, monitor, packageContainer);

        monitor.worked(1);

        // 7. create node dialog
        monitor.beginTask("Creating node dialog ....", 1);
        createFile(nodeName + "NodeDialog.java",
        		getTemplatePath(nodeType, bComplex, "NodeDialog.template"),
                substitutions, monitor, packageContainer);
        monitor.worked(1);

        // 9. create node description xml file
        monitor.beginTask("Creating node description xml file ....", 1);
        createFile(nodeName + "NodeFactory.xml",
        		getTemplatePath(nodeType, bComplex, "NodeDescriptionXML.template"),
                substitutions, monitor, packageContainer);

        monitor.worked(1);

        // 10. create package.html file
        if (!packageContainer.getFile("package.html").exists()) {
            monitor.beginTask("Creating package.html file ....", 1);
            createFile("package.html",
            		getTemplatePath(nodeType, bComplex, "packageHTML.template"),
            		substitutions, monitor, packageContainer);
        }

        monitor.worked(1);

        // 11. copy additional files (icon, ...)
        if (!packageContainer.getFile("default.png").exists()) {
            monitor.beginTask("Adding additional files....", 2);
            IFile defIcon = packageContainer.getFile("default.png");

            // copy default.png
            URL url =
                    RDKitNodesWizardsPlugin.getDefault().getBundle().getEntry(
                    	getTemplatePath(nodeType, bComplex, "default.png"));
            try {
                defIcon.create(url.openStream(), true, monitor);
            } catch (IOException e1) {
                e1.printStackTrace();
                throwCoreException(e1.getMessage());
            }
        }

        monitor.worked(2);

        // open the model file in the editor
        monitor.setTaskName("Opening file for editing...");
        getShell().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchPage page =
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getActivePage();
                try {
                    IDE.openEditor(page, nodeModelFile, true);
                } catch (PartInitException e) {
                }
            }
        });
        monitor.worked(1);
    }

    /**
     * Adds a line to the manifest file of the plugin to specify the new node package.
     *
     * @param project Project of the plugin the node is getting added to. Must not be null.
     * @param packageName Package name of the node.
     *
     * @throws CoreException Thrown, if manifest could not be changed.
     */
    private static void addNodeExtensionToManifest(final IProject project, final String packageName)
    	throws CoreException {
    	String strExistCheck1 = packageName + ",";
    	String strExistCheck2 = packageName + ";";

    	// Read and manipulate manifest file content
    	File manifest = project.getFile("META-INF/MANIFEST.MF").getLocation().toFile();
        StringBuilder text = new StringBuilder();
        String NL = System.getProperty("line.separator");
        Scanner scanner = null;

        try {
        	boolean inBlock = false;
        	boolean done = false;
            scanner = new Scanner(new FileInputStream(manifest), "UTF-8");

            while (scanner.hasNextLine()){
            	String line = scanner.nextLine();
            	String lineTrimmed = line.trim();
            	if (!done) {
            		if (!inBlock && lineTrimmed.startsWith("Export-Package:")) {
            			inBlock = true;
	            	}
	            	if (inBlock) {
	            		if (line.indexOf(strExistCheck1) != -1 ||
	            			line.indexOf(strExistCheck2) != -1) {
	            			done = true;
	            		}
	            		else {
	            			if (!lineTrimmed.endsWith(",")) {
	            				line += "," + NL + " " + packageName;
	            				done = true;
	            			}
	            		}
	            	}
            	}

            	text.append(line + NL);
            }

            if (!inBlock) {
            	text.append("Export-Package: " + packageName + NL);
            }
        }
		catch (IOException exc) {
			throwCoreException("MANIFEST.MF file not found. " + exc.getMessage());
		}
        finally{
            if (scanner != null) {
                scanner.close();
            }
		}

        // Save manifest file
        FileOutputStream out = null;

        try {
        	out = new FileOutputStream(manifest, false);
        	out.write(text.toString().getBytes());
        }
		catch (IOException exc) {
			throwCoreException("MANIFEST.MF file could not be written. " + exc.getMessage());
		}
        finally{
        	try {
        	    if (out != null) {
        	        out.close();
        	    }
			}
			catch (IOException e) {
				// Empty by purpose
			}
		}

        project.getFile("META-INF/MANIFEST.MF").refreshLocal(1, null);
    }

    /**
     * Adds a line to the plugin.xml file of the plugin to specify the new node with
     * its factory class.
     *
     * @param project Project of the plugin the node is getting added to. Must not be null.
     * @param factoryClassName Factory class name of the node.
     */
    private static void addNodeExtensionToPlugin(final IProject project,
            final String factoryClassName) {
        project.getFile("plugin.xml").exists();
        File pluginXml = project.getFile("plugin.xml").getLocation().toFile();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(pluginXml);
            NodeList rootElements = document.getChildNodes();
            Node rootElement = null;
            for (int i = 0; i < rootElements.getLength(); i++) {
                Node element = rootElements.item(i);
                if (element.getNodeName().equals("plugin")) {
                    rootElement = element;
                    break;
                }
            }
            if (rootElement == null) {
                throw new RuntimeException(
                        "Project does not contain a valid plugin.xml");
            }

            NodeList children = rootElement.getChildNodes();
            Node nodeExtensionElement = null;
            for (int i = 0; i < children.getLength(); i++) {
                Node element = children.item(i);
                if (element.getNodeName().equals("extension")) {
                    NamedNodeMap attributes = element.getAttributes();
                    if (attributes.getNamedItem("point").getNodeValue().equals(
                            "org.knime.workbench.repository.nodes")) {
                        nodeExtensionElement = element;
                        break;
                    }

                }
            }
            // If a node extension point did not exist, create one
            if (nodeExtensionElement == null) {
                nodeExtensionElement = document.createElement("extension");
                Attr pointAttr = document.createAttribute("point");
                pointAttr.setValue("org.knime.workbench.repository.nodes");
                nodeExtensionElement.appendChild(pointAttr);
                rootElement.appendChild(nodeExtensionElement);
            }

            // Now create a new node element
            Node newNodeElement = document.createElement("node");
            ((Element)newNodeElement).setAttribute("category-path", "/community/rdkit");
            ((Element)newNodeElement).setAttribute("expert-flag", "false");
            ((Element)newNodeElement).setAttribute("factory-class",
                    factoryClassName);
            ((Element)newNodeElement).setAttribute("id", factoryClassName);
            nodeExtensionElement.appendChild(newNodeElement);

            // Prepare the DOM document for writing
            document.normalize();
            document.normalizeDocument();
            DOMSource source = new DOMSource(document);

            // Prepare the output file
            Result result = new StreamResult(pluginXml);

            // Write the DOM document to the file
            Transformer xformer =
                    TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.METHOD, "xml");
            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.transform(source, result);
            // XMLSerializer serializer = new XMLSerializer();
            // serializer.setOutputCharStream(new FileWriter(pluginXml));
            // serializer.serialize(document);
        } catch (Exception pce) {
            throw new RuntimeException(pce);
        }
    }

    /**
     * Determines the path to a template for a certain node type and complexity.
     * Templates are stored in a hierarchical folder structure, which contains
     * one level 1 the node type, and on level 2 more complex templates.
     * When requesting a template path for a particular template this method will
     * start at the deepest point of the selected branch and tries to find a
     * template. If not found, it goes one level up until it finds it or
     * (if the template does not exist) throws an exception.
     *
     * @param strNodeType Node type.
     * @param bComplex True, if the complex version is requested. False otherwise.
     * @param strTemplate Name of the template file.
     *
     * @return Path to the template file.
     *
     * @throws CoreException Thrown, if the template could not be found.
     */
    private String getTemplatePath(final String strNodeType, final boolean bComplex, final String strTemplate)
    	throws CoreException {
    	File path = new File(strTemplate);
    	if (bComplex) {
    		path = new File("templates/" + strNodeType + "/complex/" + strTemplate);
    	}
    	else {
    		path = new File("templates/" + strNodeType + "/" + strTemplate);
    	}
    	String pathString = path.toString().replace("\\", "/");

    	RDKitNodesWizardsPlugin.getDefault().getLog().log(
                new Status(IStatus.INFO, RDKitNodesWizardsPlugin.getDefault()
                        .getBundle().getSymbolicName(), IStatus.OK, "Trying to use template '" + pathString + "'", null));

    	// Walk up the hierarchy until we find a matching template
    	while (RDKitNodesWizardsPlugin.getDefault().getBundle().getEntry(
    			pathString) == null && !strTemplate.equals(pathString)) {
    		File parentDir = path.getParentFile().getParentFile();
    		path = new File(parentDir, strTemplate);
    		pathString = path.toString().replace("\\", "/");

        	RDKitNodesWizardsPlugin.getDefault().getLog().log(
                    new Status(IStatus.INFO, RDKitNodesWizardsPlugin.getDefault()
                            .getBundle().getSymbolicName(), IStatus.OK, "Trying to use template '" + pathString + "'", null));
    	}

    	if (strTemplate.equals(path.toString())) {
    		throwCoreException("Template " + strTemplate + " does not exist.");
    	}

    	RDKitNodesWizardsPlugin.getDefault().getLog().log(
                new Status(IStatus.INFO, RDKitNodesWizardsPlugin.getDefault()
                        .getBundle().getSymbolicName(), IStatus.OK, "Found template '" + pathString + "'", null));

    	return pathString;
    }

    /**
     * Creates a new file based on a specific template.
     *
     * @param filename Filename of the target file to be created. Must not be null.
     * @param templateFile Template filename path. Must not be null.
     * @param substitutions Property map with key value pairs to substitute data in the template.
     * 		Must not be null.
     * @param monitor Used to show progress. Can be null.
     * @param container Target container where the file shall be created.
     *
     * @return A file resource.
     *
     * @throws CoreException Thrown, if creation of the file failed.
     *
     */
    private IFile createFile(final String filename, final String templateFile,
            final Properties substitutions, final IProgressMonitor monitor,
            final IContainer container) throws CoreException {
        final IFile file = container.getFile(new Path(filename));
        try {
            InputStream stream =
                    openSubstitutedContentStream(templateFile, substitutions);
            if (file.exists()) {
                file.setContents(stream, true, true, monitor);
            } else {
                file.create(stream, true, monitor);
            }
            stream.close();
        } catch (IOException e) {
            throwCoreException(e.getMessage());
        }
        return file;
    }

    /**
     * We will initialize file contents with an empty String.
     *
     * @param templateFileName Template filename path. Must not be null.
     * @param substitutions Property map with key value pairs to substitute data in the template.
     * 		Must not be null.
     *
     * @return Input stream to read the data to be written into a new file.
     *
     * @throws CoreException Thrown, if input stream could not be created.
     */
    private InputStream openSubstitutedContentStream(
            final String templateFileName, final Properties substitutions)
            throws CoreException {
        URL url =
                RDKitNodesWizardsPlugin.getDefault().getBundle().getEntry(
                        templateFileName);
        String contents = "";
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            StringBuffer buf = new StringBuffer();
            String waitingForCloseTag = null;

            while ((line = reader.readLine()) != null) {
            	// Replace placeholders
                for (Iterator<Object> it = substitutions.keySet().iterator();
	                it.hasNext();) {
	                String key = (String)it.next();
	                String sub = substitutions.getProperty(key, "??" + key + "??");
	                line = line.replaceAll(key,
	                        Matcher.quoteReplacement(sub));
	            }

                if (waitingForCloseTag == null) {
                	if (line.startsWith("<REMOVE IF(")) {
                		// Start tag found for remove if condition - check condition
                		String condition = line.substring(11, line.lastIndexOf(")>"));
                		int indexOperator = condition.indexOf("==");
                		String left = condition.substring(0, indexOperator).trim();
                		String right = condition.substring(indexOperator + 2).trim();
                		if (left.length() > 0 && left.equals(right)) {
                			waitingForCloseTag = "</REMOVE IF(" + condition + ")>";
                		}
                	}
                	else if (line.startsWith("</REMOVE IF(")) {
                		// Ignore this line
                	}
                	else {
                		// Append line normally
                    	buf.append(line).append(EOL);
                	}
                }
                else if (waitingForCloseTag.equals(line.trim())) {
                	waitingForCloseTag = null;
                }
            }

            reader.close();
            contents = buf.toString();
        } catch (Exception e) {
            logError(e);
            throwCoreException("Can't process template file: url=" + url
                    + " ;file=" + templateFileName);
        }

        return new ByteArrayInputStream(contents.getBytes());
    }

    /**
     * Prepares and throws a CoreException with the specified message.
     *
     * @param message Message for the exception.
     *
     * @throws CoreException Always thrown with the specified message. It will have
     * 		an ERROR severity status logged for the RDKitNodesWizardPlugin ID.
     */
    private static void throwCoreException(final String message) throws CoreException {
        IStatus status =
                new Status(IStatus.ERROR, RDKitNodesWizardsPlugin.ID,
                        IStatus.OK, message, null);
        throw new CoreException(status);
    }
}
