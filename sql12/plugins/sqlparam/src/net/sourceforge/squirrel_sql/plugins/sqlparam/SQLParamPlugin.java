package net.sourceforge.squirrel_sql.plugins.sqlparam;
/*
 * Copyright (C) 2007 Thorsten Mürell
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import net.sourceforge.squirrel_sql.client.gui.session.ObjectTreeInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.session.SQLInternalFrame;
import net.sourceforge.squirrel_sql.client.plugin.DefaultSessionPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.PluginResources;
import net.sourceforge.squirrel_sql.client.plugin.PluginSessionCallback;
import net.sourceforge.squirrel_sql.client.session.ISQLPanelAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.event.ISQLExecutionListener;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.ObjectTreePanel;
import net.sourceforge.squirrel_sql.client.session.mainpanel.sqltab.AdditionalSQLTab;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;


/**
 * The plugin class.
 *
 * @author Thorsten Mürell
 */
public class SQLParamPlugin extends DefaultSessionPlugin
{
	private final static ILogger log = LoggerController.createLogger(SQLParamPlugin.class);

	private Resources resources;
//	private static final String PREFS_FILE_NAME = "sqlparamprefs.xml";
	Map<String, String> cache;
	
    /** 
     * Remember which sqlpanelapis we've registered listeners with so that we 
     * can unregister them when it's time to unload.
     */
    HashMap<ISQLPanelAPI, ISQLExecutionListener> panelListenerMap = new HashMap<ISQLPanelAPI, ISQLExecutionListener>();
    
	/**
	 * Return the internal name of this plugin.
	 *
	 * @return  the internal name of this plugin.
	 */
	@Override
	public String getInternalName()
	{
		return "sqlparam";
	}

	/**
	 * Return the descriptive name of this plugin.
	 *
	 * @return  the descriptive name of this plugin.
	 */
	@Override
	public String getDescriptiveName()
	{
		return "SQL Parametrisation";
	}

	/**
	 * Returns the current version of this plugin.
	 *
	 * @return  the current version of this plugin.
	 */
	@Override
	public String getVersion()
	{
		return "1.0.1";
	}


	/**
	 * Returns a comma separated list of other contributors.
	 *
	 * @return      Contributors names.
	 */
	@Override
	public String getContributors()
	{
		return "";
	}

	/**
	 * Returns the authors name.
	 *
	 * @return  the authors name.
	 */
	@Override
	public String getAuthor()
	{
		return "Thorsten M\u00FCrell";
	}

	/**
	 * Returns the name of the change log for the plugin. This should
	 * be a text or HTML file residing in the <TT>getPluginAppSettingsFolder</TT>
	 * directory.
	 *
	 * @return	the changelog file name or <TT>null</TT> if plugin doesn't have
	 * 			a change log.
	 */
	@Override
	public String getChangeLogFileName()
	{
		return "changes.txt";
	}

	/**
	 * Returns the name of the Help file for the plugin. This should
	 * be a text or HTML file residing in the <TT>getPluginAppSettingsFolder</TT>
	 * directory.
	 *
	 * @return	the Help file name or <TT>null</TT> if plugin doesn't have
	 * 			a help file.
	 */
	@Override
	public String getHelpFileName()
	{
		return "doc/readme.html";
	}

	/**
	 * Returns the name of the Licence file for the plugin. This should
	 * be a text or HTML file residing in the <TT>getPluginAppSettingsFolder</TT>
	 * directory.
	 *
	 * @return	the Licence file name or <TT>null</TT> if plugin doesn't have
	 * 			a licence file.
	 */
	@Override
	public String getLicenceFileName()
	{
		return "licence.txt";
	}

	/**
	 * Called on application startup after application started.
	 */
	@Override
	public void initialize() throws PluginException
	{
		resources = new Resources(this);
	}

	/**
	 * Called on plugin unloading.
	 */
	@Override
	public void unload()
	{
		for (Map.Entry<ISQLPanelAPI, ISQLExecutionListener> entry :  panelListenerMap.entrySet()) {
			ISQLPanelAPI api = entry.getKey();
			ISQLExecutionListener listener = entry.getValue();
			removeSQLExecutionListener(api, listener);
		}
		panelListenerMap.clear();
	}
	
	/**
	 * 
	 */
	@Override
	public boolean allowsSessionStartedInBackground()
	{
		return true;
	}

	/**
	 * Called on session creating by callback.
	 * 
	 * @param session The session
	 */
	@Override
	public void sessionCreated(ISession session)
	{
		cache = new Hashtable<String, String>();
	}
	
	/**
	 * Returns the value cache.
	 * 
	 * @return A map caching parameters to old values
	 */
	public Map<String, String> getCache() {
		return cache;
	}
	
	/**
	 * Session has been started.
	 * 
	 * @param	session		Session that has started.
	 * @return The callback to start on session events.
	 */
	@Override
	public PluginSessionCallback sessionStarted(final ISession session)
	{
		log.info("Initializing plugin");
		ISQLPanelAPI sqlPaneAPI = session.getSessionPanel().getMainSQLPaneAPI();

		initSQLParam(sqlPaneAPI, session);

		PluginSessionCallback ret = new PluginSessionCallback()
		{
			@Override
			public void sqlInternalFrameOpened(final SQLInternalFrame sqlInternalFrame, final ISession sess)
			{
				initSQLParam(sqlInternalFrame.getMainSQLPanelAPI(), sess);
			}

			@Override
			public void objectTreeInternalFrameOpened(ObjectTreeInternalFrame objectTreeInternalFrame, ISession sess)
			{
			}

			@Override
			public void objectTreeInSQLTabOpened(ObjectTreePanel objectTreePanel)
			{
			}

			@Override
         public void additionalSQLTabOpened(AdditionalSQLTab additionalSQLTab)
         {
            initSQLParam(additionalSQLTab.getSQLPanelAPI(), additionalSQLTab.getSQLPanelAPI().getSession());
         }

      };

		return ret;
	}
	
	/**
	 * This method is called on session closing an needs to free resources.
	 * 
	 * @param session the session to be closed
	 */
	@Override
	public void sessionEnding(ISession session)
	{
		ArrayList<ISQLPanelAPI> toRemove = new ArrayList<>();
		for (ISQLPanelAPI sqlPanelAPI : panelListenerMap.keySet())
		{
			if(sqlPanelAPI.getSession().getIdentifier().equals(session.getIdentifier()))
			{
				toRemove.add(sqlPanelAPI);
			}
		}

		for (ISQLPanelAPI sqlPanelAPI : toRemove)
		{
			ISQLExecutionListener listener = panelListenerMap.remove(sqlPanelAPI);
			removeSQLExecutionListener(sqlPanelAPI, listener);
		}
	}

	private void removeSQLExecutionListener(ISQLPanelAPI api, ISQLExecutionListener listener)
	{
		if (api != null && listener != null) {
			api.removeSQLExecutionListener(listener);
		}
	}	
	
	private void initSQLParam(final ISQLPanelAPI sqlPaneAPI, final ISession session)
	{
		final SQLParamPlugin plugin = this;
		
		GUIUtils.processOnSwingEventThread(() -> addSqlParamExecutionListener(plugin, sqlPaneAPI));
	}

	private void addSqlParamExecutionListener(SQLParamPlugin plugin, ISQLPanelAPI sqlPaneAPI)
	{
		log.info("Adding SQL execution listener.");
		ISQLExecutionListener listener =  new SQLParamExecutionListener(plugin, sqlPaneAPI);
		sqlPaneAPI.addSQLExecutionListener(listener);
		panelListenerMap.put(sqlPaneAPI, listener);
	}

	/**
	 * Retrieve plugins resources.
	 * 
	 * @return	Plugins resources.
	 */
	 public PluginResources getResources()
	 {
		 return resources;
	 }

}
