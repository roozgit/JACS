package controllers;

import static play.data.Form.form;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.node.ObjectNode;

import models.Dimensions;
import models.MeasurementUnits;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

@SubjectPresent
public class MeasurementUnitApplication extends Controller {
	
	/**
     * This result directly redirect to company list home.
     */
    public static final Result GO_HOME = redirect(
        routes.MeasurementUnitApplication.list(0, "unitName", "asc", ""));
	
	/**
	 * 
	 * @return
	 */
	public static Result dimensionList() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.measurementUnits.dimensionList.render(
				Dimensions.find.all()
				));
	}
	
	/**
	 * 
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
				ppid = 15;
			}
		}
		return ok(
				views.html.measurementUnits.measurementUnitList.render(
						MeasurementUnits.page(page, ppid, sortBy , order, filter),
						sortBy, order, filter
						));
	}
	
	/**
	 * 
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result createMeasurementUnitForm() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MeasurementUnits> mForm = form(MeasurementUnits.class);
		return ok(views.html.measurementUnits.createMeasurementUnitForm.render(mForm));
	}
	
	/**
	 * 
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result saveMeasurementUnit() {
		Form<MeasurementUnits> mForm = form(MeasurementUnits.class).bindFromRequest();
		if (mForm.hasErrors()) {
			return badRequest(views.html.measurementUnits.createMeasurementUnitForm.render(
					mForm));
		}
		
		mForm.get().save();
		return GO_HOME;
	}
	
	/**
	 * 
	 * @param did
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result editMeasurementUnitForm(Long did) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MeasurementUnits> mForm = form(MeasurementUnits.class).fill(MeasurementUnits.find.byId(did));;
		return ok(views.html.measurementUnits.editMeasurementUnitForm.render(
					did, mForm
				));
	}
	
	/**
	 * 
	 * @param did
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result updateMeasurementUnit(Long mid) {
		Form<MeasurementUnits> mForm = form(MeasurementUnits.class).bindFromRequest();
		if(mForm.hasErrors())
			return badRequest(views.html.measurementUnits.editMeasurementUnitForm.render(
					mid,mForm));
		
		mForm.get().update(mid);
		return GO_HOME;
	}
	
	/**
	 * 
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result createDimensionForm() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Dimensions> dForm = form(Dimensions.class);
		return ok(views.html.measurementUnits.createDimensionForm.render(dForm));
	}
	
	/**
	 * 
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result saveDimension() {
		Form<Dimensions> dForm = form(Dimensions.class).bindFromRequest();
		if (dForm.hasErrors()) {
			return badRequest(views.html.measurementUnits.createDimensionForm.render(
					dForm));
		}
		
		dForm.get().save();
		return redirect(routes.MeasurementUnitApplication.dimensionList());
	}
	
	/**
	 * 
	 * @param did
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result editDimensionForm(Long did) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Dimensions> dForm = form(Dimensions.class).fill(Dimensions.find.byId(did));;
		return ok(views.html.measurementUnits.editDimensionForm.render(
					did, dForm
				));
	}
	
	/**
	 * 
	 * @param did
	 * @return
	 */
	@Restrict({@Group("admin")})
	public static Result updateDimension(Long did) {
		Form<Dimensions> dForm = form(Dimensions.class).bindFromRequest();
		if(dForm.hasErrors())
			return badRequest(views.html.measurementUnits.editDimensionForm.render(
					did,dForm));
		
		dForm.get().update();
		return redirect(routes.MeasurementUnitApplication.dimensionList());
	}
	
	/**
	 * 
	 * @param equipmentId
	 * @return
	 */
	@BodyParser.Of(BodyParser.Json.class)
    public static Result measurementUnitOptions() {
		ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	
		for (MeasurementUnits msu : MeasurementUnits.find.all()) {
			ObjectNode node = Json.newObject();
			node.put("id", msu.id.toString());
			node.put("description", msu.dimension.name+"---"+msu.unitName+"---"+msu.unitCode);
			allnodes.add(node);
		}
		return ok(play.libs.Json.toJson(allnodes));
	}
}