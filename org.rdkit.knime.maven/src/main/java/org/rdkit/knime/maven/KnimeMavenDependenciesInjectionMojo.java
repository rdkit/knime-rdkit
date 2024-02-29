/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C)2024
 * Novartis Pharma AG, Switzerland
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
 * ---------------------------------------------------------------------
 */
package org.rdkit.knime.maven;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This Maven plugin scans a folder that contains JAR files that were fetched
 * and updated before and makes changes in the MANIFEST.MF and build.properties
 * files to specify them correctly. The folder that contains the JAR files is
 * used to find out which JAR files exist and to find the entries of old
 * references in the MANIFEST.MF file and build.properties file to remove them
 * before adding all new entries.
 *
 * @author Manuel Schwarze, Novartis Biomedical Research
 */
@Mojo(name = "knime-maven-dependencies-injection", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class KnimeMavenDependenciesInjectionMojo extends AbstractMojo {

	//
	// Members
	//

	/**
	 * The Maven project object handed over by the Maven process.
	 */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	/**
	 * Mandatory: A comma-separated list of directories with JAR files (must be relative to
	 * project folder)
	 */
	@Parameter(property = "libraryDirectories", required = true)
	String libraryDirectories;

	/**
	 * Optional: The project directory from which to look for META.INF/MANIFEST.MF and
	 * build.properties file as well as library directories.
	 */
	@Parameter(property = "projectDirectory")
	String projectDirectory;

	//
	// Constructors
	//

	/**
	 * Creates a new plugin instance with injected settings (used and done by Maven).
	 */
	public KnimeMavenDependenciesInjectionMojo() {
		super();
	}

	/**
	 * Creates a new plugin instance.
	 * 
	 * @param projectDirectory Project directory to work in. The project directory must contain
	 *      META-INF/MANIFEST.MF and build.properties as well as the library directories.
	 * @param libraryDirectories Comma-separated list of library directories relative to the project folder.
	 */
	public KnimeMavenDependenciesInjectionMojo(String projectDirectory, String libraryDirectories) {
		this.project = null;
		this.projectDirectory = projectDirectory;
		this.libraryDirectories = libraryDirectories;
	}

	//
	// Public Methods
	//

	/**
	 * Updates the MANIFEST.MF and build.properties files by removing old references JAR files from the 
	 * lib folder and adding all new JAR files that are now contained in the lib folder.
	 */
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Starting Dependencies Injection Plugin with projectDirectory=" + this.projectDirectory + " and libraryDirectories=" + this.libraryDirectories);
		this.projectDirectory = verifyProjectDirectory();
		getLog().info("Running Dependencies Injection Plugin for project directory " + this.projectDirectory);
		
		String[] arrLibraryDirectories = libraryDirectories.split(",");
		getLog().info("Processing JAR files in the following directories: " + Arrays.toString(arrLibraryDirectories));
		
		Set<String> setJarFiles = findAllJarFiles(projectDirectory, arrLibraryDirectories);
		
		adaptManifest(projectDirectory + "/META-INF/MANIFEST.MF", arrLibraryDirectories, setJarFiles);
		adaptBuildProperties(projectDirectory + "/build.properties", arrLibraryDirectories, setJarFiles);	
	}
	
	//
	// Protected Methods
	//
	
	/**
	 * Adapts the Manifest file that must be available as META-INF/MANIFEST.MF file within the configured project directory.
	 *  
	 * @param strManifestPath Path to the manifest file.
	 * @param arrLibraryDirectories Array of library directories relative to the project directory.
	 * @param setJarFiles Set of JAR files that were found in all library directories. Their path names
	 * 		can be absolute or relative to the configured project directory.
	 * 
	 * @throws MojoExecutionException Thrown, if the manifest file could not be processed properly.
	 */
	protected void adaptManifest(String strManifestPath, String[] arrLibraryDirectories, Set<String> setJarFiles) throws MojoExecutionException {
		String manifestContent = readManifest(strManifestPath);
		
		// Change Bundle-ClassPath
		Manifest manifest = verifyManifest(manifestContent, "Unable to parse manifest file '" + projectDirectory + "/META-INF/MANIFEST.MF'.");
		Point pBundleClassPathRange = getBlockStartAndEndPosition(manifestContent, "Bundle-ClassPath", manifest.getMainAttributes().keySet());
		getLog().info("Found 'Bundle-ClassPath' attribute from position " + pBundleClassPathRange.x + " to " + pBundleClassPathRange.y);
		String strBundleClassPath = manifestContent.substring(pBundleClassPathRange.x, pBundleClassPathRange.y);
		String strBundleClassPathModified = modifyContent("Bundle-ClassPath:", strBundleClassPath, arrLibraryDirectories, setJarFiles, ",");		
		String manifestContentModified = manifestContent.substring(0, pBundleClassPathRange.x) +
					strBundleClassPathModified + 
					manifestContent.substring(pBundleClassPathRange.y);
		verifyManifest(manifestContentModified, "Modified manifest file '" + projectDirectory + "/META-INF/MANIFEST.MF' got corrupted and will not be saved.");
		
		if (manifestContent.equals(manifestContentModified)) {
			getLog().info("META-INF/MANIFEST.MF was not changed");
		}
		else {
			getLog().info("META-INF/MANIFEST.MF was changed");
			getLog().debug("Old:\n" + manifestContent);
			getLog().debug("New:\n" + manifestContentModified);
			writeManifest(strManifestPath, manifestContentModified);		
		}
	}
	
	/**
	 * Adapts the build.properties file that must be available within the configured project directory.
	 *  
	 * @param strBuildPropertiesPath Path to the build.properties file.
	 * @param arrLibraryDirectories Array of library directories relative to the project directory.
	 * @param setJarFiles Set of JAR files that were found in all library directories. Their path names
	 * 		can be absolute or relative to the configured project directory.
	 * 
	 * @throws MojoExecutionException Thrown, if the build.properties file could not be processed properly.
	 */
	protected void adaptBuildProperties(String strBuildPropertiesPath,
			String[] arrLibraryDirectories, Set<String> setJarFiles) throws MojoExecutionException {
		String buildPropertiesContent = readBuildProperties(strBuildPropertiesPath);
		String buildPropertiesContentModified = buildPropertiesContent;
		
		// Change bin.inclues 
		Point pBinIncludesRange = getBlockStartAndEndPosition(buildPropertiesContentModified, "bin.includes", null);
		getLog().info("Found 'bin.includes' attribute from position " + pBinIncludesRange.x + " to " + pBinIncludesRange.y);
		String strBinIncludes = buildPropertiesContentModified.substring(pBinIncludesRange.x, pBinIncludesRange.y);
		String strBinIncludesModified = modifyContent("bin.includes =", strBinIncludes, arrLibraryDirectories, setJarFiles, ",\\");
		buildPropertiesContentModified = buildPropertiesContentModified.substring(0, pBinIncludesRange.x) +
				strBinIncludesModified + 
					buildPropertiesContentModified.substring(pBinIncludesRange.y);
		
		if (strBinIncludes.equals(strBinIncludesModified)) {
			getLog().info("build.properties - bin.includes was not changed");
		}
		
		// Change jars.extra.classpath
		Point pJarsExtraClasspathRange = getBlockStartAndEndPosition(buildPropertiesContentModified, "jars.extra.classpath", null);
		getLog().info("Found 'jars.extra.classpath' attribute from position " + pJarsExtraClasspathRange.x + " to " + pJarsExtraClasspathRange.y);
		String strJarsExtraClasspath = buildPropertiesContentModified.substring(pJarsExtraClasspathRange.x, pJarsExtraClasspathRange.y);
		String strJarsExtraClasspathModified = modifyContent("jars.extra.classpath =", strJarsExtraClasspath, arrLibraryDirectories, setJarFiles, ",\\");
		buildPropertiesContentModified = buildPropertiesContentModified.substring(0, pJarsExtraClasspathRange.x) +
				strJarsExtraClasspathModified + 
					buildPropertiesContentModified.substring(pJarsExtraClasspathRange.y);
		
		if (strJarsExtraClasspath.equals(strJarsExtraClasspathModified)) {
			getLog().info("build.properties - jars.extra.classpath was not changed");
		}

		if (buildPropertiesContent.equals(buildPropertiesContentModified)) {
			getLog().info("build.properties was not changed");
		}
		else {
			getLog().info("build.properties was changed");
			getLog().debug("Old:\n" + buildPropertiesContent);
			getLog().debug("New:\n" + buildPropertiesContentModified);
			writeBuildProperties(strBuildPropertiesPath, buildPropertiesContentModified);
		}
	}

	/**
	 * Determines the base / project directory and verifies that all files are
	 * present and have proper permissions.
	 * 
	 * @return Project directory.
	 * 
	 * @throws MojoExecutionException Thrown, if some pre-requisite is missing for
	 *                                running the task properly.
	 */
	protected String verifyProjectDirectory() throws MojoExecutionException {
		String projectDirectory = this.projectDirectory;

		// If Maven project is available use the base directory, unless user defined it as parameter
		if (project != null && (projectDirectory == null || projectDirectory.trim().isEmpty())) {
			File baseDir = project.getBasedir();
			if (baseDir == null) {
				throw new MojoExecutionException("No base directory set by Maven.");
			}
			projectDirectory = baseDir.getAbsolutePath();
		}

		if (projectDirectory == null) {
			throw new MojoExecutionException("No project directory set.");
		}

		File projectDir = new File(projectDirectory);
		if (!projectDir.isDirectory()) {
			throw new MojoExecutionException("Project directory '" + projectDir + "' is not a directory.");
		}
		if (!projectDir.canRead()) {
			throw new MojoExecutionException("Project directory '" + projectDir + "' has no read permission.");
		}

		File manifest = new File(projectDirectory + "/META-INF/MANIFEST.MF");
		if (!manifest.isFile()) {
			throw new MojoExecutionException("Manifest file '" + manifest + "' not found or not a file.");
		}
		if (!manifest.canRead()) {
			throw new MojoExecutionException("Manifest file '" + manifest + "' has no read permission.");
		}
		if (!manifest.canWrite()) {
			throw new MojoExecutionException("Manifest file '" + manifest + "' has no write permission.");
		}

		File buildProperties = new File(projectDirectory + "/build.properties");
		if (!buildProperties.isFile()) {
			throw new MojoExecutionException(
					"build.properties file '" + buildProperties + "' not found or not a file.");
		}
		if (!buildProperties.canRead()) {
			throw new MojoExecutionException("Manifest file '" + buildProperties + "' has no read permission.");
		}
		if (!buildProperties.canWrite()) {
			throw new MojoExecutionException("Manifest file '" + buildProperties + "' has no write permission.");
		}

		return projectDirectory;
	}

	/**
	 * Returns a set of all identified JAR files (path names relative to project
	 * directory), which must end with .jar. Excluded are *-sources.jar and
	 * *-javadoc.jar file.
	 * 
	 * @param projectDirectory      Project directory.
	 * @param arrLibraryDirectories Library directories relative to project
	 *                              directory.
	 * 
	 * @return Set of all JAR files.
	 */
	protected Set<String> findAllJarFiles(String projectDirectory, String[] arrLibraryDirectories)  throws MojoExecutionException {
		HashSet<String> setJarFiles = new HashSet<>();
		
		try {
			String absoluteProjectDirectory = new File(projectDirectory).getCanonicalPath().replace("\\", "/");

			for (String strDirectory : arrLibraryDirectories) {
				File dir = new File(projectDirectory + "/" + strDirectory.trim());
				File[] arrJarFiles = dir.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(".jar")
						&& !pathname.getName().toLowerCase().endsWith("-sources.jar")
						&& !pathname.getName().toLowerCase().endsWith("-javadoc.jar"));

				if (arrJarFiles != null) {
					for (File fileJar : arrJarFiles) {
						String strPath = fileJar.getCanonicalPath().replace("\\", "/").replace(absoluteProjectDirectory + "/", "");
						setJarFiles.add(strPath);
						getLog().info("Found " + strPath);
					}
				}
			}
		}
		catch (IOException exc) {
			throw new MojoExecutionException("Unable to resolve absolute and relative path names.", exc);
		}

		return setJarFiles;
	}

	/**
	 * Reads in the manifest file as UTF-8 file.
	 * 
	 * @param strManifestPath Path to the manifest file.
	 * 
	 * @return The content of the manifest file. Potential Windows-style line ends are converted to Linux line ends.
	 * 
	 * @throws MojoExecutionException Thrown, if we are unable to read the manifest file.
	 */
	protected String readManifest(String strManifestPath) throws MojoExecutionException {
		StringBuilder manifestContent = new StringBuilder();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
				Files.newInputStream(new File(strManifestPath).toPath()), StandardCharsets.UTF_8))) {
			String str;

			while ((str = in.readLine()) != null) {
				manifestContent.append(str).append("\n");
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException(
					"Unable to read in manifest file '" + projectDirectory + "/META-INF/MANIFEST.MF'.");
		}

		
		return manifestContent.toString().replace("\r\n", "\n");
	}
	
	/**
	 * Verifies the integrity of a modified manifest file.
	 * 
	 * @param manifestContent Manifest file content to be verified.
	 * @param errorMessage Error message to show to the user, if the verification fails. 
	 * 
	 * @return Manifest object, if verification was successful.
	 * 
	 * @throws MojoExecutionException Thrown, if we are unable to parse the manifest data.
	 */
	protected Manifest verifyManifest(String manifestContent, String errorMessage) throws MojoExecutionException {
		try {
			return new Manifest(new ByteArrayInputStream(manifestContent.getBytes()));
		}
		catch (IOException exc) {
			getLog().error("Failed to verify the following manifest: " + exc.getMessage(), exc);
			getLog().error(manifestContent);
			throw new MojoExecutionException(errorMessage, exc);
		}
	}

	/**
	 * Writes out the manifest file as UTF-8 file.
	 * 
	 * @param strManifestPath Path to the manifest file.
	 * @param content The content of the manifest file to be written.
	 * 
	 * @throws MojoExecutionException Thrown, if we are unable to write the manifest file.
	 */
	protected void writeManifest(String strManifestPath, String content) throws MojoExecutionException {
		try (Writer out = new BufferedWriter(new OutputStreamWriter(
				Files.newOutputStream(new File(strManifestPath).toPath()), StandardCharsets.UTF_8))) {
			out.append(content);
			out.flush();
		}
		catch (Exception e) {
			throw new MojoExecutionException(
					"Unable to write out manifest file '" + strManifestPath + "' with changes.");
		}
	}

	/**
	 * Reads in the build.properties file as ISO 8859-1 encoded file.
	 *  
	 * @param strBuildPropertiesPath Path to the build.properties file.
	 * 
	 * @return The content of the build.properties file. Potential Windows-style line ends are converted to Linux line ends.
	 * 
	 * @throws MojoExecutionException Thrown, if we are unable to read the build properties file.
	 */
	protected String readBuildProperties(String strBuildPropertiesPath) throws MojoExecutionException {
		StringBuilder content = new StringBuilder();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
				Files.newInputStream(new File(strBuildPropertiesPath).toPath()), StandardCharsets.ISO_8859_1))) {
			String str;

			while ((str = in.readLine()) != null) {
				content.append(str).append("\n");
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException(
					"Unable to read in file '" + strBuildPropertiesPath + "'.");
		}

		
		return content.toString().replace("\r\n", "\n");
	}

	/**
	 * Writes out the build.properties file as ISO 8859-1 encoded file.
	 *  
	 * @param strBuildPropertiesPath Path to the build.properties file.
	 * @param content The content of the build.properties file to be written.
	 * 
	 * @throws MojoExecutionException Thrown, if we are unable to write the build properties file.
	 */
	protected void writeBuildProperties(String strBuildPropertiesPath, String content) throws MojoExecutionException {
		try (Writer out = new BufferedWriter(new OutputStreamWriter(
				Files.newOutputStream(new File(strBuildPropertiesPath).toPath()), StandardCharsets.ISO_8859_1))) {
			out.append(content);
			out.flush();
		}
		catch (Exception e) {
			throw new MojoExecutionException(
					"Unable to write out file '" + strBuildPropertiesPath + "' with changes.");
		}
	}

	/**
	 * Determines the range (start - end position) for a specific attribute within a manifest file.
	 * If the block is not found, it will return as start and end position the size of the content,
	 * because it would mean that we need to append the block to the existing file.
	 * 
	 * @param content Content of text file.
	 * @param blockStart Text that starts a block, which we want to determine the row range for.
	 * @param setPossibleBlockStarters Other texts (can be objects) that start other blocks. Can be null to look just for a new line,
	 * 		considering the \\ character as multi-line value spread.
	 * 
	 * @return Point with x as start position and y as ending position (exclusive).
	 *
	 */
	protected Point getBlockStartAndEndPosition(String content, String blockStart, Set<Object> setPossibleBlockStarters) {
		Point p = new Point(-1, -1);
		int iPosition = 0;

		String[] arrLines = content.split("\n");
		for (int i = 0; i < arrLines.length; i++) {
			boolean bMultiValueLine = (i > 0 && arrLines[i - 1].trim().endsWith("\\"));
			
			if (arrLines[i].startsWith(blockStart)) {
				p.x = iPosition;
				p.y = -1;
			}
			else if (p.x >= 0) {
				if (setPossibleBlockStarters != null) {
					for (Object otherBlockStarter : setPossibleBlockStarters) {
						String strOtherBlockStarter = otherBlockStarter.toString();
						if (!strOtherBlockStarter.equals(blockStart) && arrLines[i].trim().startsWith(strOtherBlockStarter)) {
							p.y = iPosition;
							break;
						}
					}
				}
				else if (!bMultiValueLine) {
					p.y = iPosition;
					break;
				}
			}
			if (p.y >= 0) {
				break;
			}
			
			iPosition += arrLines[i].length() + 1;
		}
		
		// If the block we search for is the last one in the file, then we set the end to the size of the file
		if (p.x >= 0 && p.y == -1) {
			p.y = content.length();
		}

		// If we did not find the block we searched for, use the size of the file as position
		if (p.x == -1 && p.y == -1) {
			p = new Point(content.length(), content.length());
		}
		
		return p;
	}
	
	/**
	 * Modifies a string (block) by removing all JAR file names that start with any of the library directory names
	 * and afterwards adding all JAR files that were found in the library directories.
	 * 
	 * @param strLineStart Block start, e.g. "Bundle-ClassPath:". Must not be null.
	 * @param content The content to be changed.
	 * @param arrLibraryDirectories Array of library directories (relative to project path).
	 * @param setJarFiles Identified JAR file names in the library directories.
	 * @param strSeparator Separator characters that shall be used between JAR file names (a comma or comma and backslash). New line is
	 * 		not necessary to be specified as it will always be added.
	 * 
	 * @return Modified content.
	 */
	protected String modifyContent(String strLineStart, String content, String[] arrLibraryDirectories, Set<String> setJarFiles, String strSeparator) {
		StringBuilder sb = new StringBuilder(strLineStart);
		
		// We ignore in the payload the line start text (e.g. bin.includes), all new lines characters as well as all \ (multi-line characters)
		content = content.replace(strLineStart, "").replace("\n", "").replace("\\", "");
		TreeSet<String> setSortedJars = new TreeSet<>();
		
		// Find all JARs that will not be replaced
		// Note: A separator might be ,\ but in that case we ignore the \ as it just spans content on multiple lines
		for (String strJar : content.split(Pattern.quote(strSeparator.replace("\\", "")))) {
			strJar = strJar.trim();
			if (!strJar.isEmpty() && !willBeReplaced(strJar, arrLibraryDirectories)) {
				setSortedJars.add(strJar);
			}
		}
		
		// Add all JARs that were found
		setSortedJars.addAll(setJarFiles);
		
		// Put it all back together
		int i = 1;
		int iLen = setSortedJars.size();
		for (String strJar : setSortedJars) {
			sb.append(" ").append(strJar);
			if (i < iLen) {
				sb.append(strSeparator);
			}
			sb.append("\n");
			i++;
		}
		
		String strChangedContent = sb.toString();
		
		// Remove this attribute completely, if it is empty
		if (strChangedContent.equals(strLineStart)) {
			strChangedContent = "";
		}
		return strChangedContent;
	}
	
	/**
	 * Checks, if the JAR file path name starts with one of the passed library directories.
	 * 
	 * @param strJar JAR file path name
	 * @param arrLibraryDirectories Array with relative library directories, e.g. lib/nibr
	 * 
	 * @return True, if the JAR file path name started with one of the directory names. False, otherwise.
	 */
	protected boolean willBeReplaced(String strJar, String[] arrLibraryDirectories) {
		boolean bToBeReplaced = false;
		
		for (String libraryDirectory : arrLibraryDirectories) {
			if (strJar.startsWith(libraryDirectory + "/")) {
				bToBeReplaced = true;
				break;
			}
		}
		
		return bToBeReplaced;
	}

	//
	// Public Static Methods
	//

	/**
	 * Runs the KNIME Maven Dependencies Injection Mojo as Command Line Utility.
	 * 
	 * @param args Two arguments expected in the form:
	 * 		projectDirectory=<The base directory of the project>, 
	 * 		e.g. C:\Development\KNIME\4.6\knime-rdkit\org.rdkit.knime.nodes, and
	 * 		libraryDirectories=<Comma-separated list of relative paths to library folders, 
	 * 		relative to projectDirectory>, e.g. "lib".
	 */
	public static void main(String[] args) throws Exception {
		String strProjectDirectory = null;
		String strLibraryDirectories = null;
		
		if (args == null) {
			args = new String[0];
		}

		for (String arg : args) {
			if (arg != null && arg.startsWith("projectDirectory=")) {
				strProjectDirectory = arg.substring("projectDirectory=".length());
			} else if (arg != null && arg.startsWith("libraryDirectories=")) {
				strLibraryDirectories = arg.substring("libraryDirectories=".length());
			}
		}
		
		if (strProjectDirectory == null || strLibraryDirectories == null) {
			printUsageInfo();
			System.exit(1);
		}
		
		org.apache.maven.plugin.Mojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectDirectory, strLibraryDirectories);
		mojo.setLog(new SystemStreamLog());
		mojo.execute();
	}
	
	// 
	// Private Static Methods
	//
	
	/**
	 * Prints out usage information on system error stream.
	 */
	public static void printUsageInfo() {
		System.err.println("Run this tool either as command line utility or as Maven plugin. "
				+ "It scans a folder that contains JAR files that were fetched and updated before "
				+ "and makes changes in the MANIFEST.MF and build.properties files to specify them correctly. "
				+ "The folder that contains the JAR files is used to find out which JAR files exist "
				+ "and to find the entries of old references in the MANIFEST.MF file and "
				+ "build.properties file to remove them before adding all new entries.");
		System.err.println(
				"\nCommand Line Usage:\n" +
				"===================\n" +
				"KnimeMavenDependenciesInjectionMojo "
				+ "projectDirectory=<The base directory of the project> "
				+ "libraryDirectories=<Comma-separated list of relative paths to library folders, relative to projectDirectory>");
		System.err.println(
				"Example: KnimeMavenDependenciesInjectionMojo "
				+ "projectDirectory=\"C:\\Development\\KNIME\\4.6\\knime-rdkit\\org.rdkit.knime.nodes\" "
				+ "libraryDirectories=lib");
		System.err.println(
				"\nMaven Usage:\n" +
				"============\n" +
				"Please refer to Github documentation here: https://github.com/rdkit/knime-rdkit/tree/master/org.rdkit.knime.nodes/lib");
	}
	
}
