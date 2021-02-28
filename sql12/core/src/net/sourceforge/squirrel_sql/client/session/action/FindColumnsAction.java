package net.sourceforge.squirrel_sql.client.session.action;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.action.findcolums.FindColumnsCtrl;
import net.sourceforge.squirrel_sql.client.session.action.findcolums.FindColumnsScope;

import java.awt.event.ActionEvent;

public class FindColumnsAction extends SquirrelAction implements ISessionAction
{
   private ISession _session;

   public FindColumnsAction(IApplication app)
   {
      super(app);
   }

   @Override
   public void setSession(ISession session)
   {
      _session = session;
      setEnabled(null != _session);
   }


   public void actionPerformed(ActionEvent e)
   {
      if (_session == null)
      {
         return;
      }

      new FindColumnsCtrl(new FindColumnsScope(_session));
   }
}
