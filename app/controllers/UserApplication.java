package controllers;

import static play.data.Form.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.validation.ValidationException;

import models.SecurityRole;
import models.Users;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import security.PasswordHash;
import be.objectify.deadbolt.java.actions.Dynamic;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.node.ObjectNode;


@SubjectPresent
public class UserApplication extends Controller {
    
	/**
     * This result directly redirect to company list home.
     */
    public static final Result GO_HOME = redirect(
        routes.UserApplication.list(0, "userName", "asc", ""));
	
    /**
	 * Displays info for one user
	 * @param id
	 * @return
	 */
	public static Result userIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		
		return ok(views.html.users.userIndex.render(Users.find.byId(id)));
	}
	
			
	/**
	 * Returns a list of 50 Users for use in userList or filtered queries
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
	public static Result list(int page, String sortBy, String order, String filter) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		int ppid = 30;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 30;
			}
		}
		return ok(
				views.html.users.userList.render(
						Users.page(page, ppid, sortBy , order, filter),
						sortBy, order, filter
						));
	}
	
	/**
	 * 
	 * display createUser form
	 */
	@Restrict({@Group("admin")})
	public static Result createUser() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Users> newUserForm = form(Users.class);
		return ok(views.html.users.createUserForm.render(newUserForm));

	}
	
	/**
	 * Saves user to db
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result save() {
		Form<Users> newUserForm = form(Users.class).bindFromRequest();
		if(!newUserForm.field("password").valueOr("").isEmpty()) {
            if(!newUserForm.field("password").valueOr("").equals(newUserForm.field("repeatPassword").value())) {
            	newUserForm.reject("repeatPassword", "Password don't match");
            }
		}
		String[] sss=newUserForm.field("password").value().split("\\s");
		if(sss.length==0 || newUserForm.field("password").valueOr("").isEmpty()) {
			newUserForm.reject("password", "Password is empty");
		}
		if (newUserForm.hasErrors() ||
				sss.length==0) {
			return badRequest(views.html.users.createUserForm.render(
					newUserForm));
		}
		Users u = newUserForm.get();
		Ebean.beginTransaction();
		try {
			u.password = PasswordHash.createHash(u.password);
			u.save();
			flash("success", "User " + newUserForm.get().userName + " has been created");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("User save error",e.fillInStackTrace());
			flash("error", "User save error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return GO_HOME;
	}
	
	/**
	 * Display editUser form
	 * @param id
	 * @return
	 */
	
	@Dynamic(value = "editProfile")
	public static Result editUser(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Users> userForm = form(Users.class).fill(Users.find.byId(id));
			return ok(views.html.users.editUser.render(id, userForm));
	}
	
	/**
	 * Update user with id = id
	 * @param id
	 * @return
	 */
	@Dynamic(value = "editProfile")
	public static Result update(Long id) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {
				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (Users.class.getField(key).getType().toString()
								.compareTo("interface java.util.List") == 0) {
							newData.put(key + "[0]" + ".id", value[0]);
						} else {
							newData.put(key, value[0]);
						}
					} catch (NoSuchFieldException | SecurityException e) {
						newData.put(key, value[0]);
					}
				} else if (value.length > 1) {
					for (int i = 0; i < value.length; i++) {
						newData.put(key + "[" + i + "]" + ".id", value[i]);
					}
				}
			}
		}
		Form<Users> updateForm = new Form<Users>(Users.class)
				.bind(newData);
		if (updateForm.hasErrors()) {
			return badRequest(views.html.users.editUser.render(id, updateForm));
		}
		
		Users u = updateForm.get();
		Ebean.beginTransaction();
		try {
			u.id = id;
			if(!Users.findByUserName(session().get("userName")).roles.contains(SecurityRole.findByName("admin")) &&
					!u.userName.equals(Users.find.byId(id).userName)) throw new ValidationException("Only admininstrator can change user names");
			u.saveManyToManyAssociations("userFiles");
			u.update(id);
			flash("success", "User " + updateForm.get().userName + " has been updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("User update error",e.fillInStackTrace());
			flash("error", "User update error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect((routes.UserApplication.userIndex(id)));
	}
	
	@Dynamic(value = "editProfile")
	public static Result editPassword(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(
	            views.html.users.editPassword.render(
	            		id, form(PasswordChange.class)
	            		));
	}
	
	@Dynamic(value = "editProfile")
	public static Result updatePassword(Long id) {
			
		Form<PasswordChange> passForm = Form.form(PasswordChange.class).bindFromRequest();
		if(!passForm.field("newPassword").valueOr("").isEmpty()) {
            if(!passForm.field("newPassword").valueOr("").equals(passForm.field("repeatPassword").value())) {
            	passForm.reject("repeatPassword", "Passwords don't match");
            }
		}
		if(passForm.hasErrors()) {
			return badRequest(views.html.users.editPassword.render(id,passForm));
		} else {
			Ebean.beginTransaction();
			try {
				Users u = Users.find.byId(id);
				u.password=PasswordHash.createHash(passForm.get().newPassword);
				u.update(id);
				flash("success","Password successfully changed");
				Ebean.commitTransaction();
			} catch (Exception e) {
				Logger.error("Password update error",e.fillInStackTrace());
				flash("error","password not updated");
				Ebean.rollbackTransaction();
			} finally {
				Ebean.endTransaction();
			}
			
		}
		return redirect(routes.UserApplication.userIndex(id));
		
	}
	
	@Restrict({@Group("admin")})
	public static Result disableLogin(Long uid) {
		Ebean.beginTransaction();
		try {
			Users u = Users.find.byId(uid);
			u.password=null;
			u.update(uid);
			flash("success","This user can't login anymore");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Error in disabling user",e.fillInStackTrace());
			flash("error","Disabling user failed");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.UserApplication.userIndex(uid));
	}
	
	@Restrict({@Group("admin")})
	public static Result resetPassword(Long uid) {
		Ebean.beginTransaction();
		try {
			Users u = Users.find.byId(uid);
			u.password=PasswordHash.createHash("abc");
			u.update(uid);
			flash("success","Password was successfully reset to 'abc'");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Password reset error",e.fillInStackTrace());
			flash("error","password not reset");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.UserApplication.userIndex(uid));
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result editSecurity(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.users.editSecurity.render(
				id,
				Users.find.byId(id)
				));
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result updateSecurity(Long id) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {
				String[] value = urlFormEncoded.get(key);
				for (int i = 0; i < value.length; i++) {
					newData.put(key + "[" + i + "]", value[i]);
				}
			}
		}
		Users uu = Users.find.byId(id);
		uu.roles=new ArrayList<SecurityRole>();
		if(newData.size()>0) {
			for(String rr : newData.values()) {
				uu.roles.add(SecurityRole.findByName(rr));
			}
			uu.update(id);
			uu.saveManyToManyAssociations("roles");
			
		} else {
			uu.deleteManyToManyAssociations("roles");
		}
		
		return redirect(routes.UserApplication.userIndex(id));
	}
	
	/**
	 * class for change password
	 * @author rook
	 *
	 */
	public static class PasswordChange {
		public String oldPassword;
        public String newPassword;
        
        public String validate() {
            if(Users.authenticate(session().get("userName"), oldPassword) == null) {
                return "Old password is wrong!";
            }
            return null;
        }
	}
	
	/**
	 * 
	 * @param uid
	 * @return
	 */
	public static Result setPageSize(Long uid) {
		DynamicForm inForm = Form.form().bindFromRequest();
		Integer ps = -1;
		try {
			ps = Integer.valueOf(inForm.get("pgSize"));
		}
		catch (Exception eex) {
			ps = -1;
		}
		if (ps !=-1)
			session().put("pagesize",ps.toString());
		else session().remove("pagesize");
		
		return redirect(routes.UserApplication.userIndex(uid));
	}
	
	/**
     * 
     * @param
     * @param
     * @return
     */
    public static Result disciplineUserFiller(Long disId) {
    	ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	
    	for(Users us : Users.find.where().eq("discipline.id", disId)
    			.orderBy("lastName")
    			.findList()) {
    		ObjectNode node = Json.newObject();
    		node.put("id", us.id);
    		node.put("value", us.lastName + " "+us.firstName);
    		allnodes.add(node);
    	}
    	return ok(play.libs.Json.toJson(allnodes));
        }
	
}
