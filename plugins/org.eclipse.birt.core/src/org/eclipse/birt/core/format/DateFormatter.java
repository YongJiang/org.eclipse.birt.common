/*******************************************************************************
* Copyright (c) 2004,2007 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.core.format;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * DateFormatter.
 * 
 * Design for Class DateFormatter.This version is for open source, so we only
 * apply the function which Java has provided. Beside these basic function, in
 * this version, we also provide some new API for further implementation in the
 * future
 * 
 */
public class DateFormatter
{

	private static final String UNFORMATTED = "Unformatted";
	public static final String DATETIME_UNFORMATTED = "DateTime" + UNFORMATTED;
	public static final String DATE_UNFORMATTED = "Date" + UNFORMATTED;
	public static final String TIME_UNFORMATTED = "Time" + UNFORMATTED;

	/**
	 * Comment for <code>formatPattern</code> record the string pattern
	 */
	private String formatPattern;

	/**
	 * Comment for <code>dateTimeFormat</code> used for two methods,
	 * createDateFormat() and format()
	 */
	private com.ibm.icu.text.DateFormat dateTimeFormat;

	private com.ibm.icu.text.DateFormat timeFormat;

	private com.ibm.icu.text.DateFormat dateFormat;
	/**
	 * Comment for <code>locale</code> used for record Locale information
	 */
	private ULocale locale = ULocale.getDefault( );
	
	private TimeZone timeZone = null;

	/**
	 * logger used to log syntax errors.
	 */
	static protected Logger logger = Logger.getLogger( DateFormatter.class.getName( ) );

	/**
	 * constuctor method with no paremeter
	 */
	public DateFormatter( )
	{
		this( null, null, null );
	}
	
	public DateFormatter( TimeZone timeZone )
	{
		this( null, null, timeZone );
	}

	/**
	 * constuctor method with String parameter
	 * 
	 * @param pattern
	 */
	public DateFormatter( String pattern )
	{
		this( pattern, null, null );
	}

	/**
	 * constuctor method with Locale parameters
	 * 
	 * @param localeLoc
	 */
	public DateFormatter( ULocale localeLoc )
	{
		this( null, localeLoc, null );
	}
	
	public DateFormatter( ULocale localeLoc, TimeZone timeZone )
	{
		this( null, localeLoc, timeZone );
	}

	/**
	 * @deprecated since 2.1
	 * @return
	 */
	public DateFormatter( Locale localeLoc )
	{
		this( null, ULocale.forLocale( localeLoc ), null );
	}

	/**
	 * constuctor method with two parameters, one is String type while the other
	 * is Locale type
	 * 
	 * @param pattern
	 * @param localeLoc
	 */
	public DateFormatter( String pattern, ULocale localeLoc )
	{
		this( pattern, localeLoc, null );
	}
	
	public DateFormatter( String pattern, ULocale localeLoc, TimeZone timeZone )
	{
		if ( localeLoc != null )
			locale = localeLoc;
		if ( timeZone != null )
			this.timeZone = timeZone;
		applyPattern( pattern );
	}

	/**
	 * @deprecated since 2.1
	 * @return
	 */
	public DateFormatter( String pattern, Locale localeLoc )
	{
		this( pattern, ULocale.forLocale( localeLoc ) );
	}

	/**
	 * get the string pattern
	 * 
	 * @return
	 */
	public String getPattern( )
	{
		return this.formatPattern;
	}

	public void applyPattern( String formatString )
	{
		createPattern( formatString );
		applyTimeZone( );
	}
	/**
	 * define pattern and locale here
	 * 
	 * @param formatString
	 */
	private void createPattern( String formatString )
	{
		try
		{
			this.formatPattern = formatString;
			this.dateTimeFormat = null;
			this.dateFormat = null;
			this.timeFormat = null;

			/*
			 * we can seperate these single name-based patterns form those
			 * patterns with multinumber letters
			 */
			if ( formatString == null || UNFORMATTED.equals( formatString ) )
			{
				formatPattern = UNFORMATTED;
				dateTimeFormat = com.ibm.icu.text.DateFormat
						.getDateTimeInstance(
								com.ibm.icu.text.DateFormat.MEDIUM,
								com.ibm.icu.text.DateFormat.SHORT, locale );
				dateFormat = com.ibm.icu.text.DateFormat.getDateInstance(
						com.ibm.icu.text.DateFormat.MEDIUM, locale );
				timeFormat = com.ibm.icu.text.DateFormat.getTimeInstance(
						com.ibm.icu.text.DateFormat.MEDIUM, locale );
				return;
			}
			else if ( formatString.equals( DATETIME_UNFORMATTED ) ) //$NON-NLS-1$
			{
				dateTimeFormat = com.ibm.icu.text.DateFormat.getDateTimeInstance( com.ibm.icu.text.DateFormat.MEDIUM,
						com.ibm.icu.text.DateFormat.SHORT,
						locale );
				return;

			}
			else if ( formatString.equals( DATE_UNFORMATTED ) )
			{
				dateTimeFormat = com.ibm.icu.text.DateFormat.getDateInstance( com.ibm.icu.text.DateFormat.MEDIUM,
						locale );
				return;
			}
			else if ( formatString.equals( TIME_UNFORMATTED ) )
			{
				dateTimeFormat = com.ibm.icu.text.DateFormat.getTimeInstance( com.ibm.icu.text.DateFormat.MEDIUM,
						locale );
				return;
			}

			if ( formatString.length( ) == 1 )
			{
				char patternTemp = formatString.charAt( 0 );
				switch ( patternTemp )
				{
					case 'G' :
						dateTimeFormat = com.ibm.icu.text.DateFormat
								.getDateTimeInstance(
										com.ibm.icu.text.DateFormat.LONG,
										com.ibm.icu.text.DateFormat.LONG,
										locale );
						dateFormat = com.ibm.icu.text.DateFormat
								.getDateInstance(
										com.ibm.icu.text.DateFormat.LONG,
										locale );
						timeFormat = com.ibm.icu.text.DateFormat
								.getTimeInstance(
										com.ibm.icu.text.DateFormat.LONG,
										locale );
						return;
					case 'D' :

						dateTimeFormat = com.ibm.icu.text.DateFormat.getDateInstance( com.ibm.icu.text.DateFormat.LONG,
								locale );
						return;
					case 'd' :

						dateTimeFormat = com.ibm.icu.text.DateFormat.getDateInstance( com.ibm.icu.text.DateFormat.SHORT,
								locale );
						return;
					case 'T' :

						dateTimeFormat = com.ibm.icu.text.DateFormat.getTimeInstance( com.ibm.icu.text.DateFormat.LONG,
								locale );
						return;
					case 't' :
						dateTimeFormat = new SimpleDateFormat( "HH:mm", locale );
						return;
					case 'f' :
						dateTimeFormat = com.ibm.icu.text.DateFormat.getDateTimeInstance( com.ibm.icu.text.DateFormat.LONG,
								com.ibm.icu.text.DateFormat.SHORT,
								locale );
						return;
					case 'F' :
						dateTimeFormat = com.ibm.icu.text.DateFormat.getDateTimeInstance( com.ibm.icu.text.DateFormat.LONG,
								com.ibm.icu.text.DateFormat.LONG,
								locale );
						return;

						// I/i produces a short (all digit) date format with 4-
						// digit years
						// and a medium/long time
						// Unfortunately SHORT date format returned by
						// DateFormat is always 2-digits
						// We will need to create our own SimpleDateFormat based
						// on what the
						// DateTime factory gives us
					case 'a' :
					case 'A' :
					case 'i' :
					case 'I' :
						int timeForm = ( patternTemp == 'i' || patternTemp == 'a' )
								? com.ibm.icu.text.DateFormat.MEDIUM
								: com.ibm.icu.text.DateFormat.LONG;
						timeFormat = com.ibm.icu.text.DateFormat
								.getTimeInstance( timeForm, locale );
						if ( patternTemp == 'a' || patternTemp == 'A' )
						{
							if ( timeFormat instanceof com.ibm.icu.text.SimpleDateFormat )
							{
								com.ibm.icu.text.SimpleDateFormat temp = (com.ibm.icu.text.SimpleDateFormat) timeFormat;
								String oldPattern = temp.toPattern( );
								String newPattern = null;
								
								int ssIndex = oldPattern.indexOf( "ss" );
								newPattern = oldPattern.substring( 0, ssIndex + 2 );
								newPattern += ".SSS";
								newPattern += oldPattern.substring( ssIndex + 2 );
								
								temp.applyPattern( newPattern );
							}
						}

						com.ibm.icu.text.DateFormat factoryFormat = com.ibm.icu.text.DateFormat
								.getDateInstance(
										com.ibm.icu.text.DateFormat.SHORT,
										locale );
						dateFormat = hackYear( factoryFormat );

						factoryFormat = com.ibm.icu.text.DateFormat
								.getDateTimeInstance(
										com.ibm.icu.text.DateFormat.SHORT,
										timeForm, locale );
						dateTimeFormat = hackYear( factoryFormat );
						return;

					case 'g' :

						dateTimeFormat = com.ibm.icu.text.DateFormat.getDateTimeInstance( com.ibm.icu.text.DateFormat.SHORT,
								com.ibm.icu.text.DateFormat.SHORT,
								locale );
						return;
					case 'M' :
					case 'm' :
						dateTimeFormat = new SimpleDateFormat( "MM/dd", locale );
						return;
					case 'R' :
					case 'r' :
						dateTimeFormat = new SimpleDateFormat( "yyyy.MM.dd HH:mm:ss a",
								locale );
						dateTimeFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
						return;
					case 's' :
						dateTimeFormat = new SimpleDateFormat( "yyyy.MM.dd HH:mm:ss",
								locale );
						return;
					case 'u' :
						dateTimeFormat = new SimpleDateFormat( "yyyy.MM.dd HH:mm:ss  Z",
								locale );
						return;
						// TODO:the defination is not clear enough
						/*
						 * case 'U': return;
						 */
					case 'Y' :
					case 'y' :
						dateTimeFormat = new SimpleDateFormat( "yyyy/mm", locale );
						return;
					default :
						dateTimeFormat = new SimpleDateFormat( formatString, locale );
						return;
				}
			}

			/*
			 * including the patterns which Java accepted and those name-based
			 * patterns with multinumber letters
			 */
			if ( formatString.equals( "General Date" ) )
			{
				dateTimeFormat = com.ibm.icu.text.DateFormat.getDateTimeInstance( com.ibm.icu.text.DateFormat.LONG,
						com.ibm.icu.text.DateFormat.LONG,
						locale );
				return;
			}
			if ( formatString.equals( "Long Date" ) )
			{
				dateTimeFormat = com.ibm.icu.text.DateFormat.getDateInstance( com.ibm.icu.text.DateFormat.LONG,
						locale );
				return;

			}
			if ( formatString.equals( "Medium Date" ) )
			{
				dateTimeFormat = com.ibm.icu.text.DateFormat.getDateInstance( com.ibm.icu.text.DateFormat.MEDIUM,
						locale );
				return;

			}
			if ( formatString.equals( "Short Date" ) )
			{
				dateTimeFormat = com.ibm.icu.text.DateFormat.getDateInstance( com.ibm.icu.text.DateFormat.SHORT,
						locale );
				return;

			}
			if ( formatString.equals( "Long Time" ) )
			{
				dateTimeFormat = com.ibm.icu.text.DateFormat.getTimeInstance( com.ibm.icu.text.DateFormat.LONG,
						locale );
				return;

			}
			if ( formatString.equals( "Medium Time" ) )
			{
				dateTimeFormat = com.ibm.icu.text.DateFormat.getTimeInstance( com.ibm.icu.text.DateFormat.MEDIUM,
						locale );
				return;

			}
			if ( formatString.equals( "Short Time" ) )
			{
				dateTimeFormat = new SimpleDateFormat( "kk:mm", locale );
				return;

			}
			dateTimeFormat = new SimpleDateFormat( formatString, locale );

		}
		catch ( Exception e )
		{
			logger.log( Level.WARNING, e.getMessage( ), e );
		}
	}

	private void applyTimeZone( )
	{
		if ( this.timeZone != null )
		{
			if ( this.dateFormat != null )
			{
				this.dateFormat.setTimeZone( timeZone );
			}
			if ( this.dateTimeFormat != null )
			{
				this.dateTimeFormat.setTimeZone( timeZone );
			}
			if ( this.timeFormat != null )
			{
				this.timeFormat.setTimeZone( timeZone );
			}
		}
	}
	
	private com.ibm.icu.text.DateFormat hackYear(
			com.ibm.icu.text.DateFormat factoryFormat )
	{
		// Try cast this to SimpleDateFormat - DateFormat
		// JavaDoc says this should
		// succeed in most cases
		if ( factoryFormat instanceof SimpleDateFormat )
		{
			SimpleDateFormat factorySimpleFormat = (SimpleDateFormat) factoryFormat;

			String pattern = factorySimpleFormat.toPattern( );
			// Search for 'yy', then add a 'y' to make the year 4
			// digits
			if ( pattern.indexOf( "yyyy" ) == -1 )
			{
				int idx = pattern.indexOf( "yy" );
				if ( idx >= 0 )
				{
					StringBuffer strBuf = new StringBuffer( pattern );
					strBuf.insert( idx, 'y' );
					pattern = strBuf.toString( );
				}
			}
			return new SimpleDateFormat( pattern, locale );
		}
		return factoryFormat;
	}
	/*
	 * transfer the format string pattern from msdn to the string pattern which
	 * java can recognize
	 */
	public String format( Date date )
	{
		try
		{
			if ( date instanceof java.sql.Date )
			{
				if ( dateFormat != null )
				{
					return dateFormat.format( date );
				}
			}
			else if ( date instanceof java.sql.Time )
			{
				if ( timeFormat != null )
				{
					return timeFormat.format( date );
				}
			}
			return dateTimeFormat.format( date );
		}
		catch ( Exception e )
		{
			logger.log( Level.WARNING, e.getMessage( ), e );
			return null;
		}
	}

	/**
	 * Returns format code according to format type and current locale
	 */
	public String getFormatCode( )
	{
		String formatCode = null;
		if ( formatPattern.equals( "General Date" ) )
		{
			SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance( com.ibm.icu.text.DateFormat.LONG,
					com.ibm.icu.text.DateFormat.LONG,
					locale );
			formatCode = dateFormat.toPattern( );
		}
		if ( formatPattern.equals( "Long Date" ) )
		{
			SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance( com.ibm.icu.text.DateFormat.LONG,
					locale );
			formatCode = dateFormat.toPattern( );

		}
		if ( formatPattern.equals( "Medium Date" ) )
		{
			SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance( com.ibm.icu.text.DateFormat.MEDIUM,
					locale );
			formatCode = dateFormat.toPattern( );

		}
		if ( formatPattern.equals( "Short Date" ) )
		{
			SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance( com.ibm.icu.text.DateFormat.SHORT,
					locale );
			formatCode = dateFormat.toPattern( );

		}
		if ( formatPattern.equals( "Long Time" ) )
		{
			SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getTimeInstance( com.ibm.icu.text.DateFormat.LONG,
					locale );
			formatCode = dateFormat.toPattern( );

		}
		if ( formatPattern.equals( "Medium Time" ) )
		{
			SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getTimeInstance( com.ibm.icu.text.DateFormat.MEDIUM,
					locale );
			formatCode = dateFormat.toPattern( );
		}
		if ( formatPattern.equals( "Short Time" ) )
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat( "kk:mm", locale );
			formatCode = dateFormat.toPattern( );
		}
		if ( UNFORMATTED.equals( formatPattern ) ||
				DATETIME_UNFORMATTED.equals( formatPattern ) ||
				DATE_UNFORMATTED.equals( formatPattern ) ||
				TIME_UNFORMATTED.equals( formatPattern ) )
		{
			formatCode = "";
		}
			
		return formatCode;
	}

	/**
	 * Parses the input string into a formatted date type.
	 * 
	 * @param date
	 *            the input string to parse
	 * @return the formatted date
	 * @throws ParseException
	 *             if the beginning of the specified string cannot be parsed.
	 */

	public Date parse( String date ) throws ParseException
	{
		return dateTimeFormat.parse( date );
	}
}