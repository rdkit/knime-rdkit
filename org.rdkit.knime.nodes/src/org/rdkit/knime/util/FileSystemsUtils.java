/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2023
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

package org.rdkit.knime.util;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * Utility class containing convenience methods to work with file data via KNIME NIO interface.
 *
 * @author Roman Balabanov
 */
public class FileSystemsUtils {

    //
    // Inner classes
    //

    /**
     * Functional interface defining data reader/converter routing.
     *
     * @param <T> Output data type.
     */
    @FunctionalInterface
    public interface ReaderFunction<T> {
        /**
         * Reads and converts the data from the {@link InputStream} instance provided.
         *
         * @param stream The input stream.
         *               Mustn't be null.
         * @return File data converted.
         * @throws IOException if occurred during reading.
         */
        T read(InputStream stream) throws IOException;
    }

    /**
     * Default implementation of {@link StatusMessage} consumer, used for KNIME file handling API calls
     * and simply "re-translates" the messages received to the {@link NodeLogger} instance provided.
     */
    public static class DefaultStatusMessageConsumer implements Consumer<StatusMessage> {

        //
        // Members
        //

        /**
         * {@code NodeLogger} instance.
         */
        private final NodeLogger m_logger;

        /**
         * Constructs {@code DefaultStatusMessageConsumer} instance.
         *
         * @param logger {@code NodeLogger} instance.
         *               Must not be null.
         * @throws IllegalArgumentException Thrown if {@code logger} parameters is null.
         */
        public DefaultStatusMessageConsumer(NodeLogger logger) {
            if (logger == null) {
                throw new IllegalArgumentException("Logger parameter must not be null.");
            }

            m_logger = logger;
        }

        @Override
        public void accept(StatusMessage statusMessage) {
            if (statusMessage != null && statusMessage.getMessage() != null) {
                switch (statusMessage.getType()) {
                    case ERROR -> m_logger.error(statusMessage.getMessage());
                    case WARNING -> m_logger.warn(statusMessage.getMessage());
                    case INFO -> m_logger.info(statusMessage.getMessage());
                }
            }
        }
    }

    //
    // Static methods
    //

    /**
     * Reads a file at the root path specified by {@code modelFilePath} parameter using a reader function provided
     * and returns its output as the result.
     *
     * @param modelFilePath {@code SettingsModelReaderFileChooser} instance representing source file path.
     *                       Mustn't be null.
     * @param funcReader     {@code ReaderFunction} implementation to used to read/convert file data to resulting representation desired.
     *                       Mustn't be null.
     * @param logger         {@code NodeLogger} instance to be used among IO operations.
     *                       Mustn't be null.
     * @param <T>            Type of the resulting instance. Inflicted by {@code funcReader} parameter.
     * @return File data representation object. The type is inflicted by {@code funcReader} parameter.
     * @throws InvalidSettingsException Thrown if settings are incorrect or the file
     *                                  could not be found (also an incorrect setting).
     * @throws IOException              Thrown if an exception occurred during file reading.
     * @throws IllegalArgumentException Thrown if any of {@code modelFilePath}, {@code function} or {@code logger} parameters is null.
     * @see org.rdkit.knime.util.FileSystemsUtils.ReaderFunction
     */
    public static <T> T readFile(final SettingsModelReaderFileChooser modelFilePath,
                                 final ReaderFunction<T> funcReader,
                                 final NodeLogger logger)
            throws IOException, InvalidSettingsException
    {
        if (modelFilePath == null) {
            throw new IllegalArgumentException("File Path Settings Model parameter must not be null.");
        }
        if (funcReader == null) {
            throw new IllegalArgumentException("Reader Function parameter must not be null.");
        }
        if (logger == null) {
            throw new IllegalArgumentException("Logger parameter must not be null.");
        }

        try (final ReadPathAccessor pathAccessor = modelFilePath.createReadPathAccessor();
             final InputStream inputStream = Files.newInputStream(pathAccessor.getRootPath(new DefaultStatusMessageConsumer(logger))))
        {
            return funcReader.read(inputStream);
        }
        catch (IllegalStateException e) {
            // re-throwing "No file system connection available. Execute connector node." as IOException.
            throw new IOException(e.getMessage());
        }
    }

}
