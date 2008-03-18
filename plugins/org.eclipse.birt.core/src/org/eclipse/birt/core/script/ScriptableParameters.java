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

package org.eclipse.birt.core.script;

import java.util.Map;

import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

public class ScriptableParameters extends BaseScriptable
{

	private Map parameters;
	private final String JS_CLASS_NAME = "ScriptableParameters";

	public ScriptableParameters( Map parameters, Scriptable scope )
	{
		setParentScope( scope );
		this.parameters = parameters;
	}

	public Object get( String name, Scriptable start )
	{
		Object result = getScriptableParameter( name );
		if ( result == null )
		{
			String errorMessage = "Report parameter \"" + name + "\" does not exist.";
			throw new JavaScriptException( errorMessage, "<unknown>", -1 );
		}
		return result;
	}

	private ScriptableParameter getScriptableParameter( String name )
	{
		if ( parameters.containsKey( name ) )
		{
			return new ScriptableParameter( parameters, name, getParentScope( ) );
		}
		return null;
	}

	public boolean has( String name, Scriptable start )
	{
		return parameters.get( name ) != null;
	}

	/**
	 * Support setting parameter value by following methods:
	 * <li> params["a"] = new ParameterAttribute( "value", "displayText");
	 * <li> params["a"] = params["b"]
	 * <li> params["a"] = "value"
	 */
	public void put( String name, Scriptable start, Object value )
	{
		ParameterAttribute entry = (ParameterAttribute) parameters
				.get( name );
		if ( entry == null )
		{
			entry = new ParameterAttribute( );
			parameters.put( name, entry );
		}

		if ( value instanceof ScriptableParameter )
		{
			ScriptableParameter scriptableParameter = (ScriptableParameter) value;
			Object paramValue = scriptableParameter.get( "value", this );
			String displayText = (String) scriptableParameter.get(
					"displayText", this );
			entry.setValue( paramValue );
			entry.setDisplayText( displayText );
			return;
		}
		
		if ( value instanceof Wrapper )
		{
			value = ( (Wrapper) value ).unwrap( );
		}

		if ( value instanceof ParameterAttribute )
		{
			ParameterAttribute param = (ParameterAttribute) value;
			entry.setValue( param.getValue( ) );
			entry.setDisplayText( param.getDisplayText( ) );
		}
		else
		{
			entry.setValue( value );
		}
	}

	public String getClassName( )
	{
		return JS_CLASS_NAME;
	}
}
