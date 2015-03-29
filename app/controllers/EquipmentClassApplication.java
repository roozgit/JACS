package controllers;

import static play.data.Form.form;

import java.util.HashMap;
import java.util.Map;

import models.equipment.EquipmentClass;
import models.equipment.Equipments;
import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;

@SubjectPresent
public class EquipmentClassApplication extends Controller {
	
	/**
     * This result directly redirect to equipmentClass list home.
     */
    public static final Result GO_HOME = redirect(
        routes.EquipmentClassApplication.list(0, "name", "asc", "")
    );
    
/**
 * Information about one equipmentClass
 * @param id Id of the equipmentClass
 * @return Result of render(EquipmentClass.find.byId(id))
 */
	public static Result equipmentClassIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");

		return ok(views.html.equipments.equipmentClassIndex.render(EquipmentClass.find
				.byId(id),
				Equipments.find.where().eq("equipmentClass.id", id).findList()
				));
	}

	/**
	 * Returns a list of 25 equipmentclass for use in equipmentClassList or filtered queries
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
				views.html.equipments.equipmentClassList.render(
						EquipmentClass.page(page, ppid, sortBy , order, filter),
						sortBy, order, filter
						));
	}
	
/**
 * Lists a page of equipmentclass ordered by name and ascending
 * @return
 */
	public static Result equipmentClassList() {
		return GO_HOME;

	}

	/**
	 * Displays equipmentClass creates form
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result createEquipmentClass() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<EquipmentClass> newEquipmentClassForm = form(EquipmentClass.class);
		return ok(views.html.equipments.createEquipmentClassForm.render(newEquipmentClassForm));
	}

	/**
	 * Saves a equipmentClass
	 * @return GO_HOME
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result save() {
		Form<EquipmentClass> newEquipmentClassForm = form(EquipmentClass.class)
				.bindFromRequest();
		if (newEquipmentClassForm.hasErrors()) {
			return badRequest(views.html.equipments.createEquipmentClassForm.render(newEquipmentClassForm));
		}
		Ebean.beginTransaction();
		try {
			newEquipmentClassForm.get().save();
			flash("success", "EquipmentClass " + newEquipmentClassForm.get().name + " has been created");
		Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("EquipmentClass save error",e.fillInStackTrace());
			flash("error", "Can't save equipment class");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return GO_HOME;
	}

	/**
	 * display edit equipmentClass form
	 * @param id Id of equipmentClass
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result editEquipmentClass(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<EquipmentClass> equipmentClassForm = form(EquipmentClass.class).fill(
				EquipmentClass.find.byId(id));
		return ok(views.html.equipments.editEquipmentClass.render(id, equipmentClassForm));
	}

	/**
	 * Updates equipmentClass
	 * @param id Id of equipmentClass
	 * @return GO_HOME
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result update(Long id) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (EquipmentClass.class.getField(key).getType().toString()
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
		Form<EquipmentClass> updateForm = new Form<EquipmentClass>(EquipmentClass.class)
				.bind(newData);

		if (updateForm.hasErrors()) {
			return badRequest(views.html.equipments.editEquipmentClass.render(id,updateForm));
		}
		Ebean.beginTransaction();
				try {
					EquipmentClass ec = updateForm.get();
					ec.id = id;
					ec.saveManyToManyAssociations("possibleFailureModes");
					ec.update(id);
					flash("success", "EquipmentClass " + updateForm.get().name + " has been updated");
					Ebean.commitTransaction();
				} catch (Exception e) {
					Logger.error("EquipmentClass update error",e.fillInStackTrace());
					flash("error", "Can't update equipment class");
					Ebean.rollbackTransaction();
				} finally {
					Ebean.endTransaction();
				}
		
		return redirect(routes.EquipmentClassApplication.equipmentClassIndex(id));
	}
}
