package controllers;

import static play.data.Form.form;

import java.util.List;

import models.InventoryEvents;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;
//import views.html.*;

@SubjectPresent
public class InventoryEventApplication extends Controller {

		
	/**
	 * Returns a list of 10 disciplines for use in disciplineList or filtered queries
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
	@Restrict({ @Group("admin")})
	public static Result list() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		List<InventoryEvents> ieList = InventoryEvents.find.where().gt("id", 1L)
				.orderBy("id").findList();
		return ok(
				views.html.inventoryEvents.inventoryEventList.render(ieList));
	}
		
	@Restrict({ @Group("admin")})
	public static Result createInventoryEvent() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<InventoryEvents> newInventoryEventForm = form(InventoryEvents.class);
		return ok(views.html.inventoryEvents.createInventoryEventForm.render(newInventoryEventForm));

	}
	
	@Restrict({ @Group("admin")})
	public static Result save() {
		Form<InventoryEvents> newInventoryEventForm = form(InventoryEvents.class).bindFromRequest();
		if (newInventoryEventForm.hasErrors()) {
			return badRequest(views.html.inventoryEvents.createInventoryEventForm.render(
					newInventoryEventForm));
		}
		InventoryEvents d = newInventoryEventForm.get();
		try {
			d.save();
		} catch (Exception ex) {
			return badRequest(views.html.inventoryEvents.createInventoryEventForm.render(
					newInventoryEventForm));
		}
		return redirect(routes.InventoryEventApplication.list());
	}
	
	@Restrict({ @Group("admin")})
	public static Result editInventoryEvent(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<InventoryEvents> inventoryEventForm = form(InventoryEvents.class).fill(InventoryEvents.find.byId(id));
		return ok(views.html.inventoryEvents.editInventoryEvent.render(id, inventoryEventForm));
	}
	
	@Restrict({ @Group("admin")})
	public static Result update(Long id) {
		Form<InventoryEvents> inventoryEventForm = form(InventoryEvents.class)
				.bindFromRequest();
		if (inventoryEventForm.hasErrors() || id<2) {
			return badRequest(views.html.inventoryEvents.editInventoryEvent.render(
					id, inventoryEventForm));
		}
		inventoryEventForm.get().update(id);
		return redirect(routes.InventoryEventApplication.list());
	}
	
	
}
