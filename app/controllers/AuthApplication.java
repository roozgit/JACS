package controllers;

import static play.data.Form.form;
import models.Users;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;

public class AuthApplication extends Controller{
	
	/**
     * Login page.
     */
    public static Result login() {
    	response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
        return ok(
            views.html.login.render(form(Login.class))
        );
    }
	
    /**
     * Handle login form submission.
     */
    public static Result authenticate() {
        Form<Login> loginForm = Form.form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return badRequest(views.html.login.render(loginForm));
        } else {
        	String url= play.mvc.Controller.session().get("referrer");
        	if(url==null) url="/Dashboard";
            session().clear();
            session("userName", loginForm.get().userName);
            return redirect(
                url
            );
        }
    }
    
    public static Result logout() {
        session().clear();
        flash("success", "You've been logged out");
        return redirect(
            routes.AuthApplication.login()
        );
    }
	
	// -- Authentication
public static class Login {
        public String userName;
        public String password;
        
        public String validate() {
            if(Users.authenticate(userName, password) == null) {
                return "Invalid user or password!";
            }
            return null;
        }
        
    }

}
