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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests how manifests and build.properties manipulations are handled successfully.
 * 
 * @author Manuel Schwarze, Novartis Biomedical Research
 */
public class TestSuccessKnimeMavenDependenciesInjectionMojo {

	//
	// Constants
	//

	/** Logger. */
	private static final Logger LOGGER = Logger.getLogger(TestSuccessKnimeMavenDependenciesInjectionMojo.class.getName());
	
	//
	// Tests
	//

	@Test
	public void testUsageInfo() throws Exception {
		LOGGER.info("Testing usage info ...");
		
		PrintStream streamOld = System.err;
		ByteArrayOutputStream outFetch = new ByteArrayOutputStream();
		System.setErr(new PrintStream(outFetch));
		
		KnimeMavenDependenciesInjectionMojo.printUsageInfo();
		
		outFetch.flush();
		String strMessage = outFetch.toString();
		
		Assertions.assertAll(
				() -> Assertions.assertTrue(strMessage.contains("Command Line Usage:")),
				() -> Assertions.assertTrue(strMessage.contains("Maven Usage:")),
				() -> Assertions.assertTrue(strMessage.contains("KnimeMavenDependenciesInjectionMojo")),
				() -> Assertions.assertTrue(strMessage.contains("projectDirectory=")),
				() -> Assertions.assertTrue(strMessage.contains("libraryDirectories="))
		);
		
		System.setErr(streamOld);
	}
	
	
	@Test
	public void testAdditionsAndSorting() throws Exception {
		LOGGER.info("Testing additions and sorting ...");
		
		String strProjectFolder = TestUtils.prepareTest("successful/additions-and-sorting",
				"MANIFEST.MF", "build.properties", "lib", "mylibrary-A.jar", "mylibrary-B.jar");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		mojo.execute();
		TestUtils.compareFilesAfterTest("successful/additions-and-sorting", strProjectFolder, "MANIFEST.MF", "build.properties");
	}

	@Test
	public void testReductionAndSorting() throws Exception {
		LOGGER.info("Testing reduction and sorting ...");
		
		String strProjectFolder = TestUtils.prepareTest("successful/reduction-and-sorting",
				"MANIFEST.MF", "build.properties", "lib", "mylibrary-A.jar", "mylibrary-B.jar");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		mojo.execute();
		TestUtils.compareFilesAfterTest("successful/reduction-and-sorting", strProjectFolder, "MANIFEST.MF", "build.properties");
	}

	@Test
	public void testReductionToNoManagedJars() throws Exception {
		LOGGER.info("Testing reduction to no managed JARs ...");
		
		String strProjectFolder = TestUtils.prepareTest("successful/reduction-to-no-managed-jars",
				"MANIFEST.MF", "build.properties", "lib");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		mojo.execute();
		TestUtils.compareFilesAfterTest("successful/reduction-to-no-managed-jars", strProjectFolder, "MANIFEST.MF", "build.properties");
	}

	@Test
	public void testReductionToNoJarsAtAll() throws Exception {
		LOGGER.info("Testing reduction to no JARs at all ...");
		
		String strProjectFolder = TestUtils.prepareTest("successful/reduction-to-no-jars-at-all",
				"MANIFEST.MF", "build.properties", "lib");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		mojo.execute();
		TestUtils.compareFilesAfterTest("successful/reduction-to-no-jars-at-all", strProjectFolder, "MANIFEST.MF", "build.properties");
	}
	
	@Test
	public void testIgnoringSourcesAndJavaDocFiles() throws Exception {
		LOGGER.info("Testing ignoring sources and javadoc files ...");
		
		String strProjectFolder = TestUtils.prepareTest("successful/additions-and-sorting",
				"MANIFEST.MF", "build.properties", "lib", 
				"mylibrary-A.jar", "mylibrary-A-sources.jar", "mylibrary-A-javadoc.jar", "mylibrary-B.jar", "mylibrary-B-sources.jar");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		mojo.execute();
		TestUtils.compareFilesAfterTest("successful/additions-and-sorting", strProjectFolder, "MANIFEST.MF", "build.properties");
	}

	@Test
	public void testAttributePositionsOnTop() throws Exception {
		LOGGER.info("Testing attribute position on top ...");
		
		String strProjectFolder = TestUtils.prepareTest("successful/attribute-on-top-position",
				"MANIFEST.MF", "build.properties", "lib", "mylibrary-A.jar", "mylibrary-B.jar");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		mojo.execute();
		TestUtils.compareFilesAfterTest("successful/attribute-on-top-position", strProjectFolder, "MANIFEST.MF", "build.properties");
	}

	@Test
	public void testAttributePositionsOnBottom() throws Exception {
		LOGGER.info("Testing attribute position on bottom ...");
		
		String strProjectFolder = TestUtils.prepareTest("successful/attribute-on-bottom-position",
				"MANIFEST.MF", "build.properties", "lib", "mylibrary-A.jar", "mylibrary-B.jar");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		mojo.execute();
		TestUtils.compareFilesAfterTest("successful/attribute-on-bottom-position", strProjectFolder, "MANIFEST.MF", "build.properties");
	}

	@Test
	public void testMissingAttributes() throws Exception {
		LOGGER.info("Testing missing attributes ...");
		
		String strProjectFolder = TestUtils.prepareTest("successful/missing-attributes",
				"MANIFEST.MF", "build.properties", "lib", "mylibrary-A.jar", "mylibrary-B.jar");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		mojo.execute();
		TestUtils.compareFilesAfterTest("successful/missing-attributes", strProjectFolder, "MANIFEST.MF", "build.properties");
	}

	@Test
	public void testMultipleValuesPerLine() throws Exception {
		LOGGER.info("Testing multiple values per line ...");
		
		String strProjectFolder = TestUtils.prepareTest("successful/multiple-values-per-line",
				"MANIFEST.MF", "build.properties", "lib", "mylibrary-A.jar", "mylibrary-B.jar");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		mojo.execute();
		TestUtils.compareFilesAfterTest("successful/multiple-values-per-line", strProjectFolder, "MANIFEST.MF", "build.properties");
	}
}
