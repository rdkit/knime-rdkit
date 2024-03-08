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
 *  along with this program; if not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 * ---------------------------------------------------------------------
 */
package org.rdkit.knime.maven;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;

/**
 * This class offers several utility methods to perform testing.
 *
 * @author Manuel Schwarze, Novartis Biomedical Research
 */
public class TestUtils {

	//
	// Constants
	//

	/** Flag to control, if temporary testing files and directories shall be deleted or not. */
	public static final boolean KEEP_TEMPORARY_DIRECTORIES_FOR_DEBUGGING = true;

	/** Logger. */
	private static final Logger LOGGER = Logger.getLogger(TestSuccessKnimeMavenDependenciesInjectionMojo.class.getName());

	//
	// Public Static Methods
	//

	/**
	 * Prepares a temporary directory that is used to simulate a project directory and creates based on the 
	 * available test data a MANIFEST file and a build.properties file as well as a selection of library files
	 * (with no content).
	 * 
	 * @param strTestDirectory The test resource directory to copy files from. Must not be null.
	 * @param strManifestFileName Set to MANIFEST.MF, if the manifest file with name MANIFEST.MF-before shall be
	 *                            used from the test resource directory. It will be copied to the temp directory under
	 *                            META-INF/MANIFEST.MF. Otherwise, set to null.   
	 * @param strBuildPropertiesFileName Set to build.properties, if the file with name build.properties-before shall be
	 * 	 *                            used from the test resource directory. It will be directly copied to the temp directory.
	 * 	                              Otherwise, set to null.
	 * @param strLibDirectory The lib directory (can be multiple nested directories) that shall be created in the temp directory.
	 *                        Can be null to skip.
	 * @param arrPseudoLibFiles Arbitrary number of JAR file names that will be created with no content in
	 *                          the generated lib directory. These file names are not used, if strLibDirectory is null.
	 * 
	 * @return The path of the temp directory, which will be used in the tests as project directory.
	 *
	 * @throws IOException Thrown, if some setup failed.
	 */
	public static String prepareTest(String strTestDirectory, String strManifestFileName, String strBuildPropertiesFileName,
			String strLibDirectory, String... arrPseudoLibFiles) throws IOException {
        ClassLoader classLoader = TestUtils.class.getClassLoader();

        // Create temp directory as simulated project directory
		Path pathTempDirectory = Files.createTempDirectory(strTestDirectory.replace("/", "-") + "-");
		File fileTempDirectory = pathTempDirectory.toFile();
		
		LOGGER.info("Create temporary project directory '" + fileTempDirectory + "'");
		
		if (!KEEP_TEMPORARY_DIRECTORIES_FOR_DEBUGGING) {
			pathTempDirectory.toFile().deleteOnExit();
		}
		
		File fileManifestDirectory = new File(pathTempDirectory.toFile().getAbsoluteFile() + "/META-INF");
		Assertions.assertTrue(fileManifestDirectory.mkdirs(), "Unable to create testing directories.");
		
        // Prepare MANIFEST.MF file
		if (strManifestFileName != null) {
	        File resourceFileBefore = new File(classLoader.getResource(strTestDirectory + "/" + strManifestFileName + "-before").getFile());
	
	        // Copy the Manifest file
	        File fileManifestCopy = new File(fileManifestDirectory, resourceFileBefore.getName().replace("-before", ""));
			Files.copy(resourceFileBefore.toPath(), fileManifestCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		
        // Prepare build.properties file
		if (strBuildPropertiesFileName != null) {
	        File resourceFileBefore = new File(classLoader.getResource(strTestDirectory + "/" + strBuildPropertiesFileName + "-before").getFile());
	
	        // Copy the Manifest file
	        File fileBuildPropertiesCopy = new File(fileTempDirectory, resourceFileBefore.getName().replace("-before", ""));
			Files.copy(resourceFileBefore.toPath(), fileBuildPropertiesCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		
		// Copy lib directory
		if (strLibDirectory != null) {
			File fileLibDirectory = new File(pathTempDirectory.toFile().getAbsoluteFile() + "/" + strLibDirectory);
			Assertions.assertTrue(fileLibDirectory.mkdirs(), "Unable to create testing directories.");
			for (String strLibFile : arrPseudoLibFiles) {
				new File(fileLibDirectory, strLibFile).createNewFile();
			}
		}
		
		return pathTempDirectory.toAbsolutePath().toString();
	}

	/**
	 * Copies files from the test resource folder into the temporary folder and compares them with the outcome of the
	 * performed test to see if the test was successful.
	 *
	 * @param strTestDirectory The test resource directory to copy files from. Must not be null.
	 * @param strTempDirectory The simulated project directory, which is the temporary test directory created in the
	 *                         beginning of a test. Must not be null.
	 * @param strManifestFileName Set to MANIFEST.MF, if the manifest file with name MANIFEST.MF-after shall be
	 *                            used from the test resource directory. It will be copied to the temp directory and used
	 *                            to compare with the existing MANIFEST.MF file. If not interested in it, set to null.
	 * @param strBuildPropertiesFileName Set to build.properties, if the file with name build.properties-after shall be
	 * 	 						   used from the test resource directory. It will be directly copied to the temp directory
	 * 	                           and used to compare with the existing build.properties file. Otherwise, set to null.
	 *
	 * @throws IOException Thrown, if the setup of the test files to compare failed.
	 */
	public static void compareFilesAfterTest(String strTestDirectory, String strTempDirectory,
			String strManifestFileName, String strBuildPropertiesFileName) throws IOException {
        ClassLoader classLoader = TestUtils.class.getClassLoader();
        File fileManifestToTestTmp = null;
        File fileManifestAfterCopyTmp = null;
        File fileBuildPropertiesToTestTmp = null;
        File fileBuildPropertiesAfterCopyTmp = null;
        
        // Prepare golden MANIFEST.MF file for easy comparison
		if (strManifestFileName != null) {
			File resourceManifestFileAfter = new File(classLoader.getResource(strTestDirectory + "/" + strManifestFileName + "-after").getFile());
			fileManifestAfterCopyTmp = new File(new File(strTempDirectory + "/META-INF"), resourceManifestFileAfter.getName());
			Files.copy(resourceManifestFileAfter.toPath(), fileManifestAfterCopyTmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
			fileManifestToTestTmp = new File(strTempDirectory + "/META-INF/" + strManifestFileName);
		}
		
        // Prepare golden MANIFEST.MF file for easy comparison
		if (strBuildPropertiesFileName != null) {
			File resourceBuildPropertiesFileAfter = new File(classLoader.getResource(strTestDirectory + "/" + strBuildPropertiesFileName + "-after").getFile());
			fileBuildPropertiesAfterCopyTmp = new File(new File(strTempDirectory), resourceBuildPropertiesFileAfter.getName());
			Files.copy(resourceBuildPropertiesFileAfter.toPath(), fileBuildPropertiesAfterCopyTmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
			fileBuildPropertiesToTestTmp = new File(strTempDirectory + "/" + strBuildPropertiesFileName);
		}

        final File fileManifestToTest = fileManifestToTestTmp;
        final File fileManifestAfterCopy = fileManifestAfterCopyTmp;
        final File fileBuildPropertiesToTest = fileBuildPropertiesToTestTmp;
        final File fileBuildPropertiesAfterCopy = fileBuildPropertiesAfterCopyTmp;
		
		Assertions.assertAll(
			() -> Assertions.assertTrue(strManifestFileName != null &&
				compareFiles(fileManifestToTest, fileManifestAfterCopy), 
				"META-INF/MANIFEST.MF file is not as expected - details in " + strTempDirectory),
			() -> Assertions.assertTrue(strBuildPropertiesFileName != null && 
				compareFiles(fileBuildPropertiesToTest, fileBuildPropertiesAfterCopy), 
				"build.properties file is not as expected - details in " + strTempDirectory)
		);
	}

	/**
	 * Compares two files.
	 *
	 * @param file1 First file to compare. Must not be null.
	 * @param file2 Second file to compare. Must not be null.
	 *
	 * @return True, if the files are the same. False, otherwise.
	 */
    public static boolean compareFiles(File file1, File file2) {
        try (FileInputStream fis1 = new FileInputStream(file1);
             FileInputStream fis2 = new FileInputStream(file2);
             BufferedInputStream bis1 = new BufferedInputStream(fis1);
             BufferedInputStream bis2 = new BufferedInputStream(fis2)) {

            int byte1, byte2;

            while ((byte1 = bis1.read()) != -1 && (byte2 = bis2.read()) != -1) {
                if (byte1 != byte2) {
                    return false;
                }
            }

            return bis1.read() == -1 && bis2.read() == -1;
        }
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Comparing files failed.", e);
            return false;
        }
    }
}
