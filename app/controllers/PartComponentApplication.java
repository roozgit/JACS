package controllers;

import static play.data.Form.form;
import models.Components;
//import views.html.*;
import models.PartsComponents;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;

@SubjectPresent
public class PartComponentApplication extends Controller {

	/*
	 * //indexes parts<->components relationships public static Result
	 * partComponentIndex() { response().setHeader("Cache-Control",
	 * "no-store, no-cache, must-revalidate"); response().setHeader("Pragma",
	 * "no-cache"); return
	 * ok(views.html.partscomponents.partComponentIndex.render
	 * (PartsComponents.find.all() )); }
	 */

	/**
	 * Returns a list of 25 parts for a specific component for use in
	 * subunitList view
	 * 
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
	public static Result list(Long parentComponentId, int page, String sortBy,
			String order, String filter) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		int ppid = 20;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 20;
			}
		}
		return ok(views.html.partscomponents.partComponentList
				.render(parentComponentId, PartsComponents.page(
						parentComponentId, page, ppid, sortBy, order, filter),
						sortBy, order, filter));
	}

	/**
	 * Lists a page of partscomponents ordered by name and ascending
	 * 
	 * @return first page of list
	 */
	public static Result componentList(Long parentComponentId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");

		return list(parentComponentId, 0, "part.name", "asc", "");
	}

	/**
	 * Displays partcomponent create form
	 * 
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result createPartComponent(Long parentComponentId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<PartsComponents> newPCForm = form(PartsComponents.class);
		return ok(views.html.partscomponents.createPartComponentForm.render(
				parentComponentId, newPCForm));

	}

	/**
	 * Saves partcomponent
	 * 
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result save(Long parentComponentId) {
		Form<PartsComponents> newPCForm = form(PartsComponents.class)
				.bindFromRequest();
		if (newPCForm.hasErrors()
				|| newPCForm.field("part.id").value().length() == 0) {
			return badRequest(views.html.partscomponents.createPartComponentForm
					.render(parentComponentId, newPCForm));
		}
		Ebean.beginTransaction();
		try {
			PartsComponents pc = newPCForm.get();
			pc.component = Components.find.byId(parentComponentId);
			pc.save();
			flash("success", "Part assigned to component");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("PartComponent save error",e.fillInStackTrace());
			flash("error", "PartComponent save error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}

		return redirect(routes.PartComponentApplication.list(parentComponentId,
				0, "part.name", "asc", ""));
	}

	/**
	 * display edit component form
	 * 
	 * @param id
	 *            Id of component
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result editPartComponent(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<PartsComponents> pcForm = form(PartsComponents.class).fill(
				PartsComponents.find.byId(id));
		return ok(views.html.partscomponents.editPartComponentForm.render(id,
				pcForm,PartsComponents.find.byId(id).component.id));
	}

	/**
	 * Updates partcomponent
	 * 
	 * @param id
	 *            Id
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result update(Long id, Long parentCompId) {
		Form<PartsComponents> pcForm = form(PartsComponents.class)
				.bindFromRequest();
		if (pcForm.hasErrors() || pcForm.field("part.id").value().length() == 0) {
			return badRequest(views.html.partscomponents.editPartComponentForm
					.render(id, pcForm,parentCompId));
		}
		Ebean.beginTransaction();
		try {
			pcForm.get().update(id);
			flash("success", "Part assigned to component");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("PartComponent update error",e.fillInStackTrace());
			flash("error", "PartComponent update error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.PartComponentApplication.list(parentCompId,0,"name","asc",""));
	}
	
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result delete(Long id, Long parentComponentId) {
		PartsComponents.find.ref(id).delete();
		return redirect(routes.PartComponentApplication.list(parentComponentId,0,"name","asc",""));
	}
	
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result copyPartsComponents(Long parentComponentId) {
		DynamicForm requestData = form().bindFromRequest();
		String newParentComponentId = requestData.get("component.id");
		if(PartsComponents.copy(parentComponentId,Long.valueOf(newParentComponentId))==true) {
			flash("success","Same parts assigned to selected component successfully.");
		} else {
			flash("error","Error! Part assignment to selected component failed!");
		}
		return redirect(
				routes.PartComponentApplication.list(
						parentComponentId, 0, "part.name", "asc", ""));
	}

}