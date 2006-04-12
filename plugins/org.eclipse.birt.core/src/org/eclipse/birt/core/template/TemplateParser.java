/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: Actuate Corporation -
 * initial API and implementation
 ******************************************************************************/

package org.eclipse.birt.core.template;

import java.io.StringReader;

public class TemplateParser
{

	public TextTemplate parse( String text )
	{

		if ( text == null || text.trim( ).equals( "" ) )
		{
			return null;
		}
		try
		{
			ASTTemplate templateNode = new Parser( ).parse( new StringReader(
					text ) );
			TextTemplate template = new TextTemplate( );
			new ASTVisitor( template ).visit( templateNode, null );
			return template;
		}
		catch ( Exception ex )
		{
			ex.printStackTrace( );
		}
		return null;
	}

	protected class ASTVisitor implements ParserVisitor
	{

		TextTemplate template;

		ASTVisitor( TextTemplate template )
		{
			this.template = template;
		}

		public Object visit( SimpleNode node, Object data )
		{
			return data;
		}

		public Object visit( ASTAttribute node, Object data )
		{
			if ( data instanceof TextTemplate.ImageNode )
			{
				TextTemplate.ImageNode image = (TextTemplate.ImageNode) data;
				image.setAttribute( node.getName( ), getAttributeValue( node
						.getValue( ) ) );
			}
			else if ( data instanceof TextTemplate.ValueNode )
			{
				if ( "format".equalsIgnoreCase( node.getName( ) ) )
				{
					TextTemplate.ValueNode value = (TextTemplate.ValueNode) data;
					value.format = getAttributeValue( node.getValue( ) );
				}
			}
			else if ( data instanceof TextTemplate.TextNode )
			{
				StringBuffer attribute = new StringBuffer( );
				attribute.append( " " + node.getName( ) + ":" + node.getValue( )
						+ " " );
				return attribute.toString( );
			}
			return data;
		}

		public Object visit( ASTValueOf node, Object data )
		{
			TextTemplate.ValueNode value = new TextTemplate.ValueNode( );
			node.childrenAccept( this, value );
			template.nodes.add( value );
			return data;
		}

		public Object visit( ASTImage node, Object data )
		{
			TextTemplate.ImageNode image = new TextTemplate.ImageNode( );
			node.childrenAccept( this, image );
			template.nodes.add( image );
			return data;
		}

		public Object visit( ASTText node, Object data )
		{
			TextTemplate.TextNode text = new TextTemplate.TextNode( );
			text.content = node.getContent( );
			template.nodes.add( text );
			return data;
		}

		public Object visit( ASTTemplate node, Object data )
		{
			return node.childrenAccept( this, data );
		}

		public Object visit( ASTEbody_content node, Object data )
		{
			if ( data instanceof TextTemplate.ImageNode )
			{
				TextTemplate.ImageNode image = (TextTemplate.ImageNode) data;
				String expr = getText( node );
				if ( expr != null && expr.length( ) > 0 )
				{
					image.setExpr( expr );
				}
			}
			else if ( data instanceof TextTemplate.ValueNode )
			{
				TextTemplate.ValueNode value = (TextTemplate.ValueNode) data;
				value.value = getText( node );
			}
			else
			{
				return getText( node );
			}
			return data;
		}

		protected String getText( ASTEbody_content node )
		{
			if ( node == null || node.children == null )
			{
				return null;
			}
			Object obj;
			StringBuffer text = new StringBuffer( );
			for ( int n = 0; n < node.children.length; n++ )
			{
				obj = node.children[n];
				if ( obj instanceof ASTText )
				{
					text.append( ( (ASTText) obj ).getContent( ) );
				}

			}
			return text.toString( );
		}

		private String getAttributeValue( String value )
		{
			if ( value != null )
			{
				int length = value.length( );
				if ( length > 2 )
				{
					// remove the first and last quote
					value = value.substring( 1, length - 1 );
				}
			}
			return value;
		}

	}

}
