/*
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * (C)Copyright 2011 by Novartis Pharma AG
 * Novartis Campus, CH-4002 Basel, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 */
package org.rdkit.knime.nodes;

import java.util.Locale;

import org.knime.testing.core.AbstractTestcaseCollector;

/**
 * This class declares by extending the KNIME Abstract Testcase Collector
 * that we have JUnit Tests that we want to test.
 *
 * To let the KNIME's UnitTestrunner Application find the JUnit tests, which are living
 * in other plug-ins or fragments, this class is extending the extension point
 * "org.knime.testing.TestcaseCollector" by implementing AbstractTestcaseCollector.
 *
 * The application collects then all Class files in the same class path as
 * this class and tries to used them as JUnit tests. The results are
 * JUnit Compatible XML report files and are written to the directory
 * specified as -destDir of the UnitTestrunner Application.
 *
 * @author Manuel Schwarze
 */
public class RdkitTestCases extends AbstractTestcaseCollector {
	// Empty by purpose
    public RdkitTestCases() {
        System.out.println("Locale: " + Locale.getDefault());
    }
}
