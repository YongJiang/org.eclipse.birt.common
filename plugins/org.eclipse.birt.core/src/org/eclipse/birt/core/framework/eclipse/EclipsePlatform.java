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

package org.eclipse.birt.core.framework.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.eclipse.birt.core.framework.IBundle;
import org.eclipse.birt.core.framework.IConfigurationElement;
import org.eclipse.birt.core.framework.IExtension;
import org.eclipse.birt.core.framework.IExtensionPoint;
import org.eclipse.birt.core.framework.IExtensionRegistry;
import org.eclipse.birt.core.framework.IPlatform;
import org.eclipse.birt.core.framework.IPlatformPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * 
 * @version $Revision: 1.8 $ $Date: 2005/07/07 00:26:36 $
 */
public class EclipsePlatform implements IPlatform
{

	public EclipsePlatform( )
	{
	}

	public IExtensionRegistry getExtensionRegistry( )
	{
		return new EclipseExtensionRegistry( Platform.getExtensionRegistry( ) );
	}

	public IBundle getBundle( String symbolicName )
	{
		Bundle bundle = Platform.getBundle( symbolicName );
		if ( bundle != null )
		{
			return new EclipseBundle( bundle );
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.core.framework.IPlatform#find(org.eclipse.birt.core.framework.IBundle,
	 *      org.eclipse.core.runtime.IPath)
	 */
	public URL find( IBundle bundle, IPlatformPath path )
	{
		if ( ( bundle instanceof EclipseBundle )
				&& ( path instanceof EclipsePath ) )
		{
			return Platform.find( ( (EclipseBundle) bundle ).bundle,
					( (EclipsePath) path ).path );
		}

		return null;
	}

	public URL asLocalURL( URL url ) throws IOException
	{
		return Platform.asLocalURL( url );
	}

	static IConfigurationElement wrap(
			org.eclipse.core.runtime.IConfigurationElement object )
	{
		return new EclipseConfigurationElement( object );
	}

	static IConfigurationElement[] wrap(
			org.eclipse.core.runtime.IConfigurationElement[] objects )
	{
		if ( objects == null )
		{
			return new IConfigurationElement[0];
		}
		IConfigurationElement[] wraps = new IConfigurationElement[objects.length];
		for ( int i = 0; i < objects.length; i++ )
		{
			wraps[i] = new EclipseConfigurationElement( objects[i] );
		}
		return wraps;
	}

	static IExtensionPoint wrap( org.eclipse.core.runtime.IExtensionPoint object )
	{
		return new EclipseExtensionPoint( object );
	}

	static IExtensionPoint[] wrap(
			org.eclipse.core.runtime.IExtensionPoint[] objects )
	{
		if ( objects == null )
		{
			return new IExtensionPoint[0];
		}
		IExtensionPoint[] wraps = new IExtensionPoint[objects.length];
		for ( int i = 0; i < objects.length; i++ )
		{
			wraps[i] = new EclipseExtensionPoint( objects[i] );
		}
		return wraps;
	}

	static IExtension wrap( org.eclipse.core.runtime.IExtension object )
	{
		return new EclipseExtension( object );
	}

	static IExtension[] wrap( org.eclipse.core.runtime.IExtension[] objects )
	{
		if ( objects == null )
		{
			return new IExtension[0];
		}
		IExtension[] wraps = new IExtension[objects.length];
		for ( int i = 0; i < objects.length; i++ )
		{
			wraps[i] = new EclipseExtension( objects[i] );
		}
		return wraps;
	}

	static Object wrap( Object object )
	{
		if ( object instanceof org.eclipse.core.runtime.IConfigurationElement )
		{
			return EclipsePlatform
					.wrap( (org.eclipse.core.runtime.IConfigurationElement) object );
		}
		else if ( object instanceof org.eclipse.core.runtime.IExtension )
		{
			return EclipsePlatform
					.wrap( (org.eclipse.core.runtime.IExtension) object );
		}
		else if ( object instanceof org.eclipse.core.runtime.IExtensionPoint )
		{
			return EclipsePlatform
					.wrap( (org.eclipse.core.runtime.IExtensionPoint) object );
		}
		return object;
	}

	/**
	 * get debug options.
	 * 
	 * call Eclipse's getDebugeOption directly.
	 * 
	 * @param option
	 *            option name
	 * @return option value
	 */
	public String getDebugOption( String option )
	{
		return Platform.getDebugOption( option );
	}

	/**
	 * setup logger used for tracing.
	 * 
	 * It reads ".options" in the plugin folder to get all the tracing items,
	 * call the .getDebugOptions() to get the option values and setup the logger
	 * use the values.
	 * 
	 * @param pluginId
	 *            plugin id
	 */
	public void initializeTracing( String pluginId )
	{

		Bundle bundle = org.eclipse.core.runtime.Platform.getBundle( pluginId );
		String debugFlag = pluginId + "/debug";
		if ( bundle != null )
		{

			try
			{
				URL optionUrl = bundle.getEntry( ".options" );
				InputStream in = optionUrl.openStream( );
				if ( in != null )
				{
					Properties options = new Properties( );
					options.load( in );
					Iterator entryIter = options.entrySet( ).iterator( );
					while ( entryIter.hasNext( ) )
					{
						Map.Entry entry = (Map.Entry) entryIter.next( );
						String option = (String) entry.getKey( );
						if (!debugFlag.equals(option))
						{
							String value = org.eclipse.core.runtime.Platform
									.getDebugOption( option );
							setupLogger( option, value );
						}
					}
				}
			}
			catch ( Exception ex )
			{
				ex.printStackTrace( );
			}
		}
	}

	/**
	 * setup logger
	 * 
	 * @param option
	 * @param value
	 */
	static void setupLogger( String option, String value )
	{
		// get the plugin name
		if ( "true".equals( value ) )
		{
			Level level = getLoggerLevel( option );
			String loggerName = getLoggerName( option );
			Logger logger = Logger.getLogger( loggerName );
			logger.addHandler( getTracingHandler( ) );
			logger.setLevel( level );
		}
	}

	/**
	 * get the logger level from the option.
	 * 
	 * It checks the option name, to see if it matches the rules:
	 * 
	 * .fine Logger.FINE .finer Logger.FINER .finest Logger.FINEST
	 * 
	 * others are Logger.FINE
	 * 
	 * @param option
	 *            option name
	 * @return logger level
	 */
	static protected Level getLoggerLevel( String option )
	{
		assert option != null;
		if ( option.endsWith( ".finer" ) )
		{
			return Level.FINER;
		}
		if ( option.endsWith( ".finest" ) )
		{
			return Level.FINEST;
		}
		return Level.FINE;
	}

	/**
	 * get the logger name from the option.
	 * 
	 * It get the logger name from the options: 1) remove any post fix from the
	 * option (.fine, .finest, .finer) 2) replace all '/' with '.' 3) trim
	 * spaces
	 * 
	 * @param option
	 *            option name
	 * @return the logger used to output the trace of that option
	 * 
	 */
	static protected String getLoggerName( String option )
	{
		assert option != null;
		if ( option.endsWith( ".fine" ) )
		{
			option = option.substring( 0, option.length( ) - 5 );
		}
		else if ( option.endsWith( "finer" ) )
		{
			option = option.substring( 0, option.length( ) - 6 );
		}
		else if ( option.endsWith( ".finest" ) )
		{
			option = option.substring( 0, option.length( ) - 7 );
		}
		return option.replace( '/', '.' ).trim( );
	}

	/**
	 * logger handler use to output the trace information.
	 */
	static StreamHandler tracingHandler;

	/**
	 * get the trace logger handle.
	 * 
	 * Trace logger handle output all the logging information to System.out
	 * 
	 * @return
	 */
	static StreamHandler getTracingHandler( )
	{
		if ( tracingHandler == null )
		{
			tracingHandler = new StreamHandler( System.out,
					new SimpleFormatter( ) );
			tracingHandler.setLevel( Level.ALL );
		}
		return tracingHandler;
	}

}
