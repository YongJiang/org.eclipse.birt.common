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

package org.eclipse.birt.core.script.functionservice.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.FrameworkException;
import org.eclipse.birt.core.framework.IConfigurationElement;
import org.eclipse.birt.core.framework.IExtension;
import org.eclipse.birt.core.framework.IExtensionPoint;
import org.eclipse.birt.core.framework.IExtensionRegistry;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.core.script.functionservice.IScriptFunction;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionArgument;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionCategory;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.osgi.framework.Bundle;

/**
 * 
 */

public class FunctionProvider
{
	//The extension constants
	private static final String EXTENSION_POINT = "org.eclipse.birt.core.ScriptFunctionService";
	private static final String ELEMENT_CATEGORY = "Category";
	private static final String ELEMENT_FUNCTION = "Function";
	private static final String ELEMENT_ARGUMENT = "Argument";
	private static final String ELEMENT_JSLIB = "JSLib";
	private static final String ELEMENT_DATATYPE = "DataType";

	private static final String ATTRIBUTE_NAME = "name";
	private static final String ATTRIBUTE_DESC = "desc";
	private static final String ATTRIBUTE_FACTORYCLASS = "factoryclass";
	private static final String ATTRIBUTE_VALUE = "value";
	private static final String ATTRIBUTE_ISOPTIONAL = "isOptional";
	private static final String ATTRIBUTE_ALLOWVARARGUMENT = "variableArguments";
	private static final String ATTRIBUTE_ISSTATIC="isStatic";
	private static final String ATTRIBUTE_ISCONSTRUCTOR="isConstructor";
	private static final String ATTRIBUTE_LOCATION = "location";
	
	private static final String DEFAULT_CATEGORYNAME = null;

	private static Map<String, Category> categories;
	private static List<URL> jsLibs = new ArrayList<URL>( );
/*
	static
	{
		getCategoryMap( );
	}*/

	/**
	 * Return all the categories defined by extensions.
	 * 
	 * @return
	 * @throws BirtException
	 */
	public static IScriptFunctionCategory[] getCategories( )
			throws BirtException
	{
		return getCategoryMap( ).values( )
				.toArray( new IScriptFunctionCategory[]{} );
	}

	/**
	 * Return the functions that defined in a category.
	 * 
	 * @param categoryName
	 * @return
	 * @throws BirtException
	 */
	public static IScriptFunction[] getFunctions( String categoryName )
			throws BirtException
	{
		if ( getCategoryMap( ).containsKey( categoryName ) )
		{
			Category category = getCategoryMap( ).get( categoryName );
			return category.getFunctions( );
		}

		return new IScriptFunction[0];
	}

	/**
	 * Register script functions to scope.
	 * 
	 * @param cx
	 * @param scope
	 * @throws BirtException
	 */
	public static void registerScriptFunction( Context cx, Scriptable scope )
			throws BirtException
	{
		List<CategoryWrapper> wrapperedCategories = getWrapperedCategories( );
		for ( CategoryWrapper category : wrapperedCategories )
		{
			ScriptableObject.putProperty( scope,
					category.getClassName( ),
					category );
		}

		for ( URL url : jsLibs )
		{
			Script script;
			try
			{
				script = cx.compileReader( new BufferedReader( new InputStreamReader( url.openStream( ) ) ),
						null,
						0,
						null );
				script.exec( cx, scope );
			}
			catch ( IOException e )
			{
			}
		}
	}

	/**
	 * Return the category map.
	 * 
	 * @return
	 */
	private static Map<String, Category> getCategoryMap( )
	{
		if ( categories != null )
			return categories;

		categories = new HashMap<String, Category>( );
		
		//Find the extension point.
		IExtensionRegistry extReg = Platform.getExtensionRegistry( );
		IExtensionPoint extPoint = extReg.getExtensionPoint( EXTENSION_POINT );

		if ( extPoint == null )
			return categories;

		//Fetch all extensions
		IExtension[] exts = extPoint.getExtensions( );
		if ( exts == null )
		{
			return categories;
		}

		//populate category map as per extension.
		for ( int e = 0; e < exts.length; e++ )
		{
			IConfigurationElement[] configElems = exts[e].getConfigurationElements( );
			if ( configElems == null )
				continue;

			for ( int i = 0; i < configElems.length; i++ )
			{
				//for element Category
				if ( configElems[i].getName( ).equals( ELEMENT_CATEGORY ) )
				{
					Category category = new Category( configElems[i].getAttribute( ATTRIBUTE_NAME ),
							configElems[i].getAttribute( ATTRIBUTE_DESC ) );
					categories.put( category.getName( ), category );

					IScriptFunctionFactory factory = null;
					try
					{
						if( configElems[i].getAttribute( ATTRIBUTE_FACTORYCLASS )!= null )
							factory = (IScriptFunctionFactory) configElems[i].createExecutableExtension( ATTRIBUTE_FACTORYCLASS );
						IConfigurationElement[] functions = configElems[i].getChildren( ELEMENT_FUNCTION );
						for ( int j = 0; j < functions.length; j++ )
						{
							IScriptFunction function = getScriptFunction( category,
									factory,
									functions[j] );
							if( function!= null )
								category.addFunction( function );
						}
					}
					catch ( FrameworkException e1 )
					{
					}

				}
				//For element function that are not under certain category. Usually those functions are
				//defined in .js file
				else if ( configElems[i].getName( ).equals( ELEMENT_FUNCTION ) )
				{
					if ( categories.get( DEFAULT_CATEGORYNAME ) == null )
					{
						categories.put( DEFAULT_CATEGORYNAME,
								new Category( DEFAULT_CATEGORYNAME, null ) );
					}
					IScriptFunction function = getScriptFunction( categories.get( DEFAULT_CATEGORYNAME ),
							null,
							configElems[i] );
					if ( function != null )
						categories.get( DEFAULT_CATEGORYNAME )
								.addFunction( function );
				}
				//Populate the .js script library
				else if ( configElems[i].getName( ).equals( ELEMENT_JSLIB ) )
				{
					populateResources( jsLibs, ".js", configElems[i] );
				}
			}
		}
		return categories;
	}

	/**
	 * Populate library resources. The library resources includes .js script lib and .jar java lib.
	 * @param libs
	 * @param suffix
	 * @param confElement
	 */
	private static void populateResources( List<URL> libs, String suffix,
			IConfigurationElement confElement )
	{
		String source = confElement.getAttribute( ATTRIBUTE_LOCATION );
		Bundle bundle = org.eclipse.core.runtime.Platform.getBundle( confElement.getDeclaringExtension( )
				.getNamespace( ) );
		if ( bundle != null )
		{
			URL url = bundle.getEntry( source );
			Enumeration files = bundle.getEntryPaths( source );
			
			if ( files != null )
			{
				//In this case, the bundle denotes to a directory.
				while ( files.hasMoreElements( ) )
				{
					String element = files.nextElement( ).toString( );
					if ( element.toLowerCase( ).endsWith( suffix ) )
					{
						try
						{
							libs.add( new URL( url, element ) );
						}
						catch ( MalformedURLException e1 )
						{
						}
					}
				}
			}
			else
			{
				//the bundle denotes to a file.
				if ( url.getFile( ).toLowerCase( ).endsWith( suffix ) )
					libs.add( bundle.getEntry( source ) );
			}
		}
	}

	/**
	 * Create script function out of a function element.
	 * @param category
	 * @param factory
	 * @param function
	 * @return
	 */
	private static IScriptFunction getScriptFunction( Category category,
			IScriptFunctionFactory factory, IConfigurationElement function )
	{
		try
		{
			//Function name
			String name = function.getAttribute( ATTRIBUTE_NAME );
			//Function Desc
			String desc = function.getAttribute( ATTRIBUTE_DESC );
			//Allow var argument
			String varArgs = function.getAttribute( ATTRIBUTE_ALLOWVARARGUMENT );
			boolean allowVarArgs = extractBoolean( varArgs, false );
			boolean isConstructor = extractBoolean( function.getAttribute( ATTRIBUTE_ISCONSTRUCTOR ), false);
			boolean isStatic = extractBoolean( function.getAttribute( ATTRIBUTE_ISSTATIC ), true);
			String dataType = null;
			List<IScriptFunctionArgument> arguments = new ArrayList<IScriptFunctionArgument>( );
			//Populate function return data type info.
			if ( hasChildren( ELEMENT_DATATYPE, function ) )
			{
				dataType = function.getChildren( ELEMENT_DATATYPE )[0].getAttribute( ATTRIBUTE_VALUE );
			}
			
			//Popualte function argument info
			if ( hasChildren( ELEMENT_ARGUMENT, function ) )
			{
				for ( int i = 0; i < function.getChildren( ELEMENT_ARGUMENT ).length; i++ )
				{
					arguments.add( getScriptFunctionArgument( function.getChildren( ELEMENT_ARGUMENT )[i] ) );
				}
			}
			return new ScriptFunction( name,
					category,
					arguments.toArray( new IScriptFunctionArgument[0] ),
					dataType,
					desc,
					factory == null ? null : factory.getFunctionExecutor( name ),
					allowVarArgs,
					isStatic,
					isConstructor );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	private static boolean extractBoolean( String varArgs, boolean ifNull )
			throws BirtException
	{
		boolean allowVarArgs = varArgs == null ? ifNull
				: DataTypeUtil.toBoolean( varArgs );
		return allowVarArgs;
	}

	/**
	 * Populate function argument.
	 * @param argument
	 * @return
	 * @throws BirtException
	 */
	private static IScriptFunctionArgument getScriptFunctionArgument(
			IConfigurationElement argument ) throws BirtException
	{
		//
		String name = argument.getAttribute( ATTRIBUTE_NAME );
		String desc = argument.getAttribute( ATTRIBUTE_DESC );

		//populate whether it is optional argument.
		String optional = argument.getAttribute( ATTRIBUTE_ISOPTIONAL );
		boolean isOptional = extractBoolean( optional, false );

		String dataType = null;

		//Populate data type
		if ( hasChildren( ELEMENT_DATATYPE, argument ) )
		{
			dataType = argument.getChildren( ELEMENT_DATATYPE )[0].getAttribute( ATTRIBUTE_VALUE );
		}

		return new Argument( name,
				dataType,
				desc,
				isOptional );
	}

	/**
	 * 
	 * @param name
	 * @param element
	 * @return
	 */
	private static boolean hasChildren( String name,
			IConfigurationElement element )
	{
		IConfigurationElement[] children = element.getChildren( name );
		return children != null && children.length > 0;
	}

	/**
	 * Create category wrapper.
	 * 
	 * @return
	 * @throws BirtException
	 */
	private static List<CategoryWrapper> getWrapperedCategories( )
			throws BirtException
	{
		List<CategoryWrapper> result = new ArrayList<CategoryWrapper>( );

		for ( Category category : getCategoryMap( ).values( ) )
		{
			if ( category.getName( ) != DEFAULT_CATEGORYNAME )
				result.add( new CategoryWrapper( category ) );
		}
		return result;
	}

}
