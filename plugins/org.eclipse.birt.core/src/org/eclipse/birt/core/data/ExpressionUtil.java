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

package org.eclipse.birt.core.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.JavascriptEvalUtil;

/**
 * This class help to manipulate expressions.
 * 
 */
public final class ExpressionUtil
{

	/** prefix for row */
	private static final String ROW_INDICATOR = "row";

	/** prefix for dataset row */
	private static final String DATASET_ROW_INDICATOR = "dataSetRow";
	
	/** prefix for parameter */
	public static final String PARAMETER_INDICATOR = "params";//$NON-NLS-1$

	private static String PREFIX = "COLUMN_";
	
	private static int suffix = 0;

	/**
	 * Return a row expression text according to given row name.
	 * 
	 * @param rowName
	 * @return
	 */
	public static String createRowExpression( String rowName )
	{
		return ROW_INDICATOR
				+ "[\"" + ( rowName == null ? "" : rowName.trim( ) ) + "\"]";
	}

	/**
	 * Return a dataSetRow expression text according to given row name.
	 * 
	 * @param rowName
	 * @return
	 */
	public static String createDataSetRowExpression( String rowName )
	{
		return DATASET_ROW_INDICATOR
				+ "[\"" + ( rowName == null ? "" : rowName.trim( ) ) + "\"]";
	}
	
	/**
	 * Return a JS row expression text according to given row name.
	 * 
	 * @param rowName
	 * @return
	 */
	public static String createJSRowExpression( String rowName )
	{
		return ROW_INDICATOR
				+ "[\""
				+ ( rowName == null
						? ""
						: JavascriptEvalUtil.transformToJsConstants( rowName.trim( ) ) )
				+ "\"]";
	}

	/**
	 * Return a JS dataSetRow expression text according to given row name.
	 * 
	 * @param rowName
	 * @return
	 */
	public static String createJSDataSetRowExpression( String rowName )
	{
		return DATASET_ROW_INDICATOR
				+ "[\""
				+ ( rowName == null
						? ""
						: JavascriptEvalUtil.transformToJsConstants( rowName.trim( ) ) )
				+ "\"]";
	}
	
	/**
	 * Return a JS parameter expression text according to given row name.
	 * 
	 * @param rowName
	 * @return
	 */
	public static String createJSParameterExpression( String parameterName )
	{
		return PARAMETER_INDICATOR
				+ "[\""
				+ ( parameterName == null
						? ""
						: JavascriptEvalUtil.transformToJsConstants( parameterName.trim( ) ) )
				+ "\"]";
	}

	/**
	 * Return a row expression text according to given row index, which is
	 * 1-based.
	 * 
	 * @param index
	 * @return
	 * @deprecated
	 */
	public static String createRowExpression( int index )
	{
		return ROW_INDICATOR + "[" + index + "]";
	}

	/**
	 * Extract all column expression info
	 * 
	 * @param oldExpression
	 * @return
	 * @throws BirtException
	 */
	public static List extractColumnExpressions( String oldExpression )
			throws BirtException
	{
		return extractColumnExpressions( oldExpression, true );
	}
	
	/**
	 * Extract all column expression info
	 * 
	 * @param oldExpression
	 * @param mode
	 *            if true, it means to compile the "row" expression.else extract
	 *            "dataSetRow" expression
	 * @return
	 * @throws BirtException
	 */
	public static List extractColumnExpressions( String oldExpression,
			boolean mode ) throws BirtException
	{
		if ( oldExpression == null || oldExpression.trim( ).length( ) == 0 )
			return new ArrayList( );

		return ExpressionParserUtility.compileColumnExpression( oldExpression,
				mode );
	}

	/**
     * whethter the expression has aggregation 
	 * @param oldExpression
	 * @return
	 * @throws BirtException
	 */
	public static boolean hasAggregation( String expression )
	{
		if ( expression == null )
			return false;

		try
		{
			return ExpressionParserUtility.hasAggregation( expression, true );
		}
		catch ( BirtException e )
		{
			return false;
		}
	}
	
	/**
	 * Return an IColumnBinding instance according to given oldExpression.
	 * 
	 * @param oldExpression
	 * @return
	 */
	public static IColumnBinding getColumnBinding( String oldExpression )
	{
		suffix++;
		return new ColumnBinding( PREFIX + suffix,
				ExpressionUtil.toNewExpression( oldExpression ) );
	}

	/**
	 * Translate the old expression with "row" as indicator to new expression
	 * using "dataSetRow" as indicator.
	 * 
	 * @param oldExpression
	 * @return
	 */
	public static String toNewExpression( String oldExpression )
	{
		if ( oldExpression == null )
			return null;

		char[] chars = oldExpression.toCharArray( );

		// 5 is the minium length of expression that can cantain a row
		// expression
		if ( chars.length < 5 )
			return oldExpression;
		else
		{
			ParseIndicator status = new ParseIndicator( 0,
					0,
					false,
					false,
					true,
					true );

			for ( int i = 0; i < chars.length; i++ )
			{
				status = getParseIndicator( chars,
						i,
						status.omitNextQuote( ),
						status.getCandidateKey1( ),
						status.getCandidateKey2( ) );

				i = status.getNewIndex( );
				if ( i >= status.getRetrieveSize( ) + 3 )
				{
					if ( status.isCandidateKey( )
							&& chars[i - status.getRetrieveSize( ) - 3] == 'r'
							&& chars[i - status.getRetrieveSize( ) - 2] == 'o'
							&& chars[i - status.getRetrieveSize( ) - 1] == 'w' )
					{
						if ( i - status.getRetrieveSize( ) - 4 <= 0
								|| isValidProceeding( chars[i
										- status.getRetrieveSize( ) - 4] ) )
						{
							if ( chars[i] == ' '
									|| chars[i] == '.' || chars[i] == '[' )
							{
								String firstPart = oldExpression.substring( 0,
										i - status.getRetrieveSize( ) - 3 );
								String secondPart = toNewExpression( oldExpression.substring( i
										- status.getRetrieveSize( ) ) );
								String newExpression = firstPart
										+ "dataSetRow" + secondPart;
								return newExpression;
							}
						}
					}
				}
			}

		}

		return oldExpression;
	}

	/**
	 * Translate the old expression with "rows" as parent query indicator to new expression
	 * using "row._outer" as parent query indicator.
	 * 
	 * @param oldExpression
	 * @param isParameterBinding
	 * @return
	 */
	public static String updateParentQueryReferenceExpression( String oldExpression, boolean isParameterBinding )
	{
		if ( oldExpression == null )
			return null;

		char[] chars = oldExpression.toCharArray( );

		// 7 is the minium length of expression that can cantain a row
		// expression
		if ( chars.length < 7 )
			return oldExpression;
		else
		{
			ParseIndicator status = new ParseIndicator( 0,
					0,
					false,
					false,
					true,
					true );

			for ( int i = 0; i < chars.length; i++ )
			{
				status = getParseIndicator( chars,
						i,
						status.omitNextQuote( ),
						status.getCandidateKey1( ),
						status.getCandidateKey2( ) );

				i = status.getNewIndex( );
				if ( i >= status.getRetrieveSize( ) + 4 )
				{
					if ( status.isCandidateKey( )
							&& chars[i - status.getRetrieveSize( ) - 4] == 'r'
							&& chars[i - status.getRetrieveSize( ) - 3] == 'o'
							&& chars[i - status.getRetrieveSize( ) - 2] == 'w'
							&& chars[i - status.getRetrieveSize( ) - 1] == 's')
					{
						if ( i - status.getRetrieveSize( ) - 5 <= 0
								|| isValidProceeding( chars[i
										- status.getRetrieveSize( ) - 5] ) )
						{
							if ( chars[i] == ' '
									|| chars[i] == '.' || chars[i] == '[' )
							{
								int start = i;
								int end = 1;
								//end is the offset of "[n]" in "rows[n]".
								do
								{
									i++;
									end++;
								}while( i < chars.length && chars[i]!=']');
								
								String firstPart = oldExpression.substring( 0,
										start - status.getRetrieveSize( ) - 4 );
								String secondPart = updateParentQueryReferenceExpression( oldExpression.substring( start
										- status.getRetrieveSize( ) + end), isParameterBinding );
								String newExpression = firstPart
										+ (isParameterBinding?"row":"row._outer") + secondPart;
								return newExpression;
							}
						}
					}
				}
			}

		}

		return oldExpression;
	}
	
	/**
	 * whether the exression is report paramter reference.The pattern should
	 * like params["aa"].if yes, return true. else return false;
	 * 
	 * @param expression
	 */
	public static boolean isScalarParamReference( String expression )
	{
		final String PARAM_PATTERN = "params\\[\".+\\\"]";
		Pattern pattern = Pattern.compile( PARAM_PATTERN );
		Matcher matcher = pattern.matcher( expression );
		return matcher.matches( );
	}
	
	/**
	 * This method is used to provide information necessary for next step parsing.
	 * 
	 * @param chars
	 * @param i
	 * @param omitNextQuote
	 * @param candidateKey1
	 * @param candidateKey2
	 * @return
	 */
	private static ParseIndicator getParseIndicator( char[] chars, int i,
			boolean omitNextQuote, boolean candidateKey1, boolean candidateKey2 )
	{
		int retrieveSize = 0;

		if ( chars[i] == '/' )
		{
			if ( i > 0 && chars[i - 1] == '/' )
			{
				retrieveSize++;
				while ( i < chars.length - 2 )
				{
					i++;
					retrieveSize++;
					if ( chars[i] == '\n' )
					{
						break;
					}
				}
				retrieveSize++;
				i++;
			}
		}
		else if ( chars[i] == '*' )
		{
			if ( i > 0 && chars[i - 1] == '/' )
			{
				i++;
				retrieveSize = retrieveSize + 2;
				while ( i < chars.length - 2 )
				{
					i++;
					retrieveSize++;
					if ( chars[i - 1] == '*' && chars[i] == '/' )
					{
						break;
					}
				}
				retrieveSize++;
				i++;
			}
		}

		if ( ( !omitNextQuote ) && chars[i] == '"' )
		{
			candidateKey1 = !candidateKey1;
			if ( candidateKey1 )
				candidateKey2 = true;
		}
		if ( ( !omitNextQuote ) && chars[i] == '\'' )
		{
			candidateKey2 = !candidateKey2;
			if ( candidateKey2 )
				candidateKey1 = true;
		}
		if ( chars[i] == '\\' )
			omitNextQuote = true;
		else
			omitNextQuote = false;

		return new ParseIndicator( retrieveSize,
				i,
				candidateKey1,
				omitNextQuote,
				candidateKey1,
				candidateKey2 );
	}

	/**
	 * Test whether the char immediately before the candidate "row" key is
	 * valid.
	 * 
	 * @param operator
	 * @return
	 */
	private static boolean isValidProceeding( char operator )
	{
		if ( ( operator >= 'A' && operator <= 'Z' )
				|| ( operator >= 'a' && operator <= 'z' ) || operator == '_' )
			return false;

		return true;
	}
	
	/**
	 * 
	 * @param jointColumName
	 * @return
	 */
	public static String[] getSourceDataSetNames( String jointColumName )
	{
		assert jointColumName != null;

		String[] result = new String[2];
		if ( jointColumName.indexOf( "::" ) != -1 )
		{
			String[] splited = jointColumName.split( "::" );

			result[0] = splited[0];
			if ( result[0].endsWith( "1" ) || result[0].endsWith( "2" ) )
				result[1] = result[0].substring( 0, result[0].length( ) - 1 );
		}

		return result;
	}
	
	/**
	 * Extracts the name from the given qualified data set name. A qualified 
	 * data set name is a name with library namespace. 
	 * 
	 * <p>
	 * For example,
	 * <ul>
	 * <li>"dataSet1" is extracted from "LibA.dataSet1"
	 * <li>"dataSet1" is returned from "dataSet1"
	 * </ul>
	 * 
	 * @param qualifiedName
	 *            the qualified reference value
	 * @return the name
	 */
	
	public static String getDataSetName(String qualifiedName)
	{
		if ( qualifiedName == null || qualifiedName.length( ) == 0)
			return null;

		String temp[] = qualifiedName.split( "\\Q.\\E" );  //$NON-NLS-1$
		if( temp.length >= 2 )
			return temp[1].trim( );
		
		return  qualifiedName.trim( );
	}
}

/**
 * A utility class for internal use only.
 * 
 */
class ParseIndicator
{

	private int retrieveSize;
	private int newIndex;
	private boolean isCandidateKey;
	private boolean omitNextQuote;
	private boolean candidateKey1;
	private boolean candidateKey2;

	ParseIndicator( int retrieveSize, int newIndex, boolean isCandidateKey,
			boolean omitNextQuote, boolean candidateKey1, boolean candidateKey2 )
	{
		this.retrieveSize = retrieveSize;
		this.newIndex = newIndex;
		this.isCandidateKey = isCandidateKey;
		this.omitNextQuote = omitNextQuote;
		this.candidateKey1 = candidateKey1;
		this.candidateKey2 = candidateKey2;
	}

	public int getRetrieveSize( )
	{
		return this.retrieveSize;
	}

	public int getNewIndex( )
	{
		return this.newIndex;
	}

	public boolean isCandidateKey( )
	{
		return this.isCandidateKey;
	}

	public boolean omitNextQuote( )
	{
		return this.omitNextQuote;
	}

	public boolean getCandidateKey1( )
	{
		return this.candidateKey1;
	}

	public boolean getCandidateKey2( )
	{
		return this.candidateKey2;
	}
}

class ColumnBinding implements IColumnBinding
{

	private String columnName;
	private String expression;
	private int level;

	ColumnBinding( String columnName, String expression )
	{
		this.columnName = columnName;
		this.expression = expression;
		this.level = 0;
	}
	
	ColumnBinding( String columnName, String expression, int level )
	{
		this.columnName = columnName;
		this.expression = expression;
		this.level = level;
	}
	
	public String getResultSetColumnName( )
	{
		return this.columnName;
	}

	public String getBoundExpression( )
	{
		return this.expression;
	}

	public int getOuterLevel( )
	{
		return level;
	}

}