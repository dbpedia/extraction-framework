package org.dbpedia.extraction.live.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;


//http://stackoverflow.com/questions/1497569/how-to-execute-sql-script-file-using-jdbc
public class SQLUtil
{
	private static final Logger logger = Logger.getLogger(SQLUtil.class);

	public static String placeHolder(int n, int m)
	{
		String part = "";
		for(int i = m - 1; i >= 0; --i) {
			part += "?";

			if(i != 0)
				part += ", ";
		}

		if(m > 1)
			part = "(" + part + ")";

		String result = "";
		for(int j = n - 1; j >= 0; --j) {
			result += part;

			if(j != 0)
				result += ", ";
		}

		return result;
	}

	private static Pattern singleLineCommentPattern = Pattern.compile("^--.*$", Pattern.MULTILINE);

	private static Pattern functionPattern = Pattern.compile("CREATE\\s+FUNCTION.*\\sAS\\s+(.*)\\s", Pattern.CASE_INSENSITIVE);
	/**
	 *
	 *
	 */
	public static List<String> parseSQLScript(String data)
	{
		//data = data.replaceAll("/\\*.*\\*<remove>/", "");
		data = data.replaceAll("/\\*.*\\*/", "");

		Matcher matcher = singleLineCommentPattern.matcher(data);
		data = matcher.replaceAll("");

		String[] parts = data.split(";");

		Stack<String> stack = new Stack<String>();

		List<String> result = new ArrayList<String>();
		String current = "";
		for(String part : parts) {
			part = part.trim();

			// While the stack is not empty, check if the part contains
			// matching tokens
			// This routine does not consider the order of the tokens
			while(!stack.isEmpty()) {
				String tos = stack.peek();
				if(part.contains(tos)) {
					stack.pop();
				}
				else {
					break;
				}
			}

			if(!current.isEmpty()) {
				current += ";";
			}

			if(part.isEmpty())
				continue;

			current += part;

			Matcher m = functionPattern.matcher(part);
			if(m.find()) {
				String delim = m.group(1);
				stack.push(delim);
			}

			if(stack.isEmpty()) {
				result.add(current);
				current = "";
			}
		}

		return result;
	}

	/*
	public static String multiImplode(Collection<? extends Collection<?>> rows)
	{
		MultiRowString result = new MultiRowString();

		for(Collection<?> row : rows) {
			result.nextRow();

			for(Object o : row) {
				result.add(o);
			}

			result.endRow();
		}

		return result.toString();
	}


	public static String implodeEscaped(String separator, Collection<Object> items)
	{
		return StringUtil.implode(
				separator,
				new TransformIterator<Object, String>(
						items.iterator(),
						SQLEscapeTransformer.getInstance()));
	}
	*/

	public static <T> T single(ResultSet rs, Class<T> clazz)
		throws SQLException
	{
		return single(rs, clazz, true);
	}


	/**
	 * Returns the 1st column of the first row or null of there is no row.
	 * Also throws exception if there is more than 1 row and 1 column.
	 *
	 * @param
	 * @param
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T single(ResultSet rs, Class<T> clazz, boolean bClose)
		throws SQLException
	{
		if(rs.getMetaData().getColumnCount() != 1)
			throw new RuntimeException("only a single column expected");

		T result = null;

		if(rs.next()) {
			Object o = rs.getObject(1);;
			//System.out.println("Result = " + o);
			result = (T)o;

			if(rs.next())
				throw new RuntimeException("only at most 1 row expected");
		}

		if(bClose)
			rs.close();

		return result;
	}


	public static <T> List<T> list(ResultSet rs, Class<T> clazz)
		throws SQLException
	{
		return list(rs, clazz, true);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> list(ResultSet rs, Class<T> clazz, boolean bClose)
		throws SQLException
	{
		List<T> result = new ArrayList<T>();

		try {
			while(rs.next()) {
				Object o = rs.getObject(1);
				//System.out.println("Result = " + o);
				T item = (T)o;
				result.add(item);
			}
		}
		finally {
			if(bClose)
				rs.close();
		}

		return result;

	}


	public static <T> void executeSetArgs(PreparedStatement stmt, Object ...args)
		throws SQLException
	{
		for(int i = 0; i < args.length; ++i) {
			stmt.setObject(i + 1, args[i]);
		}

		// Pad with nulls
		int n = stmt.getParameterMetaData().getParameterCount();
		//System.out.println("x = " + n);
		for(int i = args.length; i < n; ++i) {
			stmt.setObject(i + 1, null);
		}
		//System.out.println("y = " + n);
	}

	/* Closing the statements also closes the resultset...
	public static ResultSet execute(Connection conn, String sql)
		throws SQLException
	{
		ResultSet result = null;
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			result = stmt.executeQuery(sql);
		}
		finally {
			if(stmt != null) {
				stmt.close();
			}
		}

		return result;
	}
	*/


	public static <T> T execute(Connection conn, String sql, Class<T> clazz, Object ...args)
		throws SQLException
	{
		PreparedStatement stmt = conn.prepareStatement(sql);

		T result = execute(stmt, clazz, args);

		stmt.close();

		return result;
	}

	public static ResultSet executeCore(Connection conn, String sql, Object ...args)
		throws SQLException
	{
		logger.trace("Executing statement '" + sql + "' with args " + Arrays.asList(args));

		PreparedStatement stmt = conn.prepareStatement(sql);

		executeSetArgs(stmt, args);
		ResultSet result = stmt.executeQuery();

		//stmt.close();

		return result;
	}

	public static ResultSet execute(PreparedStatement stmt, Object ...args)
		throws SQLException
	{
		executeSetArgs(stmt, args);

		ResultSet result = stmt.executeQuery();
		return result;
	}

	public static <T> T execute(PreparedStatement stmt, Class<T> clazz, Object ...args)
		throws SQLException
	{
		executeSetArgs(stmt, args);

		T result = null;
		if(clazz == null || Void.class.equals(clazz)) {
			stmt.execute();
		}
		else {
			ResultSet rs = stmt.executeQuery();
			result = SQLUtil.single(rs, clazz);
			rs.close();
		}

		return result;
	}

	public static <T> List<T> executeList(Connection conn, String sql, Class<T> clazz, Object ...args)
		throws SQLException
	{
		logger.trace("Executing statement '" + sql + "' with args " + Arrays.asList(args));

		PreparedStatement stmt = conn.prepareStatement(sql);

		List<T> result = executeList(stmt, clazz, args);

		stmt.close();

		return result;
	}

	public static <T> List<T> executeList(PreparedStatement stmt, Class<T> clazz, Object ...args)
		throws SQLException
	{
		executeSetArgs(stmt, args);

		ResultSet rs = stmt.executeQuery();
		List<T> result = SQLUtil.list(rs, clazz);

		return result;
	}

	public static void close(ResultSet resultSet)
	{
		try {
			if(resultSet != null)
				resultSet.close();
		}
		catch(SQLException e) {
			logger.error(ExceptionUtils.getFullStackTrace(e));
		}
	}

	/*
	public static String escape(Object value)
	{
		String v;
		if(value == null) {
			v = "NULL";
		}
		else {
			if(value instanceof String)
				v = "'" + StringEscapeUtils.escapeSql(value.toString()) + "'";
			else
				v = value.toString();
		}

		return v;
	}
	*/

	public static String escapePostgres(Object o)
	{
		return o.toString().replace("\\", "\\\\").replace("'", "\\'");
	}

	public static String quotePostgres(Object o)
	{
		if(o == null)
			return null;

		return "E'" + escapePostgres(o) + "'";
	}

	public static List<String> quotePostgres(Iterable<?> items)
	{
		List<String> result = new ArrayList<String>();
		for(Object item : items)
			result.add(quotePostgres(item));

		return result;
	}
}
