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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.core.archive.ArchiveUtil;

/**
 * the archive file contains following mode:
 * <li> "r" open the file for read only.
 * <li> "rw" create the file for read/write
 * <li> "rw+" open file is open for read/write
 * <li> "rwt" create the trainsnt file, it will be removed after closing.
 */
class ArchiveFileV2 implements IArchiveFile, ArchiveConstants
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
	protected HashMap entries;

	/**
	 * cache manager of the archive file.
	 */
	protected BlockManager caches;

	/**
	 * the total blocks exits in this file
	 */
	protected int totalBlocks;

	/**
	 * the total blocks exits in the disk
	 */
	protected int totalDiskBlocks;

	/**
	 * if the caches is enabled, used for debug.
	 */
	private boolean enableCache = true;

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

	ArchiveFileV2( String fileName, RandomAccessFile rf, String mode )
			throws IOException
	{
		this( null, null, fileName, rf, mode );
	}

	ArchiveFileV2( String fileName, String mode )
			throws IOException
	{
		this( null, null, fileName, null, mode );
	}
	
	ArchiveFileV2( String systemId, String fileName, String mode )
			throws IOException
	{
		this( systemId, null, fileName, null, mode );
	}

	ArchiveFileV2( String systemId, String dependId, String fileName,
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
	ArchiveFileV2( String systemId, String dependId, String fileName, RandomAccessFile rf,
			String mode ) throws IOException
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
		caches.setCacheSize( cacheSize );
	}

	public int getUsedCache( )
	{
		return caches.getUsedCache( );
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
			caches = new BlockManager( new CacheEventAdapter( ), BLOCK_SIZE );

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
				if ( rf == null )
				{
					// try to create the parent folder
					File parentFile = new File( archiveName ).getParentFile( );
					if ( parentFile != null && !parentFile.exists( ) )
					{
						parentFile.mkdirs( );
					}

					rf = new RandomAccessFile( archiveName, "rw" );
				}
				rf.setLength( 0 );
			}

			BLOCK_SIZE = getDefaultBlockSize( );
			caches = new BlockManager( new CacheEventAdapter( ), BLOCK_SIZE );
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
		caches.reset( );
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
			caches.flush( );
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

	public synchronized ArchiveEntry getEntry( String name )
	{
		return (ArchiveEntry) entries.get( name );
	}

	public synchronized List listEntries( String namePattern )
	{
		ArrayList list = new ArrayList( );
		Iterator iter = entries.keySet( ).iterator( );
		while ( iter.hasNext( ) )
		{
			String name = (String) iter.next( );
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

		if ( !name.startsWith( ArchiveUtil.UNIX_SEPERATOR ) )
			name = ArchiveUtil.UNIX_SEPERATOR + name;

		ArchiveEntry entry = (ArchiveEntry) entries.get( name );
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

		if ( !name.startsWith( ArchiveUtil.UNIX_SEPERATOR ) )
			name = ArchiveUtil.UNIX_SEPERATOR + name;

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

	public Object lockEntry( ArchiveEntry entry ) throws IOException
	{
		assertOpen( );
		ArchiveEntryV2 entryV2 = (ArchiveEntryV2) entry;
		if ( useNativeLock )
		{
			if ( !isTransient )
			{
				entryV2.ensureSize( 1 );
				int blockId = entryV2.index.getBlock( 0 );
				return rf.getChannel( ).lock( blockId * BLOCK_SIZE, 1, false );
			}
		}
		return entry;
	}

	public void unlockEntry( Object locker ) throws IOException
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
			throw new IOException( "Archive must be opend for write" );
		}
	}

	private void assertOpen( ) throws IOException
	{
		if ( isClosed )
		{
			throw new IOException( "The archive is closed" );
		}
	}

	/**
	 * read the data from cache.
	 * 
	 * This API read <code>len</code> bytes from <code>blockOff</code> in
	 * block <code>blockId</code>, store the data into <code>b</code> from
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
	synchronized void read( int blockId, int blockOff, byte[] b, int off,
			int len ) throws IOException
	{
		assertOpen( );
		if ( enableCache )
		{
			Block block = caches.getBlock( blockId );
			block.read( blockOff, b, off, len );
		}
		else
		{
			long pos = blockId * BLOCK_SIZE + blockOff;
			rf.seek( pos );
			rf.readFully( b, off, len );
		}
	}

	/**
	 * write the data into cache.
	 * 
	 * The API saves <code>len</code> bytes in <code>b</code> from
	 * <code>off</code> to block <code>blockId</code> from
	 * <code>blockOff</code>
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
		if ( enableCache )
		{
			Block block = caches.getBlock( blockId );
			block.write( blockOff, b, off, len );
		}
		else
		{
			long pos = blockId * BLOCK_SIZE + blockOff;
			rf.seek( pos );
			rf.write( b, off, len );
		}
	}

	class CacheEventAdapter extends BlockManagerEventAdapter
	{

		public void flush( Block block ) throws IOException
		{
			if ( isWritable )
			{
				if ( rf == null )
				{
					// try to create the parent folder
					File parentFile = new File( archiveName ).getParentFile( );
					if ( parentFile != null && !parentFile.exists( ) )
					{
						parentFile.mkdirs( );
					}

					rf = new RandomAccessFile( archiveName, "rw" );
					rf.setLength( 0 );
				}
				block.flush( rf );
				if ( block.id >= totalDiskBlocks )
				{
					totalDiskBlocks = block.id + 1;
				}
			}
		}

		public void refresh( Block block ) throws IOException
		{
			assertOpen( );
			if ( block.id < totalDiskBlocks )
			{
				block.refresh( rf );
			}
		}
	}

	int getDefaultBlockSize( )
	{
		String value = System.getProperty( PROPERTY_DEFAULT_BLOCK_SIZE );
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

}
