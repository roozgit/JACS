/*
 * Copyright 2012 Steve Chaloner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package security;

import models.Users;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.AbstractDeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;
import controllers.routes;

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
public class MyDeadboltHandler extends AbstractDeadboltHandler
{
    public F.Promise<Result> beforeAuthCheck(Http.Context context)
    {
        // returning null means that everything is OK.  Return a real result if you want a redirect to a login page or
        // somewhere else
    	play.mvc.Controller.session().put("referrer",context.request().uri().toString());
        return F.Promise.pure(null);
    }

    public F.Promise<Subject> getSubject(Http.Context ctx)
	{
	final String userName = ctx.session().get("userName");
        return F.Promise.promise(new F.Function0<Subject>()
        {
            @Override
            public Subject apply() throws Throwable {
                return Users.findByUserName(userName);
            }
        });
    	}    

    public DynamicResourceHandler getDynamicResourceHandler(Http.Context context)
    {
        return new MyDynamicResourceHandler();
    }

    @Override
    public F.Promise<Result> onAuthFailure(Http.Context context,
                                                 String content)
    {
        // you can return any result from here - forbidden, etc
        return F.Promise.promise(new F.Function0<Result>()
        {
            @Override
            public Result apply() throws Throwable {
            	if( context.request().uri().toString()=="/")
            		return redirect(routes.Application.index());
            	else {
            		play.api.i18n.Lang en = new play.api.i18n.Lang("en", "US");
            		play.api.i18n.Lang fa = new play.api.i18n.Lang("fa", "IR");
            		play.api.i18n.Lang retLang;
            		play.mvc.Controller.response().setContentType("text/html ;charset=utf-8");
            		
            		String langNum =play.mvc.Controller.session().get("play_user_lang");
            		if(langNum.equals("1")) {
            			retLang = fa;
            		} else {
            			retLang = en;
            		}
                    return forbidden("<h3>"+play.i18n.Messages.get(retLang,"authError")+"</h3>" +
                	"<p><a href=/Dashboard>Dashboard</a></p>" +
                    "<p><a href=/logout>Log out</a></p>");
            	}
            	
            }
        });
    }
}
