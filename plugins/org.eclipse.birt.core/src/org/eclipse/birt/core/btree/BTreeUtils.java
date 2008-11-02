/*******************************************************************************
 * Copyright (c) 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.core.btree;

public class BTreeUtils
{

	static public int bytesToInteger( byte[] b )
	{
		return ( ( b[0] & 0xFF ) << 24 ) + ( ( b[1] & 0xFF ) << 16 )
				+ ( ( b[2] & 0xFF ) << 8 ) + ( ( b[3] & 0xFF ) << 0 );
	}

	static public void integerToBytes( int v, byte[] b )
	{
		b[0] = (byte) ( ( v >>> 24 ) & 0xFF );
		b[1] = (byte) ( ( v >>> 16 ) & 0xFF );
		b[2] = (byte) ( ( v >>> 8 ) & 0xFF );
		b[3] = (byte) ( ( v >>> 0 ) & 0xFF );
	}
}
