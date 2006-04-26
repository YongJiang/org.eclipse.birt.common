/*******************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.core.framework;

import java.util.HashMap;

/**
 * Defines an implementation of IPlatformConfig to access OSGi framework.
 * implements the interface IPlatformConfig
 */
public class PlatformConfig implements IPlatformConfig
{

	/**
	 * the properties that needed when platfrom is running it's an instance of
	 * HashMap
	 */
	protected HashMap properties = new HashMap( );

	public Object getProperty( String name )
	{
		return properties.get( name );
	}

	public void setProperty( String name, Object value )
	{
		properties.put( name, value );
	}

	public String getBIRTHome( )
	{
		Object birtHome = properties.get( BIRT_HOME );
		if ( birtHome instanceof String )
		{
			return (String) birtHome;
		}
		return null;
	}

	public void getBIRTHome( String birtHome )
	{
		properties.put( BIRT_HOME, birtHome );
	}

	public String[] getOSGiArguments( )
	{
		Object arguments = properties.get( OSGI_ARGUMENTS );
		if ( arguments instanceof String[] )
		{
			return (String[]) arguments;
		}
		return null;
	}

	public void setOSGiArguments( String[] arguments )
	{
		properties.put( OSGI_ARGUMENTS, arguments );
	}

	public IPlatformContext getPlatformContext( )
	{
		Object context = properties.get( PLATFORM_CONTEXT );
		if ( context instanceof IPlatformContext )
		{
			return (IPlatformContext) context;
		}
		return null;
	}

	public void setPlatformContext( IPlatformContext context )
	{
		properties.put( PLATFORM_CONTEXT, context );
	}
}