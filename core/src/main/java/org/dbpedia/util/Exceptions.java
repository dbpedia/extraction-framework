package org.dbpedia.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Some utility methods for throwing and handling exceptions.
 */
public class Exceptions
{

  /**
   * @param error may be null
   * @return root cause, null if given error is null.
   */
  public static Throwable getRootCause( Throwable error )
  {
    Throwable cause = error;
    while (cause != null)
    {
      error = cause;
      cause = error.getCause();
    }
    return error;
  }

  /**
   * @param <T>
   * @param exception
   * @param cause may be null
   * @return exception given exception, with added cause
   */
  public static <T extends Throwable> T initCause( T exception, Throwable cause )
  {
    // the cast is ok - initCause returns exception itself
    @SuppressWarnings("unchecked")
    T result = (T)exception.initCause(cause);
    
    return result;
  }

  /**
   * Sets the second Throwable as the cause of the first and throws the first Throwable.
   * @param exception any Throwable, will be thrown
   * @param cause the cause of the first parameter. use null to indicate that the cause is nonexistent or unknown.
   * @return nothing, but the compiler thinks it's a RuntimeException
   */
  public static RuntimeException unchecked( Throwable exception, Throwable cause )
  {
    // the <RuntimeException> part lets the compiler think we cast the
    // Throwable to a RuntimeException, which we don't really do
    Exceptions.<RuntimeException>cheat(exception.initCause(cause));
    return null;
  }

  /**
   * <p>Throws the given Throwable, but the compiler doesn't know it.</p>
   * <p>Note: the return type is another cheat that allows this method to be
   * used like this:
   * <pre><code>throw Exceptions.unchecked( error );</code></pre>
   * The <tt>throw</tt> is never reached, because the exception is not
   * <i>returned</i> (which would lead to a ClassCastException) but
   * <i>thrown</i> by this method. The above code has the same effect as this:
   * <pre><code>Exceptions.unchecked( error );</code></pre>
   * The advantage of the explicit <tt>throw</tt> is that the compiler knows
   * that this line unconditionally throws an exception.
   * </p>
   *
   * @param exception any Throwable, will be thrown
   * @return nothing, but the compiler thinks it's a RuntimeException
   */
  public static RuntimeException unchecked( Throwable exception )
  {
    // the <RuntimeException> part lets the compiler think we cast the
    // Throwable to a RuntimeException, which we don't really do
    Exceptions.<RuntimeException>cheat(exception);
    return null; // we never get here
  }

  /**
   * Throws the given Throwable, but the compiler thinks it's first cast to type T.
   * @param <T> the compiler thinks we cast the given Throwable to this type
   * @param throwable any Throwable, will be thrown
   * @throws T not really - throws the given Throwable
   */
  private static <T extends Throwable> void cheat( Throwable throwable ) throws T
  {
    // at runtime, this cast does nothing, because T is erased to Throwable
    @SuppressWarnings("unchecked")
    T result = (T)throwable;
    
    throw result;
  }

  /**
   * Returns all elements of the stack trace of the given exception as a String,
   * as printed by {@link Throwable#printStackTrace(PrintWriter)}.
   * TODO: add argument that lets us cut the boring stuff.
   * @param error
   * @return the stack trace of the given exception
   */
  public static String getStackTrace( Throwable error )
  {
    StringWriter writer = new StringWriter();
    PrintWriter printer = new PrintWriter(writer);
    error.printStackTrace(printer);
    printer.close();  // not really necessary here, but anyway...
    return writer.toString();
  }

  /**
   * Returns at most the given number of elements of the stack trace of the given exception as a String,
   * as printed by {@link #printStackTrace(Throwable, int, PrintWriter)}.
   * TODO: include the stack frames of the cause(s) ?
   * @param error may be null
   * @param depth number of stack trace elements to include
   * @return the stack trace of the given exception
   */
  public static String getStackTrace( Throwable error, int depth )
  {
    return getStackTrace(null, error, depth);
  }

  /**
   * Returns at most the given number of elements of the stack trace of the given exception as a String,
   * as printed by {@link #printStackTrace(Throwable, int, PrintWriter)}.
   * TODO: include the stack frames of the cause(s) ?
   * @param message may be null
   * @param error may be null
   * @param depth number of stack trace elements to include
   * @return the stack trace of the given exception
   */
  public static String getStackTrace( Object message, Throwable error, int depth )
  {
    StringWriter writer = new StringWriter();
    PrintWriter printer = new PrintWriter(writer);
    printStackTrace(message, error, depth, printer);
    printer.close();  // not really necessary here, but anyway...
    return writer.toString();
  }

  /**
   * Print at most the given number of elements of the stack trace of
   * the given exception to the given stream. TODO: include the stack frames of the cause(s) ?
   * @param error may be null
   * @param depth number of stack trace elements to include
   * @param stream must not be null
   */
  public static void printStackTrace( Throwable error, int depth, PrintWriter stream )
  {
    printStackTrace(null, error, depth, stream);
  }

  /**
   * Print the given message and at most the given number of elements of the stack trace of
   * the given exception to the given stream. TODO: include the stack frames of the cause(s) ?
   * <p/>
   * This is an adapted copy of {@link Throwable#printStackTrace(PrintWriter)}.
   * @param message may be null
   * @param error may be null
   * @param depth number of stack trace elements to include
   * @param stream must not be null
   */
  public static void printStackTrace( Object message, Throwable error, int depth, PrintWriter stream )
  {
    synchronized(stream)
    {
      if (message != null) stream.println(message);
      if (error == null) return;
      stream.println(error);
      StackTraceElement[] trace = error.getStackTrace();
      depth = Math.min(depth, trace.length);
      for (int i = 0; i < depth; i++)
      {
        stream.println("\tat " + trace[i]);
      }
      if (trace.length > depth)
      {
        stream.println("\t... " + (trace.length - depth) + " more");
      }
    }
  }

  /**
   * Print at most the given number of elements of the stack trace of
   * the given exception to the given stream. TODO: include the stack frames of the cause(s) ?
   * @param error may be null
   * @param depth number of stack trace elements to include
   * @param stream must not be null
   */
  public static void printStackTrace( Throwable error, int depth, PrintStream stream )
  {
    printStackTrace(null, error, depth, stream);
  }

  /**
   * Print the given message and at most the given number of elements of the stack trace of
   * the given exception to the given stream. TODO: include the stack frames of the cause(s) ?
   * <p/>
   * This is an adapted copy of {@link Throwable#printStackTrace(PrintStream)}.
   * @param message may be null
   * @param error may be null
   * @param depth number of stack trace elements to include
   * @param stream must not be null
   */
  public static void printStackTrace( Object message, Throwable error, int depth, PrintStream stream )
  {
    synchronized(stream)
    {
      if (message != null) stream.println(message);
      if (error == null) return;
      stream.println(error);
      StackTraceElement[] trace = error.getStackTrace();
      depth = Math.min(depth, trace.length);
      for (int i = 0; i < depth; i++)
      {
        stream.println("\tat " + trace[i]);
      }
      if (trace.length > depth)
      {
        stream.println("\t... " + (trace.length - depth) + " more");
      }
    }
  }

}
