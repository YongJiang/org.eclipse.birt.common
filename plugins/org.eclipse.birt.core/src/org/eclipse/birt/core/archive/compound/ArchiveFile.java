/*******************************************************************************
 * Copyright (c) 2004, 2009 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.core.archive.compound;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.core.archive.cache.SystemCacheManager;
import org.eclipse.birt.core.archive.compound.v3.Ext2FileSystem;

/**
 * change the default format to ext2.
 */
public class ArchiveFile implements IArchiveFile
{

	public static final boolean enableSystemCache = true;
	public static final SystemCacheManager systemCacheManager = new SystemCacheManager(
			1024 );
	public static final boolean enableFileCache = true;
	public static final int FILE_CACHE_SIZE = 1024;

	static final long ARCHIVE_V2_TAG = ArchiveConstants.DOCUMENT_TAG;
	static final long ARCHIVE_V3_TAG = Ext2FileSystem.EXT2_MAGIC_TAG;

	/**
	 * the archive file name.
	 */
	protected String archiveName;

	protected String systemId;

	protected IArchiveFile af;

	public ArchiveFile( String fileName, String mode ) throws IOException
	{
		// set blank string as the default system id of the archive file.
		this( null, fileName, mode );
	}

	public ArchiveFile( String systemId, String fileName, String mode )
			throws IOException
	{
		if ( fileName == null || fileName.length( ) == 0 )
			throw new IOException( "The file name is null or empty string." );

		File fd = new File( fileName );
		// make sure the file name is an absolute path
		fileName = fd.getCanonicalPath( );
		this.archiveName = fileName;
		this.systemId = systemId;
		if ( "r".equals( mode ) )
		{
			openArchiveForReading( );
		}
		else if ( "rw+".equals( mode ) )
		{
			openArchiveForAppending( );
		}
		else
		{
			// rwt, rw mode
			ArchiveFileV3 f3 = new ArchiveFileV3( fileName, mode );
			f3.setSystemId( systemId );
			this.af = f3;
		}
	}

	protected void openArchiveForReading( ) throws IOException
	{
		// test if we need upgrade the document
		RandomAccessFile rf = new RandomAccessFile( archiveName, "r" );
		try
		{
			long magicTag = rf.readLong( );
			if ( magicTag == ARCHIVE_V2_TAG )
			{
				ArchiveFileV2 v2 = new ArchiveFileV2( archiveName, rf, "r" );
				upgradeSystemId( v2 );
				af = v2;
			}
			else if ( magicTag == ARCHIVE_V3_TAG )
			{
				ArchiveFileV3 fs = new ArchiveFileV3( archiveName, rf, "r" );
				upgradeSystemId( fs );
				af = fs;
			}
			else
			{
				af = new ArchiveFileV1( archiveName, rf );
			}
		}
		catch ( IOException ex )
		{
			rf.close( );
			throw ex;
		}
	}

	protected void openArchiveForAppending( ) throws IOException
	{
		// we need upgrade the document
		RandomAccessFile rf = new RandomAccessFile( archiveName, "rw" );
		if ( rf.length( ) == 0 )
		{
			// this is a empty file
			af = new ArchiveFileV3( archiveName, rf, "rw" );
		}
		else
		{
			try
			{
				long magicTag = rf.readLong( );
				if ( magicTag == ARCHIVE_V2_TAG )
				{
					af = new ArchiveFileV2( archiveName, rf, "rw+" );
				}
				else if ( magicTag == ARCHIVE_V3_TAG )
				{
					af = new ArchiveFileV3( archiveName, rf, "rw+" );
				}
				else
				{
					rf.close( );
					upgradeArchiveV1( );
					af = new ArchiveFileV3( archiveName, "rw+" );
				}
				upgradeSystemId( af );
			}
			catch ( IOException ex )
			{
				rf.close( );
				throw ex;
			}
		}
	}

	/**
	 * get the archive name.
	 * 
	 * the archive name is the file name used to create the archive instance.
	 * 
	 * @return archive name.
	 */
	public String getName( )
	{
		return archiveName;
	}

	public String getDependId( )
	{
		return af.getDependId( );
	}

	public String getSystemId( )
	{
		return systemId;
	}

	/**
	 * close the archive.
	 * 
	 * all changed data will be flushed into disk if the file is opened for
	 * write.
	 * 
	 * the file will be removed if it is opend as transient.
	 * 
	 * after close, the instance can't be used any more.
	 * 
	 * @throws IOException
	 */
	public void close( ) throws IOException
	{
		if ( isArchiveFileAvailable( af ) )
		{
			af.close( );
			af = null;
		}
	}

	public void setCacheSize( long cacheSize )
	{
		if ( isArchiveFileAvailable( af ) )
		{
			af.setCacheSize( cacheSize );
		}
	}

	public long getUsedCache( )
	{
		if ( isArchiveFileAvailable( af ) )
		{
			return af.getUsedCache( );
		}
		return 0;
	}

	static public long getTotalUsedCache( )
	{
		return (long) systemCacheManager.getUsedCacheSize( ) * 4096;
	}

	static public void setTotalCacheSize( long size )
	{
		long blockCount = ( size + 4095 ) / 4096;
		if ( blockCount > Integer.MAX_VALUE )
		{
			systemCacheManager.setMaxCacheSize( Integer.MAX_VALUE );
		}
		else
		{
			systemCacheManager.setMaxCacheSize( (int) blockCount );
		}
	}

	/**
	 * save the
	 * 
	 * @param fileName
	 * @throws IOException
	 */
	public void saveAs( String fileName ) throws IOException
	{
		ArchiveFileV3 file = new ArchiveFileV3( fileName, "rw" );
		try
		{
			file.setSystemId( systemId );
			List entries = listEntries( "/" );
			Iterator iter = entries.listIterator( );
			while ( iter.hasNext( ) )
			{
				String name = (String) iter.next( );
				ArchiveEntry tgt = file.createEntry( name );
				try
				{
					ArchiveEntry src = openEntry( name );
					try
					{
						copyEntry( src, tgt );
					}
					finally
					{
						src.close( );
					}
				}
				finally
				{
					tgt.close( );
				}
			}
		}
		finally
		{
			file.close( );
		}
	}

	/**
	 * save the file. If the file is transient file, after saving, it will be
	 * converts to normal file.
	 * 
	 * @throws IOException
	 */
	public void save( ) throws IOException
	{
		if ( isArchiveFileAvailable( af ) )
		{
			af.save( );
			/*
			 * if ( af instanceof ArchiveFileV2 ) { ( (ArchiveFileV2) af ).save(
			 * ); } else { af.flush( ); }
			 */
		}
		else
		{
			throw new IOException(
					"The archive file has been closed. System ID: " + systemId );
		}
	}

	private void copyEntry( ArchiveEntry src, ArchiveEntry tgt )
			throws IOException
	{
		byte[] b = new byte[4096];
		long length = src.getLength( );
		long pos = 0;
		while ( pos < length )
		{
			int size = src.read( pos, b, 0, 4096 );
			tgt.write( pos, b, 0, size );
			pos += size;
		}
	}

	synchronized public void flush( ) throws IOException
	{
		if ( isArchiveFileAvailable( af ) )
		{
			af.flush( );
		}
		else
		{
			throw new IOException(
					"The archive file has been closed. System ID: " + systemId );
		}
	}

	synchronized public void refresh( ) throws IOException
	{
		if ( isArchiveFileAvailable( af ) )
		{
			af.refresh( );
		}
		else
		{
			throw new IOException(
					"The archive file has been closed. System ID: " + systemId );
		}
	}

	synchronized public boolean exists( String name )
	{
		if ( isArchiveFileAvailable( af ) )
		{
			return af.exists( name );
		}
		return false;
	}

	synchronized public ArchiveEntry openEntry( String name )
			throws IOException
	{
		if ( isArchiveFileAvailable( af ) )
		{
			return af.openEntry( name );
		}
		else
		{
			throw new IOException( "Can not get entry named " + name
					+ " because the archive file has been closed. System ID: "
					+ systemId );
		}
	}

	synchronized public List listEntries( String namePattern )
	{
		if ( isArchiveFileAvailable( af ) )
		{
			return af.listEntries( namePattern );
		}
		else
		{
			return Collections.EMPTY_LIST;
		}
	}

	synchronized public ArchiveEntry createEntry( String name )
			throws IOException
	{
		if ( isArchiveFileAvailable( af ) )
		{
			return af.createEntry( name );
		}
		else
		{
			throw new IOException( "Can not create entry named " + name
					+ "because the archive file has been closed. System ID: "
					+ systemId );
		}
	}

	synchronized public boolean removeEntry( String name ) throws IOException
	{
		if ( isArchiveFileAvailable( af ) )
		{
			return af.removeEntry( name );
		}
		else
		{
			throw new IOException( "Can not remove entry named " + name
					+ " because the archive file has been closed. System ID: "
					+ systemId );
		}
	}

	public Object lockEntry( String name ) throws IOException
	{
		return af.lockEntry( name );
	}

	public void unlockEntry( Object locker ) throws IOException
	{
		if ( isArchiveFileAvailable( af ) )
		{
			af.unlockEntry( locker );
		}
		else
		{
			throw new IOException(
					"The archive file has been closed. System ID: " + systemId );
		}
	}

	/**
	 * upgrade the archive file to the latest version
	 * 
	 * @throws IOException
	 */
	private void upgradeArchiveV1( ) throws IOException
	{
		ArchiveFileV1 reader = new ArchiveFileV1( archiveName );
		try
		{
			File tempFile = File.createTempFile( "temp_", ".archive" );
			tempFile.deleteOnExit( );
			ArchiveFile writer = new ArchiveFile( tempFile.getAbsolutePath( ),
					"rwt" );
			List streams = reader.listEntries( "" );
			Iterator iter = streams.iterator( );
			while ( iter.hasNext( ) )
			{
				String name = (String) iter.next( );
				ArchiveEntry src = reader.openEntry( name );
				try
				{
					ArchiveEntry tgt = writer.createEntry( name );
					try
					{
						copyEntry( src, tgt );
					}
					finally
					{
						tgt.close( );
					}
				}
				finally
				{
					src.close( );
				}
			}
			writer.saveAs( archiveName );
			writer.close( );
		}
		finally
		{
			reader.close( );
		}
	}

	/**
	 * upgrade systemId when open/append the current file
	 * 
	 * @param file
	 */
	private void upgradeSystemId( IArchiveFile file )
	{
		if ( systemId == null )
		{
			systemId = file.getSystemId( );
		}
	}

	/**
	 * @param af
	 *            ArchiveFile
	 * @return whether the ArchiveFile instance is available
	 */
	private boolean isArchiveFileAvailable( IArchiveFile af )
	{
		return af != null;
	}
}
