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

package org.eclipse.birt.core.archive.compound.v3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class EntryTable
{

	private Ext2FileSystem fs;
	private HashMap<String, Ext2Entry> entries;
	private boolean dirty;

	EntryTable( Ext2FileSystem fs )
	{
		this.fs = fs;
		this.entries = new HashMap<String, Ext2Entry>( );
		this.dirty = true;
	}

	void read( ) throws IOException
	{
		Ext2File file = new Ext2File( fs, NodeTable.INODE_ENTRY_TABLE, false );
		try
		{
			byte[] bytes = new byte[(int) file.length( )];
			file.read( bytes, 0, bytes.length );
			DataInputStream in = new DataInputStream( new ByteArrayInputStream(
					bytes ) );
			try
			{
				while ( true )
				{
					String name = in.readUTF( );
					int inode = in.readInt( );
					entries.put( name, new Ext2Entry( name, inode ) );
				}
			}
			catch ( EOFException ex )
			{
				// expect the EOF exception
			}

		}
		finally
		{
			file.close( );
		}
		this.dirty = false;
	}

	void write( ) throws IOException
	{
		if ( !dirty )
		{
			return;
		}
		dirty = false;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream( );
		DataOutputStream out = new DataOutputStream( buffer );
		String[] names = entries.keySet( )
				.toArray( new String[entries.size( )] );
		Arrays.sort( names, 0, names.length, new Comparator<String>( ) {

			public int compare( String o1, String o2 )
			{

				return o1.compareTo( o2 );
			}
		} );
		for ( int i = 0; i < names.length; i++ )
		{
			Ext2Entry entry = entries.get( names[i] );
			out.writeUTF( entry.name );
			out.writeInt( entry.inode );
		}

		Ext2File file = new Ext2File( fs, NodeTable.INODE_ENTRY_TABLE, false );
		try
		{
			byte[] bytes = buffer.toByteArray( );
			file.write( bytes, 0, bytes.length );
			file.setLength( bytes.length );
		}
		finally
		{
			file.close( );
		}
	}

	Ext2Entry getEntry( String name )
	{
		return entries.get( name );
	}

	Ext2Entry removeEntry( String name )
	{
		Ext2Entry entry = entries.remove( name );
		if ( entry != null )
		{
			dirty = true;
		}
		return entry;
	}

	void addEntry( Ext2Entry entry )
	{
		dirty = true;
		entries.put( entry.name, entry );
	}

	String[] listEntries( )
	{
		return entries.keySet( ).toArray( new String[entries.size( )] );
	}
}
