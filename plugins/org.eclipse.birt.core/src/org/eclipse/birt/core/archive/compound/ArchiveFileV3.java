/*******************************************************************************
 * Copyright (c) 2009 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.core.archive.compound;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.core.archive.compound.v3.Ext2Entry;
import org.eclipse.birt.core.archive.compound.v3.Ext2File;
import org.eclipse.birt.core.archive.compound.v3.Ext2FileSystem;
import org.eclipse.birt.core.archive.compound.v3.Ext2Node;

public class ArchiveFileV3 implements IArchiveFile
{

	public static final String PROPERTY_SYSTEM_ID = "archive.system-id";
	public static final String PROPERTY_DEPEND_ID = "archive.depened-id";

	protected Ext2FileSystem fs;

	public ArchiveFileV3( String fileName, String mode ) throws IOException

	{
		this( fileName, null, mode );
	}

	public ArchiveFileV3( String fileName, RandomAccessFile rf, String mode )
			throws IOException

	{
		fs = new Ext2FileSystem( fileName, rf, mode );
		if ( ArchiveFile.enableSystemCache )
		{
			fs.setCacheManager( ArchiveFile.systemCacheManager );
		}
		if ( ArchiveFile.enableFileCache && fs.isRemoveOnExit( ) )
		{
			fs.setCacheSize( ArchiveFile.FILE_CACHE_SIZE * 4096 );
		}
	}

	public void close( ) throws IOException
	{
		if ( fs != null )
		{
			fs.close( );
			fs = null;
		}
	}

	public void setSystemId( String id )
	{
		fs.setProperty( PROPERTY_SYSTEM_ID, id );
	}

	public void setDependId( String id )
	{
		fs.setProperty( PROPERTY_DEPEND_ID, id );
	}

	public ArchiveEntry createEntry( String name ) throws IOException
	{
		Ext2File file = fs.createFile( name );
		return new ArchiveEntryV3( file );
	}

	public boolean exists( String name )
	{
		return fs.existFile( name );
	}

	public void flush( ) throws IOException
	{
		fs.flush( );
	}

	public String getDependId( )
	{
		return fs.getProperty( PROPERTY_DEPEND_ID );
	}

	public ArchiveEntry openEntry( String name ) throws IOException
	{
		if ( fs.existFile( name ) )
		{
			Ext2File file = fs.openFile( name );
			return new ArchiveEntryV3( file );
		}
		throw new FileNotFoundException( name );
	}

	public String getName( )
	{
		return fs.getFileName( );
	}

	public String getSystemId( )
	{
		return fs.getProperty( PROPERTY_SYSTEM_ID );
	}

	public long getUsedCache( )
	{
		return (long) fs.getUsedCacheSize( ) * 4096;
	}

	public List listEntries( String namePattern )
	{
		ArrayList<String> files = new ArrayList<String>( );
		for ( String file : fs.listFiles( ) )
		{
			if ( file.startsWith( namePattern ) )
			{
				files.add( file );
			}
		}
		return files;
	}

	public synchronized Object lockEntry( String name ) throws IOException
	{
		if ( !fs.existFile( name ) )
		{
			if ( !fs.isReadOnly( ) )
			{
				Ext2File file = fs.createFile( name );
				file.close( );
			}
		}
		Ext2Entry entry = fs.getEntry( name );
		if ( entry != null )
		{
			return entry;
		}
		throw new FileNotFoundException( name );
	}

	public void refresh( ) throws IOException
	{
	}

	public boolean removeEntry( String name ) throws IOException
	{
		fs.removeFile( name );
		return true;
	}

	public void save( ) throws IOException
	{
		fs.setRemoveOnExit( false );
		fs.flush( );
	}

	public void setCacheSize( long cacheSize )
	{
		long cacheBlock = cacheSize / 4096;
		if ( cacheBlock > Integer.MAX_VALUE )
		{
			fs.setCacheSize( Integer.MAX_VALUE );
		}
		else
		{
			fs.setCacheSize( (int) cacheBlock );
		}
	}

	synchronized public void unlockEntry( Object locker ) throws IOException
	{
		assert ( locker instanceof Ext2Entry );
	}

	private static class ArchiveEntryV3 extends ArchiveEntry
	{

		Ext2File file;

		ArchiveEntryV3( Ext2File file )
		{
			this.file = file;
		}

		public String getName( )
		{
			return file.getName( );
		}

		public long getLength( ) throws IOException
		{
			return file.length( );
		}

		public void close( ) throws IOException
		{
			file.close( );
		}

		@Override
		public void flush( ) throws IOException
		{
		}

		@Override
		public int read( long pos, byte[] b, int off, int len )
				throws IOException
		{
			file.seek( pos );
			return file.read( b, off, len );
		}

		@Override
		public void refresh( ) throws IOException
		{
		}

		@Override
		public void setLength( long length ) throws IOException
		{
			file.setLength( length );
		}

		@Override
		public void write( long pos, byte[] b, int off, int len )
				throws IOException
		{
			file.seek( pos );
			file.write( b, off, len );
		}
	}
}
