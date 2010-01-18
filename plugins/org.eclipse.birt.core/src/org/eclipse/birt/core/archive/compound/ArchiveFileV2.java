/*******************************************************************************
 * Copyright (c) 2004, 2008 Actuate Corporation.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.core.archive.cache.CacheListener;
import org.eclipse.birt.core.archive.cache.Cacheable;
import org.eclipse.birt.core.archive.cache.FileCacheManager;

/**
 * the archive file contains following mode:
 * <li> "r" open the file for read only.
 * <li> "rw" create the file for read/write
 * <li> "rw+" open file is open for read/write
 * <li> "rwt" create the trainsnt file, it will be removed after closing.
 */
public class ArchiveFileV2 implements IArchiveFile, ArchiveConstants
{

	/** the physical file correspond to this compound file system */
	protected RandomAccessFile rf;

	/**
	 * if the file is closed.
	 */
	protected boolean isClosed;
	/**
	 * the archive file is writable.
	 */
	protected boolean isWritable;

	/**
	 * the archive file is transient.
	 */
	protected boolean isTransient;

	/**
	 * the archive file is appended.
	 */
	protected boolean isAppend;

	/**
	 * the archive file name.
	 */
	protected String archiveName;

	protected String systemId;

	protected String dependId;

	protected int BLOCK_SIZE;
	/**
	 * header status
	 */
	protected ArchiveHeader head;
	/**
	 * allocation table of the archive file
	 */
	protected AllocTable allocTbl;
	/**
	 * entry table of the archive file
	 */
	protected NameTable entryTbl;
	/**
	 * archive entries in the table
	 */
	protected HashMap<String, ArchiveEntryV2> entries;

	/**
	 * cache manager of the archive file.
	 */
	protected FileCacheManager caches;

	/**
	 * the total blocks exits in this file
	 */
	protected int totalBlocks;

	/**
	 * the total blocks exits in the disk
	 */
	protected int totalDiskBlocks;

	/**
	 * setup the flags used to open the archive.
	 * <p>
	 * 
	 * the mode can be either of:
	 * <li>r</li>
	 * open the archive file for read only, the file must exits.
	 * <li>rw</li>
	 * open the archive file for read and write, if the file is exits, create a
	 * new one.
	 * <li>rw+</li>
	 * open the archive file for read and wirte, if the file is exits, open the
	 * file.
	 * <li>rwt</li>
	 * open the archive file for read and write. The exits file will be removed.
	 * The file will be removed after close.
	 * 
	 * @param mode
	 *            the open mode.
	 */
	private void setupArchiveMode( String mode )
	{
		if ( "r".equals( mode ) )
		{
			isWritable = false;
			isTransient = false;
			isAppend = false;
		}
		else if ( "rw".equals( mode ) )
		{
			isWritable = true;
			isTransient = false;
			isAppend = false;
		}
		else if ( "rw+".equals( mode ) )
		{
			isWritable = true;
			isTransient = false;
			isAppend = true;
		}
		else if ( "rwt".equals( mode ) )
		{
			isWritable = true;
			isTransient = true;
			isAppend = false;
		}
		else
		{
			throw new IllegalArgumentException( );
		}
	}

	/**
	 * create the archive file.
	 * 
	 * @param fileName
	 *            file name.
	 * @param rf
	 *            the random access file
	 * @param mode
	 *            open mode.
	 * @throws IOException
	 */

	public ArchiveFileV2( String fileName, RandomAccessFile rf, String mode )
			throws IOException
	{
		this( null, null, fileName, rf, mode );
	}

	public ArchiveFileV2( String fileName, String mode ) throws IOException
	{
		this( null, null, fileName, null, mode );
	}

	public ArchiveFileV2( String systemId, String fileName, String mode )
			throws IOException
	{
		this( systemId, null, fileName, null, mode );
	}

	public ArchiveFileV2( String systemId, String dependId, String fileName,
			String mode ) throws IOException
	{
		this( systemId, dependId, fileName, null, mode );
	}

	/**
	 * create the archive file.
	 * 
	 * @param fileName
	 *            file name.
	 * @param mode
	 *            open mode.
	 * @throws IOException
	 */
	public ArchiveFileV2( String systemId, String dependId, String fileName,
			RandomAccessFile rf, String mode ) throws IOException
	{
		if ( fileName == null || fileName.length( ) == 0 )
			throw new IOException( "The file name is null or empty string." );

		// make sure the file name is an absolute path
		File fd = new File( fileName );
		fileName = fd.getCanonicalPath( );
		this.archiveName = fileName;
		this.rf = rf;
		this.systemId = systemId;
		this.dependId = dependId;
		this.caches = new FileCacheManager( );
		caches.setCacheListener( new ArchiveFileV2CacheListener( ) );
		caches.setSystemCacheManager( ArchiveFile.systemCacheManager );

		setupArchiveMode( mode );

		if ( isWritable && !isAppend )
		{
			// rw mode
			createDocument( );
		}
		else if ( isWritable && isAppend )
		{
			// rw+ mode
			if ( !( new File( fileName ) ).exists( ) )
			{
				createDocument( );
			}
			else
			{
				openDocument( );
			}
		}
		else
		{
			openDocument( );
		}

		isClosed = false;
	}

	/**
	 * set up the cache size.
	 * 
	 * the actually cache size is round to block size.
	 * 
	 * @param cacheSize
	 *            cache size in bytes
	 */
	public void setCacheSize( int cacheSize )
	{
		int cacheBlocks = ( cacheSize + BLOCK_SIZE - 1 ) / BLOCK_SIZE;
		caches.setMaxCacheSize( cacheBlocks );
	}

	public int getUsedCache( )
	{
		return caches.getUsedCacheSize( ) * BLOCK_SIZE;
	}

	public String getDependId( )
	{
		return dependId;
	}

	public String getSystemId( )
	{
		if ( systemId == null )
		{
			return archiveName;
		}
		return systemId;
	}

	/**
	 * open the archive file for read or rw.
	 * 
	 * @throws IOException
	 */
	private void openDocument( ) throws IOException
	{
		try
		{
			if ( rf == null )
			{
				if ( !isWritable && !useNativeLock )
				{
					rf = new RandomAccessFile( archiveName, "r" );
				}
				else
				{
					ensureParentFolderCreated( );
					rf = new RandomAccessFile( archiveName, "rw" );
				}
			}

			head = ArchiveHeader.read( rf );
			if ( systemId == null )
			{
				systemId = head.systemId;
			}
			if ( dependId == null )
			{
				dependId = head.dependId;
			}
			BLOCK_SIZE = head.blockSize;
			totalBlocks = (int) ( ( rf.length( ) + BLOCK_SIZE - 1 ) / BLOCK_SIZE );
			totalDiskBlocks = totalBlocks;
			allocTbl = AllocTable.loadTable( this );
			entryTbl = NameTable.loadTable( this );
			entries = new HashMap( );
			Iterator iter = entryTbl.listEntries( ).iterator( );
			while ( iter.hasNext( ) )
			{
				NameEntry nameEnt = (NameEntry) iter.next( );
				entries.put( nameEnt.getName( ), new ArchiveEntryV2( this,
						nameEnt ) );
			}
		}
		catch ( IOException ex )
		{
			if ( rf != null )
			{
				rf.close( );
				rf = null;
			}
			throw ex;
		}
	}

	/**
	 * create the document
	 * 
	 * @throws IOException
	 */
	private void createDocument( ) throws IOException
	{
		try
		{
			if ( !isTransient )
			{
				ensureFileCreated( );
				rf.setLength( 0 );
			}

			BLOCK_SIZE = getDefaultBlockSize( );
			totalBlocks = 3;
			totalDiskBlocks = 0;
			head = new ArchiveHeader( BLOCK_SIZE );
			head.flush( this );
			allocTbl = AllocTable.createTable( this );
			entryTbl = NameTable.createTable( this );
			entries = new HashMap( );
		}
		catch ( IOException ex )
		{
			if ( rf != null )
			{
				rf.close( );
				rf = null;
			}
			throw ex;
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
	public synchronized void close( ) throws IOException
	{
		if ( isWritable )
		{
			head.setStatus( FILE_STATUS_FINISHED );
			if ( !isTransient )
			{
				flush( );
			}
		}
		if ( rf != null )
		{
			rf.close( );
			rf = null;
		}
		if ( isTransient )
		{
			new File( archiveName ).delete( );
		}
		if ( caches != null )
		{
			caches.clear( );
		}
		isClosed = true;
	}

	public synchronized void flush( ) throws IOException
	{
		assertWritable( );
		if ( !isTransient )
		{
			head.flush( this );
			entryTbl.flush( );
			allocTbl.flush( );
			if ( caches != null )
			{
				caches.touchAllCaches( new ArchiveFileV2CacheListener( ) );
			}
		}
	}

	public synchronized void save( ) throws IOException
	{
		assertWritable( );
		if ( isTransient )
		{
			isTransient = false;
		}
		flush( );
	}

	public synchronized void refresh( ) throws IOException
	{
		assertOpen( );
		if ( !isWritable )
		{
			totalBlocks = (int) ( ( rf.length( ) + BLOCK_SIZE - 1 ) / BLOCK_SIZE );
			totalDiskBlocks = totalBlocks;
			head.refresh( this );
			allocTbl.refresh( );
			entryTbl.refresh( );
		}

	}

	public synchronized boolean exists( String name )
	{
		return entries.containsKey( name );
	}

	public synchronized ArchiveEntry openEntry( String name )
			throws IOException
	{
		ArchiveEntryV2 entry = entries.get( name );
		if ( entry != null )
		{
			return entry;
		}
		throw new FileNotFoundException( name );
	}

	public synchronized List listEntries( String namePattern )
	{
		ArrayList<String> list = new ArrayList<String>( );
		Iterator<String> iter = entries.keySet( ).iterator( );
		while ( iter.hasNext( ) )
		{
			String name = iter.next( );
			if ( namePattern == null || name.startsWith( namePattern ) )
			{
				list.add( name );
			}
		}
		return list;
	}

	public synchronized ArchiveEntry createEntry( String name )
			throws IOException
	{
		assertWritable( );

		ArchiveEntryV2 entry = entries.get( name );
		if ( entry != null )
		{
			entry.setLength( 0L );
			return entry;
		}
		NameEntry nameEnt = entryTbl.createEntry( name );
		entry = new ArchiveEntryV2( this, nameEnt );
		entries.put( name, entry );
		return entry;
	}

	public synchronized boolean removeEntry( String name ) throws IOException
	{
		assertWritable( );

		ArchiveEntryV2 entry = (ArchiveEntryV2) entries.get( name );
		if ( entry != null )
		{
			entries.remove( name );
			entryTbl.removeEntry( entry.entry );
			if ( entry.index != null )
			{
				allocTbl.removeEntry( entry.index );
			}
			return true;
		}
		return false;
	}

	/**
	 * should use the native file lock to synchronize the reader/writer
	 */
	private boolean useNativeLock = false;

	synchronized public Object lockEntry( String name ) throws IOException
	{
		assertOpen( );

		ArchiveEntryV2 entry = entries.get( name );
		if ( entry == null )
		{
			if ( !isWritable )
			{
				throw new FileNotFoundException( name );
			}
			entry = (ArchiveEntryV2) createEntry( name );
		}
		if ( useNativeLock )
		{
			if ( !isTransient )
			{
				entry.ensureSize( 1 );
				int blockId = entry.index.getBlock( 0 );
				return rf.getChannel( ).lock( blockId * BLOCK_SIZE, 1, false );
			}
		}
		return entry;
	}

	synchronized public void unlockEntry( Object locker ) throws IOException
	{
		assertOpen( );
		if ( locker instanceof FileLock )
		{
			FileLock flck = (FileLock) locker;
			flck.release( );
		}
		if ( !( locker instanceof ArchiveEntry ) )
		{
			throw new IOException( "Invalide lock type:" + locker );
		}
	}

	/**
	 * return the total blocks of the archive file.
	 * 
	 * @return
	 * @throws IOException
	 */
	int getTotalBlocks( )
	{
		return totalBlocks;
	}

	int allocateBlock( ) throws IOException
	{
		assertWritable( );
		return totalBlocks++;
	}

	private void assertWritable( ) throws IOException
	{
		assertOpen( );
		if ( !isWritable )
		{
			throw new IOException(
					"Archive must be opend for write. System ID: " + systemId );
		}
	}

	private void assertOpen( ) throws IOException
	{
		if ( isClosed )
		{
			throw new IOException( "The archive is closed. System ID: "
					+ systemId );
		}
	}

	/**
	 * read the data from cache.
	 * 
	 * This API read <code>len</code> bytes from <code>blockOff</code> in block
	 * <code>blockId</code>, store the data into <code>b</code> from
	 * <code>off</code>. The read cache is identified by <code>slotId</code>
	 * 
	 * @param blockId
	 *            the block id
	 * @param blockOff
	 *            the block offset
	 * @param b
	 *            read buffer
	 * @param off
	 *            buffer offset
	 * @param len
	 *            read length
	 * @throws IOException
	 */
	synchronized int read( int blockId, int blockOff, byte[] b, int off, int len )
			throws IOException
	{
		assertOpen( );
		long pos = (long) blockId * BLOCK_SIZE + blockOff;
		long fileLength = rf.length( );
		if ( pos + len > fileLength )
		{
			len = (int) ( fileLength - pos );
		}
		rf.seek( pos );
		rf.readFully( b, off, len );
		return len;
	}

	/**
	 * write the data into cache.
	 * 
	 * The API saves <code>len</code> bytes in <code>b</code> from
	 * <code>off</code> to block <code>blockId</code> from <code>blockOff</code>
	 * 
	 * @param blockId
	 *            block id.
	 * @param blockOff
	 *            offset in the block.
	 * @param b
	 *            data to be saved
	 * @param off
	 *            offset.
	 * @param len
	 *            write size.
	 * @throws IOException
	 */
	synchronized void write( int blockId, int blockOff, byte[] b, int off,
			int len ) throws IOException
	{
		assertWritable( );
		ensureFileCreated( );
		long pos = (long) blockId * BLOCK_SIZE + blockOff;
		rf.seek( pos );
		rf.write( b, off, len );
	}

	synchronized protected Block createBlock( ) throws IOException
	{
		int blockId = allocateBlock( );
		Block block = new Block( this, blockId, BLOCK_SIZE );
		caches.addCache( block );
		return block;
	}

	synchronized protected void unloadBlock( Block block ) throws IOException
	{
		caches.releaseCache( block );
	}

	synchronized Block loadBlock( int blockId ) throws IOException
	{
		Object cacheKey = Integer.valueOf( blockId );
		Block block = (Block) caches.getCache( cacheKey );
		if ( block == null )
		{
			block = new Block( this, blockId, BLOCK_SIZE );
			block.refresh( );
			caches.addCache( block );
		}
		return block;
	}

	private void ensureFileCreated( ) throws IOException
	{
		if ( rf != null )
		{
			return;
		}
		ensureParentFolderCreated( );

		if ( isWritable )
		{
			rf = new RandomAccessFile( archiveName, "rw" );
			rf.setLength( 0 );
		}
	}

	private void ensureParentFolderCreated( )
	{
		// try to create the parent folder
		File parentFile = new File( archiveName ).getParentFile( );
		if ( parentFile != null && !parentFile.exists( ) )
		{
			parentFile.mkdirs( );
		}
	}

	int getDefaultBlockSize( )
	{
		String value = (String) AccessController
				.doPrivileged( new PrivilegedAction<Object>( ) {

					public Object run( )
					{
						return System.getProperty( PROPERTY_DEFAULT_BLOCK_SIZE );
					}
				} );

		if ( value != null )
		{
			try
			{
				int defaultBlockSize = Integer.parseInt( value );
				defaultBlockSize = ( defaultBlockSize + 1023 ) / 1024 * 1024;
				if ( defaultBlockSize > 0 )
				{
					return defaultBlockSize;
				}
			}
			catch ( Exception ex )
			{
				// just skip the exception
			}
		}
		return DEFAULT_BLOCK_SIZE;
	}

	static class ArchiveFileV2CacheListener implements CacheListener
	{

		public void onCacheRelease( Cacheable cache )
		{
			Block block = (Block) cache;
			try
			{
				block.flush( );
			}
			catch ( IOException ex )
			{
				ex.printStackTrace( );
			}
		}
	}

}
