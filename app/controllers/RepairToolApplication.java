package controllers;

import static play.data.Form.form;

import java.util.HashMap;
import java.util.Map;

import models.RepairTools;
import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;

@SubjectPresent
public class RepairToolApplication extends Controller {
	
	/**
     * This result directly redirect to repairTool list home.
     */
    public static final Result GO_HOME = redirect(
        routes.RepairToolApplication.list(0, "name", "asc", ""));
    
/**
 * Information about one repairTool
 * @param id Id of the repairTool
 * @return Result of render(RepairTools.find.byId(id))
 */
	public static Result repairToolIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");

		return ok(views.html.repairtools.repairToolIndex.render(RepairTools.find
				.byId(id)));
	}

	/**
	 * Returns a list of 25 tools for use in repairToolList or filtered queries
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
		int ppid = 20;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 20;
			}
		}
		return ok(
				views.html.repairtools.repairToolList.render(
						RepairTools.page(page, ppid, sortBy , order, filter),
						sortBy, order, filter
						));
	}
	
/**
 * Lists a page of tools ordered by name and ascending
 * @return
 */
	public static Result repairToolList() {
		return GO_HOME;
	}

	/**
	 * Displays repairTool creates form
	 * @return form display
	 */
	@Restrict({@Group("admin"),
		@Group("creator")})
	public static Result createRepairTool() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<RepairTools> newRepairToolForm = form(RepairTools.class);
		return ok(views.html.repairtools.createRepairToolForm.render(newRepairToolForm));
	}

	/**
	 * Saves a repairTool
	 * @return GO_HOME
	 */
	@Restrict({@Group("admin"),
		@Group("creator")})
	public static Result save() {
		Form<RepairTools> newRepairToolForm = form(RepairTools.class)
				.bindFromRequest();
		if (newRepairToolForm.hasErrors()) {
			return badRequest(views.html.repairtools.createRepairToolForm.render(newRepairToolForm));
		}
		Ebean.beginTransaction();
		try {
			newRepairToolForm.get().save();
			flash("success", "RepairTool " + newRepairToolForm.get().name + " has been created");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Tool save error",e.fillInStackTrace());
			flash("error", "Tool save error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return GO_HOME;
	}

	/**
	 * display edit repairTool form
	 * @param id Id of repairTool
	 * @return form display
	 */
	@Restrict({@Group("admin"),
		@Group("editor")})
	public static Result editRepairTool(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<RepairTools> repairToolForm = form(RepairTools.class).fill(
				RepairTools.find.byId(id));
		return ok(views.html.repairtools.editRepairTool.render(id, repairToolForm));
	}

	/**
	 * Updates repairTool
	 * @param id Id of repairtool
	 * @return GO_HOME
	 */
	@Restrict({@Group("admin"),
		@Group("editor")})
	public static Result update(Long id) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {
				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (RepairTools.class.getField(key).getType().toString()
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
		Form<RepairTools> updateForm = new Form<RepairTools>(RepairTools.class)
				.bind(newData);

		if (updateForm.hasErrors()) {
			return badRequest(views.html.repairtools.editRepairTool.render(id,updateForm));
		}
		Ebean.beginTransaction();
				try {
					RepairTools rt = updateForm.get();
					rt.id = id;
					rt.saveManyToManyAssociations("files");
					rt.update(id);
					flash("success", "RepairTool " + updateForm.get().name + " has been updated");
					Ebean.commitTransaction();
				} catch (Exception e) {
					Logger.error("History update error",e.fillInStackTrace());
					flash("error", "History update error");
					Ebean.rollbackTransaction();
				} finally {
					Ebean.endTransaction();
				}
		
		return redirect(routes.RepairToolApplication.repairToolIndex(id));
	}
	
}
