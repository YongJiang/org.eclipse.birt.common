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

import org.eclipse.birt.core.framework.FrameworkException;
import org.eclipse.birt.core.framework.IConfigurationElement;
import org.eclipse.birt.core.framework.IExtension;
import org.eclipse.core.runtime.CoreException;


/**
 * 
 * @version $Revision: #1 $ $Date: 2005/02/01 $
 */
class EclipseConfigurationElement implements IConfigurationElement
{
	org.eclipse.core.runtime.IConfigurationElement object;
	EclipseConfigurationElement(org.eclipse.core.runtime.IConfigurationElement object)
	{
		this.object = object;
	}
	public Object createExecutableExtension(String propertyName) throws FrameworkException
	{
		try
		{
			return object.createExecutableExtension(propertyName);
		}
		catch(CoreException ex)
		{
			throw new FrameworkException(ex);
		}
	}

	public String getAttribute(String name)
	{
		return object.getAttribute(name);
	}

	public String getAttributeAsIs(String name)
	{
		return object.getAttributeAsIs(name);
	}

	public String[] getAttributeNames()
	{
		return object.getAttributeNames();
	}

	public IConfigurationElement[] getChildren()
	{
		return EclipsePlatform.wrap(object.getChildren());
	}

	public IConfigurationElement[] getChildren(String name)
	{
		return EclipsePlatform.wrap(object.getChildren(name));
	}

	public IExtension getDeclaringExtension()
	{
		return EclipsePlatform.wrap(object.getDeclaringExtension());
	}

	public String getName()
	{
		return object.getName();
	}
	
	public Object getParent()
	{
		return EclipsePlatform.wrap(object.getParent());
	}

	public String getValue()
	{
		return object.getValue();
	}

	public String getValueAsIs()
	{
		return object.getValueAsIs();
	}

}
