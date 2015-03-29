package controllers;

import static play.data.Form.form;


import javax.validation.ValidationException;

import models.Disciplines;
import models.SecurityRole;
//import views.html.*;
import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;

@SubjectPresent
public class DisciplineApplication extends Controller {

	@Restrict({ @Group("admin")})
	public static Result createSecurityRoles(Long id) {
		String dname = Disciplines.find.byId(id).name;
		Ebean.beginTransaction();
		try {
			SecurityRole newSR = new SecurityRole();
			newSR.name= dname+".Authorizer";
			if(SecurityRole.findByName(newSR.name) != null)
				throw new ValidationException("Permissions already exists");
			newSR.description = "Can authorize " + dname + " work";
			newSR.save();
			
			SecurityRole newSR2 = new SecurityRole();
			newSR2.name= dname +".InCharge";
			if(SecurityRole.findByName(newSR2.name) != null)
				throw new ValidationException("Permissions already exists");
			newSR2.description = "Can be in charge of " + dname + " work";
			newSR2.save();
			
			SecurityRole newSR3 = new SecurityRole();
			newSR3.name= Disciplines.find.byId(id).name+".Verifier";
			if(SecurityRole.findByName(newSR3.name) != null)
				throw new ValidationException("Permissions already exists");
			newSR.description = "Can verify " + dname + " work";
			newSR3.save();
			Ebean.commitTransaction();
			flash("success","Security roles added");
		} catch (Exception e) {
			Logger.error("Security roles creation error",e.fillInStackTrace());
			flash("error","Can't create roles");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.DisciplineApplication.disciplineIndex(id));
	}
	
	/**
     * This result directly redirect to company list home.
     */
    public static final Result GO_HOME = redirect(
        routes.DisciplineApplication.list(0, "name", "asc", ""));

	public static Result disciplineIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		
		return ok(views.html.disciplines.disciplineIndex.render(
				Disciplines.find.byId(id)
				));
	}
	
	/**
	 * Returns a list of 10 disciplines for use in disciplineList or filtered queries
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
		int ppid = 10;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 10;
			}
		}
		return ok(
				views.html.disciplines.disciplineList.render(
						Disciplines.page(page, ppid, sortBy , order, filter),
						sortBy, order, filter
						));
	}
		
	@Restrict({ @Group("admin")})
	public static Result createDiscipline() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Disciplines> newDisciplineForm = form(Disciplines.class);
		return ok(views.html.disciplines.createDisciplineForm.render(newDisciplineForm));

	}
	
	@Restrict({ @Group("admin")})
	public static Result save() {
		Form<Disciplines> newDisciplineForm = form(Disciplines.class).bindFromRequest();
		if (newDisciplineForm.hasErrors()) {
			return badRequest(views.html.disciplines.createDisciplineForm.render(
					newDisciplineForm));
		}
		Disciplines d = newDisciplineForm.get();
		try {
			d.save();
		} catch (Exception ex) {
			return badRequest(views.html.disciplines.createDisciplineForm.render(
					newDisciplineForm));
		}
		return GO_HOME;
	}
	
	@Restrict({ @Group("admin")})
	public static Result editDiscipline(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Disciplines> disciplineForm = form(Disciplines.class).fill(Disciplines.find.byId(id));
		return ok(views.html.disciplines.editDiscipline.render(id, disciplineForm));
	}
	
	@Restrict({ @Group("admin")})
	public static Result update(Long id) {
		Form<Disciplines> disciplineForm = form(Disciplines.class)
				.bindFromRequest();
		if (disciplineForm.hasErrors()) {
			return badRequest(views.html.disciplines.editDiscipline.render(
					id, disciplineForm));
		}
		disciplineForm.get().update(id);
		return GO_HOME;
	}
	
}
