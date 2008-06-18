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

package org.eclipse.birt.core.archive.compound;

import java.io.IOException;

class ArchiveEntryV2 extends ArchiveEntry implements ArchiveConstants
{
	protected final int BLOCK_SIZE;
	protected int cachId;
	protected ArchiveFileV2 af;
	protected NameEntry entry;
	protected AllocEntry index;

	ArchiveEntryV2( ArchiveFileV2 af, NameEntry entry ) throws IOException
	{
		this.af = af;
		this.BLOCK_SIZE = af.BLOCK_SIZE;
		this.entry = entry;
		this.cachId = entry.getBlock( );
		if ( cachId != -1 )
		{
			index = af.allocTbl.loadEntry( cachId );
		}
	}

	public synchronized long getLength( ) throws IOException
	{
		return entry.getLength( );
	}

	public synchronized void setLength( long length ) throws IOException
	{
		ensureSize( length );
		entry.setLength( length );
	}

	public synchronized void flush( ) throws IOException
	{
		// TODO: support flush in future
	}

	public synchronized void refresh( ) throws IOException
	{
		// TODO: support refresh in future.
	}

	public Object lock( ) throws IOException
	{
		return af.lockEntry( this );
	}

	public void unlock( Object lock ) throws IOException
	{
		af.unlockEntry( lock );
	}

	public synchronized int read( long pos, byte[] b, int off, int len )
			throws IOException
	{
		long length = entry.getLength( );

		if ( pos >= length )
		{
			return -1;
		}

		if ( pos + len > length )
		{
			len = (int) ( length - pos );
		}

		if ( len == 0 )
		{
			return 0;
		}

		// read first block
		int blockId = (int) ( pos / BLOCK_SIZE );
		int blockOff = (int) ( pos % BLOCK_SIZE );
		int readSize = BLOCK_SIZE - blockOff;
		if ( len < readSize )
		{
			readSize = len;
		}
		int phyBlockId = index.getBlock( blockId );
		af.read( phyBlockId, blockOff, b, off, readSize );
		int remainSize = len - readSize;

		// read blocks
		while ( remainSize >= BLOCK_SIZE )
		{
			blockId++;
			phyBlockId = index.getBlock( blockId );
			af.read( phyBlockId, 0, b, off + readSize, BLOCK_SIZE );
			readSize += BLOCK_SIZE;
			remainSize -= BLOCK_SIZE;
		}

		// read remain blocks
		if ( remainSize > 0 )
		{
			blockId++;
			phyBlockId = index.getBlock( blockId );
			af.read( phyBlockId, 0, b, off + readSize, remainSize );
			readSize += remainSize;
		}

		return readSize;
	}

	public synchronized void write( long pos, byte[] b, int off, int len )
			throws IOException
	{
		ensureSize( pos + len );

		if ( len == 0 )
		{
			return;
		}
		int blockId = (int) ( pos / BLOCK_SIZE );
		int phyBlockId = index.getBlock( blockId );
		int blockOff = (int) ( pos % BLOCK_SIZE );
		int writeSize = BLOCK_SIZE - blockOff;
		if ( len < writeSize )
		{
			writeSize = len;
		}
		af.write( phyBlockId, blockOff, b, off, writeSize );
		int remainSize = len - writeSize;

		// write blocks
		while ( remainSize >= BLOCK_SIZE )
		{
			blockId++;
			phyBlockId = index.getBlock( blockId );
			af.write( phyBlockId, 0, b, off + writeSize, BLOCK_SIZE );
			writeSize += BLOCK_SIZE;
			remainSize -= BLOCK_SIZE;
		}

		// write remain blocks
		if ( remainSize > 0 )
		{
			blockId++;
			phyBlockId = index.getBlock( blockId );
			af.write( phyBlockId, 0, b, off + writeSize, remainSize );
		}

		long length = entry.getLength( );
		long offset = pos + len;
		if ( length < offset )
		{
			setLength( offset );
		}
	}

	protected void ensureSize( long newLength ) throws IOException
	{
		if ( index == null )
		{
			index = af.allocTbl.createEntry( );
			entry.setBlock( index.getFirstBlock( ) );
		}
		int blockCount = (int) ( ( newLength + BLOCK_SIZE - 1 ) / BLOCK_SIZE );
		int totalBlock = index.getTotalBlocks( );
		if ( blockCount > totalBlock )
		{
			while ( totalBlock < blockCount )
			{
				int freeBlock = af.allocTbl.getFreeBlock( );
				index.appendBlock( freeBlock );
				totalBlock++;
			}
		}
	}
}