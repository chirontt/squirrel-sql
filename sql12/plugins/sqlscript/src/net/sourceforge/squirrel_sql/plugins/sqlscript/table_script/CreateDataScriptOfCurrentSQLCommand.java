package net.sourceforge.squirrel_sql.plugins.sqlscript.table_script;

/*
 * Copyright (C) 2001 Johan Compagner
 * jcompagner@j-com.nl
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.*;

import net.sourceforge.squirrel_sql.client.session.EditableSqlCheck;
import net.sourceforge.squirrel_sql.client.session.ISQLPanelAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.sqltab.BaseSQLTab;
import net.sourceforge.squirrel_sql.fw.sql.*;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.StringUtilities;
import net.sourceforge.squirrel_sql.plugins.sqlscript.FrameWorkAcessor;
import net.sourceforge.squirrel_sql.plugins.sqlscript.SQLScriptPlugin;

public class CreateDataScriptOfCurrentSQLCommand extends CreateDataScriptCommand
{

   private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(CreateDataScriptOfCurrentSQLCommand.class);

   private final SQLScriptPlugin _plugin;

   public CreateDataScriptOfCurrentSQLCommand(ISession session, SQLScriptPlugin plugin)
   {
      super(session, false);
      _plugin = plugin;
   }

   /**
    * Execute this command.
    */
   public void execute()
   {
      _session.getApplication().getThreadPool().addTask(() -> doCreateDataScript());
      showAbortFrame();
   }

   private void doCreateDataScript()
   {
      final StringBuilder sbRows = new StringBuilder();

      try
      {
          ISQLPanelAPI api = FrameWorkAcessor.getSQLPanelAPI(_session);

          String scripts = api.getSQLScriptToBeExecuted();

         IQueryTokenizer qt = _session.getQueryTokenizer();
         qt.setScriptToTokenize(scripts);

         if(false == qt.hasQuery())
         {
            // i18n[CreateDataScriptOfCurrentSQLCommand.noQuery=No query found to create the script from.]
            _session.showErrorMessage(s_stringMgr.getString("CreateTableOfCurrentSQLCommand.noQuery"));
            return;
         }

         ISQLConnection conn = _session.getSQLConnection();

         while (qt.hasQuery())
         {
            final Statement stmt = conn.createStatement();
            try
            {
               String sql = qt.nextQuery().getQuery();

               ResultSet srcResult = stmt.executeQuery(sql);
               String tableName = getFirstTableNameFromResultSetMetaData(srcResult);

               if (StringUtilities.isEmpty(tableName, true))
               {
                  tableName = new EditableSqlCheck(sql).getTableNameFromSQL();
               }

               if (StringUtilities.isEmpty(tableName, true))
               {
                  tableName = "PressCtrlH";
               }

               genInserts(srcResult, tableName, sbRows, false);
            }
            finally
            {
               SQLUtilities.closeStatement(stmt);
            }
         }  // end while
      }
      catch (Exception e)
      {
         _session.showErrorMessage(e);
      }
      finally
      {
         SwingUtilities.invokeLater(new Runnable()
         {
            public void run()
            {
               if (sbRows.length() > 0)
               {
                  FrameWorkAcessor.getSQLPanelAPI(_session).appendSQLScript(sbRows.toString(), true);

                  if (false == _session.getSelectedMainTab() instanceof BaseSQLTab)
                  {
                     _session.selectMainTab(ISession.IMainPanelTabIndexes.SQL_TAB);
                  }
               }
               hideAbortFrame();
            }
         });
      }
   }

   private String getFirstTableNameFromResultSetMetaData(ResultSet srcResult) throws SQLException
   {
      ResultSetMetaData metaData = srcResult.getMetaData();
      //String tableName = metaData.getTableName(1);

      for (int i = 1; i <= metaData.getColumnCount(); i++)
      {
         ITableInfo tInfo = new TableInfo(
               metaData.getCatalogName(i),
               metaData.getSchemaName(i),
               metaData.getTableName(i),
               "TABLE", "",
               _session.getMetaData());

         String tableName = ScriptUtil.getTableName(tInfo);

         if(false == StringUtilities.isEmpty(tableName, true))
         {
            return tableName;
         }

      }

      return null;
   }
}