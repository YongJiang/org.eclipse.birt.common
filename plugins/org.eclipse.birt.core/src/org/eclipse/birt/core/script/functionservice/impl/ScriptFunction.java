
/*******************************************************************************
 * Copyright (c) 2004, 2007 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.core.script.functionservice.impl;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunction;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionArgument;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionCategory;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionExecutor;

/**
 * This class is an implementation of IScriptFuction interface.
 */

public class ScriptFunction implements IScriptFunction
{
	//
	private String name;
	private IScriptFunctionCategory category;
	private String dataType;
	private String desc;
	private IScriptFunctionExecutor executor;
	private IScriptFunctionArgument[] argument;
	private boolean allowVarArguments;
	
	/**
	 * Constructor.
	 * 
	 * @param name
	 * @param category
	 * @param argument
	 * @param dataType
	 * @param desc
	 * @param executor
	 */
	ScriptFunction( String name, IScriptFunctionCategory category, IScriptFunctionArgument[] argument, String dataType, String desc, IScriptFunctionExecutor executor, boolean allowVarArguments )
	{
		this.name = name;
		this.category = category;
		this.argument = argument;
		this.dataType = dataType;
		this.desc = desc;
		this.executor = executor;
		this.allowVarArguments = allowVarArguments;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.core.script.functionservice.IScriptFunction#getArguments()
	 */
	public IScriptFunctionArgument[] getArguments( )
	{
		return this.argument;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.core.script.functionservice.IScriptFunction#getCategory()
	 */
	public IScriptFunctionCategory getCategory( )
	{
		return this.category;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.core.script.functionservice.IScriptFunction#getDataType()
	 */
	public String getDataTypeName( )
	{
		return this.dataType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.core.script.functionservice.INamedObject#getName()
	 */
	public String getName( )
	{
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.core.script.functionservice.IDescribable#getDescription()
	 */
	public String getDescription( )
	{
		return this.desc;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.core.script.functionservice.IScriptFunctionExecutor#execute(java.lang.Object[])
	 */
	public Object execute( Object[] arguments ) throws BirtException
	{
		if( this.executor!= null )
			return this.executor.execute( arguments );
		return null;
	}

	public boolean allowVarArguments( )
	{
		return this.allowVarArguments;
	}

}
