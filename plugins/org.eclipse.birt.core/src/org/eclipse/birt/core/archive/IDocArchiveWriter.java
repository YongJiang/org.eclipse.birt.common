/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.core.archive;

import java.io.IOException;

/**
 * An interface that wraps around a report archive for reading. A report 
 * archive may be, but is not limited to a zip file in compressed format, 
 * a folder in uncompressed format. 
 * 
 * Notice that the interface does not define archive file name, nor 
 * does it define folder name to store/uncompress the archive to. 
 * Setting such environments up is implementation class's  
 * responsibility. 
 */
public interface IDocArchiveWriter {

	/**
	 * This function must be called before the writer is used.
	 *  initialize the document archive. 
	 */
	public void initialize( ) throws IOException;
	
	/**
	 * @return the archive name
	 */
	public String getName();

	/**
	 * Create a random access stream in the archive and return it.
	 * 
	 * @param relativePath - relative path to report archive path. The path is based on Unix 
	 * 						syntax, with the root of the archive denoted by "/". The 
	 * 						initial "/" character can be skipped.
	 *  
	 * @return RAOutputStream
	 */
	public RAOutputStream createRandomAccessStream(String relativePath) throws IOException;
	
	/**
	 * Delete a stream from the archive. 
	 * Note: Not all of the derived classes support this function. E.g. FileArchiveWriter doesn't support it.  
	 * @param relativePath - the relative path of the stream
	 * @return whether the operation was successful
	 * @throws IOException
	 */
	public boolean dropStream( String relativePath );
	
	/**
	 * @param relativePath - the relative stream path in the archive. 
	 * The relative path is based on Unix syntax, with the root of the archive denoted 
	 * by "/". The initial "/" character can be skipped.
	 * 
	 * @return a list of strings representing the underlying stream 
	 * names. The return values are in the relative path format too.  
	 */
	public boolean exists( String relativePath );

	/**
	 * Set the stream sorter (if needed). The stream sorter will be used to sort the streams. If no stream sorter is set, the streams will be written in random order.
	 * @param streamSorter - the stream sorter
	 */
	public void setStreamSorter( IStreamSorter streamSorter );
	
	/**
	 * This function must be called after the writer is used.
	 * finalizes the socument archive. This may involve compressing the archive
	 * to a single file. This also closes and finishes using the archive.
	 */
	public void finish() throws IOException;
}
