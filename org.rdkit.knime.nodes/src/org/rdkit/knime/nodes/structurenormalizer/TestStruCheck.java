package org.rdkit.knime.nodes.structurenormalizer;

import java.io.File;
import java.util.Arrays;

import org.RDKit.RDKFuncs;
import org.RDKit.StringInt_Pair;
import org.rdkit.knime.util.FileUtils;
import org.rdkit.knime.util.StringUtils;
import org.rdkit.knime.util.SystemUtils;

public class TestStruCheck {

	public static void main(final String[] args) throws Exception {
		// The following is hard-coded for Manuel's development environments - needs to be adapted to the developers system
		if (SystemUtils.isWindows()) {
			try {
				// For Windows 32 bit
				System.load("C:/Development/KNIME/NIBR KNIME/rdkit_nodes/org.rdkit/org.rdkit.knime.bin.win32.x86/os/win32/x86/boost_system-vc100-mt-1_51.dll");
				System.load("C:/Development/KNIME/NIBR KNIME/rdkit_nodes/org.rdkit/org.rdkit.knime.bin.win32.x86/os/win32/x86/GraphMolWrap.dll");
			}
			catch (final Throwable exc) {
				// For Windows 64 bit
				System.load("C:/Development/KNIME/NIBR KNIME/rdkit_nodes/org.rdkit/org.rdkit.knime.bin.win32.x86_64/os/win32/x86_64/boost_system-vc100-mt-1_51.dll");
				System.load("C:/Development/KNIME/NIBR KNIME/rdkit_nodes/org.rdkit/org.rdkit.knime.bin.win32.x86_64/os/win32/x86_64/GraphMolWrap.dll");
			}
		}

		else if (SystemUtils.isUnix()) {
			try {
				// For Linux 32 bit
				System.load("/CHBS/apps_local/knime-dev/development_2.9/KNIME/NIBR/rdkit_nodes/org.rdkit/org.rdkit.knime.bin.linux.x86/os/linux/x86/libGraphMolWrap.so");
			}
			catch (final Throwable exc) {
				// For Linux 64 bit
				System.load("/CHBS/apps_local/knime-dev/development_2.9/KNIME/NIBR/rdkit_nodes/org.rdkit/org.rdkit.knime.bin.linux.x86_64/os/linux/x86_64/libGraphMolWrap.so");

			}
		}

		else if (SystemUtils.isMac()) {
			// For Mac 64 bit
			System.load("/toBeAdaptedToLocalSystem/rdkit_nodes/org.rdkit/org.rdkit.knime.bin.macosx.x86_64/os/macosx/x86_64/libGraphMolWrap.jnilib");
		}

		final TestStruCheck test = new TestStruCheck();
		test.test("C:\\Temp\\checkfgs.trn", "C:\\Temp\\checkfgs.chk");
	}

	public void test(final String strTransformationConfigurationFile, final String strAugmentedAtomsConfigurationFile) throws Exception {

		// Create log file
		final String strLogFile = null;
		File fileLog;
		if (!StringUtils.isEmptyAfterTrimming(strLogFile)) {
			fileLog = FileUtils.convertToFile(strLogFile, false, true);
		}
		else {
			fileLog = File.createTempFile("StructureNormalizer", ".log");
			fileLog.deleteOnExit();
		}

		// Create missing directories
		final File dirLog = fileLog.getParentFile();
		FileUtils.prepareDirectory(dirLog); // Throws an exception, if not successful

		// Initialize the structure checker
		String strOptions = "StruCheck\n" + // Double
				"-ta {0}\n" +
				"-tm\n" +
				"-or\n" +
				"-ca {1}\n" +
				"-cc\n" +
				"-cl 3\n" +
				"-cs\n" +
				"-cn 999\n" +
				"-l {2}";
		strOptions = strOptions.replace("{0}", strTransformationConfigurationFile);
		strOptions = strOptions.replace("{1}", strAugmentedAtomsConfigurationFile);
		strOptions = strOptions.replace("{2}", fileLog.getAbsolutePath());
		System.err.println("Hello");
		System.out.println("Hello");

		final int iError = RDKFuncs.initCheckMol(strOptions);
		if (iError != 0) {
			throw new Exception("Configuring the Structure Normalizer failed with error code #" + iError +
					" - Please check your configuration files (" + strTransformationConfigurationFile + ", " +
					strAugmentedAtomsConfigurationFile +").");
		}

		// Check input type
		final StringInt_Pair results = RDKFuncs.checkMolString("c1ccccn1", true);
		final String strCorrectedStructure = results.getFirst();
		final int iFlags = results.getSecond();
		final StruCheckCode[] arrErrorCodes = StruCheckCode.getCodes(iFlags, StruCheckCode.getErrorCodeMask());
		final StruCheckCode[] arrWarningCodes = StruCheckCode.getCodes(iFlags, StruCheckCode.getNonErrorCodeMask());

		System.out.println("Corrected Structure: " + strCorrectedStructure);
		System.out.println("Errors: " + Arrays.toString(arrErrorCodes));
		System.out.println("Warnings: " + Arrays.toString(arrWarningCodes));
	}
}
