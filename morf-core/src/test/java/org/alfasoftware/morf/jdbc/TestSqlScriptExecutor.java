package org.alfasoftware.morf.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test for {@link SqlScriptExecutor}.
 *
 * @author Copyright (c) Alfa Financial Software 2017
 */
public class TestSqlScriptExecutor {

  private final String            sqlScriptOne      = "update table set column = 1;";
  private final String            sqlScriptTwo      = "update table2 set column = 2;";
  private final List<String>      sqlScripts        = ImmutableList.of(sqlScriptOne, sqlScriptTwo);

  private final Connection        connection        = mock(Connection.class);
  private final Statement         statement         = mock(Statement.class);
  private final DataSource        dataSource        = mock(DataSource.class);
  private final SqlDialect        sqlDialect        = mock(SqlDialect.class);
  private final DatabaseType      databaseType      = mock(DatabaseType.class);

  private final SqlScriptExecutor sqlScriptExecutor = new SqlScriptExecutorProvider(dataSource, sqlDialect).get();


  /**
   * Set up mocks.
   */
  @Before
  public void setUp() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(sqlDialect.getDatabaseType()).thenReturn(databaseType);
    when(databaseType.reclassifyException(any(Exception.class))).thenAnswer(invoc -> (Exception) invoc.getArguments()[0]);
  }


  /**
   * Verify that {@link SqlScriptExecutor#execute(Iterable)} returns the number of rows updated.
   */
  @Test
  public void testExecute() throws SQLException {
    when(statement.getUpdateCount()).thenReturn(5).thenReturn(2);

    int result = sqlScriptExecutor.execute(sqlScripts);
    assertEquals("Return value", 7, result);
  }


  /**
   * Verify that exception handling works when a statement cannot be executed
   */
  @Test
  public void testExecuteFailure() throws Exception {
    when(statement.execute(sqlScriptOne)).thenThrow(new SQLException());

    try {
      sqlScriptExecutor.execute(sqlScripts);
      fail("Expected RuntimeSqlException");
    } catch (RuntimeSqlException e) {
      assertTrue("Message", e.getMessage().startsWith("Error executing SQL [" + sqlScriptOne + "]"));
      assertEquals("Cause", SQLException.class, e.getCause().getClass());
    }
  }


  /**
   * Verify that {@link SqlScriptExecutor#execute(Iterable, Connection) returns the number of rows updated.
   */
  @Test
  public void testExecuteWithScriptAndConnectionParameters() throws SQLException {
    when(statement.getUpdateCount()).thenReturn(5).thenReturn(2);

    int result = sqlScriptExecutor.execute(sqlScripts, connection);
    assertEquals("Return value", 7, result);
  }


  /**
   * Verify that exception handling works when a statement cannot be executed.
   */
  @Test
  public void testExecuteWithScriptAndConnectionParamatersFailure() throws Exception {
    when(statement.execute(sqlScriptOne)).thenThrow(new SQLException());

    try {
      sqlScriptExecutor.execute(sqlScripts, connection);
      fail("Expected RuntimeSqlException");
    } catch (RuntimeSqlException e) {
      assertTrue("Message", e.getMessage().startsWith("Error executing SQL [" + sqlScriptOne + "]"));
      assertEquals("Cause", SQLException.class, e.getCause().getClass());
    }
  }


  /**
   * Verify that {@link SqlScriptExecutor#executeAndCommit(Iterable, Connection)} a list of scripts will return the number of rows updated and commit all
   * statements.
   */
  @Test
  public void testExecuteAndCommit() throws SQLException {
    when(statement.getUpdateCount()).thenReturn(5).thenReturn(2);

    int result = sqlScriptExecutor.executeAndCommit(sqlScripts, connection);
    assertEquals("Return value", 7, result);
    verify(connection, times(sqlScripts.size())).commit();
  }


  /**
   * Verify that exception handling works when a statement cannot be executed.
   */
  @Test
  public void testExecuteAndCommitFailure() throws Exception {
    when(statement.execute(sqlScriptOne)).thenThrow(new SQLException());

    try {
      sqlScriptExecutor.executeAndCommit(sqlScripts, connection);
      fail("Expected RuntimeSqlException");
    } catch (RuntimeSqlException e) {
      assertTrue("Message", e.getMessage().startsWith("Error executing SQL [" + sqlScriptOne + "]"));
      assertEquals("Cause", SQLException.class, e.getCause().getClass());
    }
  }


  /**
   * Test that exceptions are reclassified by the DatabaseType and wrapped
   */
  @Test
  public void testExceptionReclassification() throws Exception {
    RuntimeException originalException = new RuntimeException();
    SQLException transformedException = new SQLException();
    when(statement.execute(sqlScriptOne)).thenThrow(originalException);
    when(databaseType.reclassifyException(originalException)).thenReturn(transformedException);

    try {
      sqlScriptExecutor.executeAndCommit(sqlScripts, connection);
      fail("Expected RuntimeSqlException");
    } catch (RuntimeSqlException e) {
      assertTrue("Message", e.getMessage().startsWith("Error executing SQL [" + sqlScriptOne + "]"));
      assertEquals("Cause", transformedException, e.getCause());
    }
  }
}