/* 
 * This source code, its documentation and all related files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (C) 2012
 * Novartis Institutes for BioMedical Research
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 */
package org.rdkit.knime.util;


/**
 * This interface defines methods to register RDKit objects, which are subject of later
 * cleanup.
 * 
 * @author Manuel Schwarze
 */
public interface RDKitObjectCleaner {
	
	/**
	 * Creates a new wave id. This id must be unique in the context of the overall runtime
	 * of the Java VM, at least in the context of the same class loader and memory area.
	 * 
	 * @return Unique wave id.
	 */
	int createUniqueCleanupWaveId();
	
    /**
     * Registers an RDKit based object, which must have a delete() method implemented
     * for freeing up resources later. The cleanup will happen for all registered 
     * objects when the method {@link #cleanupMarkedObjects()} is called. 
     * 
     * @param <T> Any class that implements a delete() method to be called to free up resources.  
     * @param rdkitObject An RDKit related object that should free resources when not 
     * 		used anymore. Can be null.
     * 
     * @return The same object that was passed in. Null, if null was passed in.
     * 
     * @see #markForCleanup(Object, int)
     */
    <T extends Object> T markForCleanup(T rdkitObject);
    
    /**
     * Registers an RDKit based object that is used within a certain block (wave). $
     * This object must have a delete() method implemented for freeing up resources later. 
     * The cleanup will happen for all registered objects when the method 
     * {@link #cleanupMarkedObjects(int)} is called with the same wave.
     * 
     * @param <T> Any class that implements a delete() method to be called to free up resources.  
     * @param rdkitObject An RDKit related object that should free resources when not 
     * 		used anymore. Can be null.
     * @param wave A number that identifies objects registered for a certain "wave". 
     * 
     * @return The same object that was passed in. Null, if null was passed in.
     * 
     * @see #markForCleanup(Object)
     */
    <T extends Object> T markForCleanup(T rdkitObject, int wave);

    /** 
     * Frees resources for all objects that have been registered prior to this last
     * call using the method {@link #cleanupMarkedObjects()}.
     */
    void cleanupMarkedObjects();
    
    /** 
     * Frees resources for all objects that have been registered prior to this last
     * call for a certain wave using the method {@link #cleanupMarkedObjects(int)}.
     * 
     * @param wave A number that identifies objects registered for a certain "wave". 
     */
    void cleanupMarkedObjects(int wave);

}
