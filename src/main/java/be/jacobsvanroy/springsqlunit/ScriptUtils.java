/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.jacobsvanroy.springsqlunit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.CannotReadScriptException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Generic utility methods for working with SQL scripts. Mainly for internal use
 * within the framework.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Dave Syer
 * @author Chris Beams
 * @author Oliver Gierke
 * @author Chris Baldwin
 * @since 4.0.3
 */
public abstract class ScriptUtils {

    private static final Log logger = LogFactory.getLog(ScriptUtils.class);

    /**
     * Default statement separator within SQL scripts.
     */
    public static final String DEFAULT_STATEMENT_SEPARATOR = ";";

    /**
     * Fallback statement separator within SQL scripts.
     * <p>Used if neither a custom defined separator nor the
     * {@link #DEFAULT_STATEMENT_SEPARATOR} is present in a given script.
     */
    public static final String FALLBACK_STATEMENT_SEPARATOR = "\n";

    /**
     * End of file (EOF) SQL statement separator.
     * <p>This value may be supplied as the {@code separator} to {@link
     * #executeSqlScript(javax.sql.DataSource, EncodedResource, boolean, boolean, String, String, String, String)}
     * to denote that an SQL script contains a single statement (potentially
     * spanning multiple lines) with no explicit statement separator. Note that
     * such a script should not actually contain this value; it is merely a
     * <em>virtual</em> statement separator.
     */
    public static final String EOF_STATEMENT_SEPARATOR = "^^^ END OF SCRIPT ^^^";

    /**
     * Default prefix for line comments within SQL scripts.
     */
    public static final String DEFAULT_COMMENT_PREFIX = "--";

    /**
     * Default start delimiter for block comments within SQL scripts.
     */
    public static final String DEFAULT_BLOCK_COMMENT_START_DELIMITER = "/*";

    /**
     * Default end delimiter for block comments within SQL scripts.
     */
    public static final String DEFAULT_BLOCK_COMMENT_END_DELIMITER = "*/";


    /**
     * Prevent instantiation of this utility class.
     */
    private ScriptUtils() {
        /* no-op */
    }

    /**
     * Split an SQL script into separate statements delimited by the provided
     * separator string. Each individual statement will be added to the provided
     * {@code List}.
     * <p>Within the script, the provided {@code commentPrefix} will be honored:
     * any text beginning with the comment prefix and extending to the end of the
     * line will be omitted from the output. Similarly, the provided
     * {@code blockCommentStartDelimiter} and {@code blockCommentEndDelimiter}
     * delimiters will be honored: any text enclosed in a block comment will be
     * omitted from the output. In addition, multiple adjacent whitespace characters
     * will be collapsed into a single space.
     *
     * @param resource                   the resource from which the script was read
     * @param script                     the SQL script; never {@code null} or empty
     * @param separator                  text separating each statement &mdash; typically a ';' or
     *                                   newline character; never {@code null}
     * @param commentPrefix              the prefix that identifies SQL line comments &mdash;
     *                                   typically "--"; never {@code null} or empty
     * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter;
     *                                   never {@code null} or empty
     * @param blockCommentEndDelimiter   the <em>end</em> block comment delimiter;
     *                                   never {@code null} or empty
     * @param statements                 the list that will contain the individual statements
     */
    public static void splitSqlScript(EncodedResource resource, String script, String separator, String commentPrefix,
                                      String blockCommentStartDelimiter, String blockCommentEndDelimiter, List<String> statements) {

        Assert.hasText(script, "script must not be null or empty");
        Assert.notNull(separator, "separator must not be null");
        Assert.hasText(commentPrefix, "commentPrefix must not be null or empty");
        Assert.hasText(blockCommentStartDelimiter, "blockCommentStartDelimiter must not be null or empty");
        Assert.hasText(blockCommentEndDelimiter, "blockCommentEndDelimiter must not be null or empty");

        StringBuilder sb = new StringBuilder();
        boolean inLiteral = false;
        boolean inEscape = false;
        char[] content = script.toCharArray();
        for (int i = 0; i < script.length(); i++) {
            char c = content[i];
            if (inEscape) {
                inEscape = false;
                sb.append(c);
                continue;
            }
            // MySQL style escapes
            if (c == '\\') {
                inEscape = true;
                sb.append(c);
                continue;
            }
            if (c == '\'') {
                inLiteral = !inLiteral;
            }
            if (!inLiteral) {
                if (script.startsWith(separator, i)) {
                    // we've reached the end of the current statement
                    if (sb.length() > 0) {
                        statements.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    i += separator.length() - 1;
                    continue;
                } else if (script.startsWith(commentPrefix, i)) {
                    // skip over any content from the start of the comment to the EOL
                    int indexOfNextNewline = script.indexOf("\n", i);
                    if (indexOfNextNewline > i) {
                        i = indexOfNextNewline;
                        continue;
                    } else {
                        // if there's no EOL, we must be at the end
                        // of the script, so stop here.
                        break;
                    }
                } else if (script.startsWith(blockCommentStartDelimiter, i)) {
                    // skip over any block comments
                    int indexOfCommentEnd = script.indexOf(blockCommentEndDelimiter, i);
                    if (indexOfCommentEnd > i) {
                        i = indexOfCommentEnd + blockCommentEndDelimiter.length() - 1;
                        continue;
                    } else {
                        throw new RuntimeException(String.format("Missing block comment end delimiter [%s] for resource [%s].",
                                blockCommentEndDelimiter, resource.toString()));
                    }
                } else if (c == ' ' || c == '\n' || c == '\t') {
                    // avoid multiple adjacent whitespace characters
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                        c = ' ';
                    } else {
                        continue;
                    }
                }
            }
            sb.append(c);
        }
        if (StringUtils.hasText(sb)) {
            statements.add(sb.toString());
        }
    }

    /**
     * Read a script from the provided resource, using the supplied comment prefix
     * and statement separator, and build a {@code String} containing the lines.
     * <p>Lines <em>beginning</em> with the comment prefix are excluded from the
     * results; however, line comments anywhere else &mdash; for example, within
     * a statement &mdash; will be included in the results.
     *
     * @param resource      the {@code EncodedResource} containing the script
     *                      to be processed
     * @param commentPrefix the prefix that identifies comments in the SQL script &mdash;
     *                      typically "--"
     * @param separator     the statement separator in the SQL script &mdash; typically ";"
     * @return a {@code String} containing the script lines
     * @throws IOException in case of I/O errors
     */
    private static String readScript(EncodedResource resource, String commentPrefix, String separator)
            throws IOException {
        LineNumberReader lnr = new LineNumberReader(resource.getReader());
        try {
            return readScript(lnr, commentPrefix, separator);
        } finally {
            lnr.close();
        }
    }

    /**
     * Read a script from the provided {@code LineNumberReader}, using the supplied
     * comment prefix and statement separator, and build a {@code String} containing
     * the lines.
     * <p>Lines <em>beginning</em> with the comment prefix are excluded from the
     * results; however, line comments anywhere else &mdash; for example, within
     * a statement &mdash; will be included in the results.
     *
     * @param lineNumberReader the {@code LineNumberReader} containing the script
     *                         to be processed
     * @param commentPrefix    the prefix that identifies comments in the SQL script &mdash;
     *                         typically "--"
     * @param separator        the statement separator in the SQL script &mdash; typically ";"
     * @return a {@code String} containing the script lines
     * @throws IOException in case of I/O errors
     */
    public static String readScript(LineNumberReader lineNumberReader, String commentPrefix, String separator)
            throws IOException {
        String currentStatement = lineNumberReader.readLine();
        StringBuilder scriptBuilder = new StringBuilder();
        while (currentStatement != null) {
            if (commentPrefix != null && !currentStatement.startsWith(commentPrefix)) {
                if (scriptBuilder.length() > 0) {
                    scriptBuilder.append('\n');
                }
                scriptBuilder.append(currentStatement);
            }
            currentStatement = lineNumberReader.readLine();
        }
        appendSeparatorToScriptIfNecessary(scriptBuilder, separator);
        return scriptBuilder.toString();
    }

    private static void appendSeparatorToScriptIfNecessary(StringBuilder scriptBuilder, String separator) {
        if (separator == null) {
            return;
        }
        String trimmed = separator.trim();
        if (trimmed.length() == separator.length()) {
            return;
        }
        // separator ends in whitespace, so we might want to see if the script is trying
        // to end the same way
        if (scriptBuilder.lastIndexOf(trimmed) == scriptBuilder.length() - trimmed.length()) {
            scriptBuilder.append(separator.substring(trimmed.length()));
        }
    }

    /**
     * Does the provided SQL script contain the specified delimiter?
     *
     * @param script the SQL script
     * @param delim  String delimiting each statement - typically a ';' character
     */
    public static boolean containsSqlScriptDelimiters(String script, String delim) {
        boolean inLiteral = false;
        char[] content = script.toCharArray();
        for (int i = 0; i < script.length(); i++) {
            if (content[i] == '\'') {
                inLiteral = !inLiteral;
            }
            if (!inLiteral && script.startsWith(delim, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Execute the given SQL script using default settings for separator separators,
     * comment delimiters, and exception handling flags.
     * <p>Statement separators and comments will be removed before executing
     * individual statements within the supplied script.
     * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
     *
     * @param dataSource the dataSource to use to execute the script; already
     *                   configured and ready to use
     * @param resource   the resource to load the SQL script from; encoded with the
     *                   current platform's default encoding
     * @see #executeSqlScript(javax.sql.DataSource, EncodedResource, boolean, boolean, String, String, String, String)
     * @see #DEFAULT_COMMENT_PREFIX
     * @see #DEFAULT_STATEMENT_SEPARATOR
     * @see #DEFAULT_BLOCK_COMMENT_START_DELIMITER
     * @see #DEFAULT_BLOCK_COMMENT_END_DELIMITER
     */
    public static void executeSqlScript(DataSource dataSource, Resource resource) {
        executeSqlScript(dataSource, new EncodedResource(resource));
    }

    /**
     * Execute the given SQL script using default settings for separator separators,
     * comment delimiters, and exception handling flags.
     * <p>Statement separators and comments will be removed before executing
     * individual statements within the supplied script.
     * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
     *
     * @param dataSource the dataSource to use to execute the script; already
     *                   configured and ready to use
     * @param resource   the resource (potentially associated with a specific encoding)
     *                   to load the SQL script from
     * @see #executeSqlScript(DataSource, EncodedResource, boolean, boolean, String, String, String, String)
     * @see #DEFAULT_COMMENT_PREFIX
     * @see #DEFAULT_STATEMENT_SEPARATOR
     * @see #DEFAULT_BLOCK_COMMENT_START_DELIMITER
     * @see #DEFAULT_BLOCK_COMMENT_END_DELIMITER
     */
    public static void executeSqlScript(DataSource dataSource, EncodedResource resource) {
        executeSqlScript(dataSource, resource, false, false, DEFAULT_COMMENT_PREFIX, DEFAULT_STATEMENT_SEPARATOR,
                DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER);
    }

    /**
     * Execute the given SQL script.
     * <p>Statement separators and comments will be removed before executing
     * individual statements within the supplied script.
     * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
     *
     * @param dataSource                 the dataSource to use to execute the script; already
     *                                   configured and ready to use
     * @param resource                   the resource (potentially associated with a specific encoding)
     *                                   to load the SQL script from
     * @param continueOnError            whether or not to continue without throwing an exception
     *                                   in the event of an error
     * @param ignoreFailedDrops          whether or not to continue in the event of specifically
     *                                   an error on a {@code DROP} statement
     * @param commentPrefix              the prefix that identifies comments in the SQL script &mdash;
     *                                   typically "--"
     * @param separator                  the script statement separator; defaults to
     *                                   {@value #DEFAULT_STATEMENT_SEPARATOR} if not specified and falls back to
     *                                   {@value #FALLBACK_STATEMENT_SEPARATOR} as a last resort; may be set to
     *                                   {@value #EOF_STATEMENT_SEPARATOR} to signal that the script contains a
     *                                   single statement without a separator
     * @param blockCommentStartDelimiter the <em>start</em> block comment delimiter; never
     *                                   {@code null} or empty
     * @param blockCommentEndDelimiter   the <em>end</em> block comment delimiter; never
     *                                   {@code null} or empty
     * @see #DEFAULT_STATEMENT_SEPARATOR
     * @see #FALLBACK_STATEMENT_SEPARATOR
     * @see #EOF_STATEMENT_SEPARATOR
     */
    public static void executeSqlScript(DataSource dataSource, EncodedResource resource, boolean continueOnError,
                                        boolean ignoreFailedDrops, String commentPrefix, String separator, String blockCommentStartDelimiter,
                                        String blockCommentEndDelimiter) {

        try {
            if (logger.isInfoEnabled()) {
                logger.info("Executing SQL script from " + resource);
            }

            long startTime = System.currentTimeMillis();
            List<String> statements = new LinkedList<String>();
            String script;
            try {
                script = readScript(resource, commentPrefix, separator);
            } catch (IOException ex) {
                throw new CannotReadScriptException(resource, ex);
            }

            if (separator == null) {
                separator = DEFAULT_STATEMENT_SEPARATOR;
            }
            if (!EOF_STATEMENT_SEPARATOR.equals(separator) && !containsSqlScriptDelimiters(script, separator)) {
                separator = FALLBACK_STATEMENT_SEPARATOR;
            }

            splitSqlScript(resource, script, separator, commentPrefix, blockCommentStartDelimiter,
                    blockCommentEndDelimiter, statements);
            for (String statement : statements) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                jdbcTemplate.execute(statement);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            if (logger.isInfoEnabled()) {
                logger.info("Executed SQL script from " + resource + " in " + elapsedTime + " ms.");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to execute database script from resource [" + resource + "]", ex);
        }
    }

}