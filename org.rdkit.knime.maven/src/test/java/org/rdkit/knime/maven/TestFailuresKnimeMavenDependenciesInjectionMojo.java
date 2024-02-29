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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests how failures are handled.
 * 
 * @author Manuel Schwarze, Novartis Biomedical Research
 */
public class TestFailuresKnimeMavenDependenciesInjectionMojo {

	//
	// Constants
	//

	/** Logger. */
	private static final Logger LOGGER = Logger.getLogger(TestFailuresKnimeMavenDependenciesInjectionMojo.class.getName());
	
	//
	// Tests
	//

	@Test
	public void testMissingManifestFile() throws Exception {
		LOGGER.info("Testing missing manifest file ...");
		
		String strProjectFolder = TestUtils.prepareTest("failures/diverse",
				null, "build.properties", "lib", "mylibrary-A.jar", "mylibrary-B.jar");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		
		Assertions.assertTrue(Assertions.assertThrows(MojoExecutionException.class, () -> {
			try {
				mojo.execute();
			}
			catch (Exception exc) {
				LOGGER.log(Level.INFO, "Caught exception with message: " + exc.getMessage());
				throw exc;
			}
	    }).getMessage().contains("'" + strProjectFolder + File.separator + "META-INF" + File.separator + "MANIFEST.MF' not found or not a file"));		
	}

	@Test
	public void testMissingBuildPropertiesFile() throws Exception {
		LOGGER.info("Testing missing build.properties file ...");
		
		String strProjectFolder = TestUtils.prepareTest("failures/diverse",
				"MANIFEST.MF", null, "lib", "mylibrary-A.jar", "mylibrary-B.jar");
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo(strProjectFolder, "lib");
		
		Assertions.assertTrue(Assertions.assertThrows(MojoExecutionException.class, () -> {
			try {
				mojo.execute();
			}
			catch (Exception exc) {
				LOGGER.log(Level.INFO, "Caught exception with message: " + exc.getMessage());
				throw exc;
			}
	    }).getMessage().contains("'" + strProjectFolder + File.separator + "build.properties' not found or not a file"));		
	}

	@Test
	public void testMissingProjectDirectory() {
		LOGGER.info("Testing missing project directory ...");
		
		KnimeMavenDependenciesInjectionMojo mojo = new KnimeMavenDependenciesInjectionMojo("/tmp/not-existing", "lib");
		
		Assertions.assertTrue(Assertions.assertThrows(MojoExecutionException.class, () -> {
			try {
				mojo.execute();
			}
			catch (Exception exc) {
				LOGGER.log(Level.INFO, "Caught exception with message: " + exc.getMessage());
				throw exc;
			}
	    }).getMessage().contains("not a directory"));		
	}
	
	
}
