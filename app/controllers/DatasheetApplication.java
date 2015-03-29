package controllers;


import static play.data.Form.form;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.Components;
import models.Datasheet;
import models.History;
import models.Installations;
import models.MeasurementUnits;
import models.Parts;
import models.Plants;
import models.Sections;
import models.Subunits;
import models.equipment.Equipments;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

@SubjectPresent
public class DatasheetApplication extends Controller {
	
/**
 * Lists all datasheet of an item
 * @param parentLevel id of level for which we are creating hisotry.
 * @param parentId
 * @return
 */
	public static Result list(Integer parentLevel, Long parentId, int page, String sortBy,
			String order, String filter) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		String fieldName = new String();
		switch(parentLevel) {
		case 3:
			fieldName = "parentInstallation.id";
			break;
		case 4:
			fieldName = "parentPlant.id";
			break;
		case 5:
			fieldName = "parentSection.id";
			break;
		case 6:
			fieldName = "parentEquipment.id";
			break;
		case 7:
			fieldName = "parentSubunit.id";
			break;
		case 8:
			fieldName = "parentComponent.id";
			break;
		case 9:
			fieldName = "parentPart.id";
			break;
		default:
			fieldName = "error";
			break;
				
		}
		int ppid = 50;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 50;
			}
		}
		
		if(fieldName=="error")
			return badRequest("No such level"); else
		return ok(views.html.datasheet.datasheetList.render(
				parentLevel,
				parentId,
				Datasheet.page(fieldName, parentId, page, ppid, sortBy, order,
						filter)
				, sortBy, order, filter
				));
	}
	
	
	
	/**
	 * Display create datasheet form
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("planner")})
	public static Result createDatasheet(Integer parentLevel, Long parentId, Long historyId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		if(historyId<=-1000L) {
			flash("error","No history found");
			return redirect(session().get("refererPage"));
		}
		Form<Datasheet> newDatasheetForm = form(Datasheet.class);
		return ok(views.html.datasheet.datasheetCreateForm.render(
					parentLevel, parentId, historyId, newDatasheetForm
					));

	}
	
	/**
	 * save new datasheet
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("planner")})
	public static Result save(Integer parentLevel, Long parentId, Long historyId) {

			Form<Datasheet> newDatasheetForm = form(Datasheet.class).bindFromRequest();
			if (newDatasheetForm.hasErrors()) {
				return badRequest(views.html.datasheet.datasheetCreateForm.render(
						parentLevel, parentId, historyId, newDatasheetForm));
			}
			Datasheet his = newDatasheetForm.get();
			switch(parentLevel) {
			case 3:
				his.parentInstallation = Installations.find.byId(parentId);
				if(his.parentInstallation==null) return badRequest("NOTHING");
				break;				
			case 4:
				his.parentPlant = Plants.find.byId(parentId);
				if(his.parentPlant==null) return badRequest("NOTHING");
				break;				
			case 5:
				his.parentSection = Sections.find.byId(parentId);
				if(his.parentSection==null) return badRequest("NOTHING");
				break;
			case 6:
				his.parentEquipment = Equipments.find.byId(parentId);
				if(his.parentEquipment==null) return badRequest("NOTHING");
				break;
				
			case 7:
				his.parentSubunit = Subunits.find.byId(parentId);
				if(his.parentSubunit==null) return badRequest("NOTHING");
				break;
			case 8:
				his.parentComponent = Components.find.byId(parentId);
				if(his.parentComponent==null) return badRequest("NOTHING");
				break;
			case 9:
				his.parentPart = Parts.find.byId(parentId);
				if(his.parentPart==null)return badRequest("NOTHING");
				break;
			}
			if(historyId > 0) his.parentHistory = History.find.byId(historyId); else his.parentHistory=null;
			his.save();
			flash("success","Data added");
			if(historyId>0) return redirect(session().get("refererPage")+"#tabs-maintainedItems");
			return redirect(routes.DatasheetApplication.list(parentLevel, parentId,0,"parameter","asc",""));
		 //dummy return: should never happen
	}
	
	
	/**
	 * update datasheet
	 * @param datasheetId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("editor"), @Group("planner") })
	public static Result update() {
			DynamicForm datasheetFieldForm = form().bindFromRequest();
			
			Datasheet his = Datasheet.find.byId(Long.valueOf(datasheetFieldForm.get("pk")));
			String fieldName = datasheetFieldForm.get("name");
			String updatedValue = datasheetFieldForm.get("value");
			if(updatedValue.length()==0) updatedValue =null;
			switch(fieldName) {
			case "parameter":
				his.parameter = updatedValue;
				break;
			case "value":
				his.value = updatedValue;
				break;
			case "minValue":
				his.minValue=updatedValue;
				break;
			case "maxValue":
				his.maxValue=updatedValue;
				break;
			case "unit.id":
				Long unitId=-1L;
				try {
				unitId = Long.valueOf(updatedValue);
				} catch (Exception x) {
					unitId=-1L;
				}
				if(unitId==-1L) his.unit=null; else his.unit= MeasurementUnits.find.byId(unitId);
			}
			try {
				if(his.parameter==null) throw new Exception();
			his.update();
			} catch (Exception x) {
				return internalServerError("Can't update field");
			}
			
			return ok("Update successfule");
			
		//return redirect(routes.DatasheetApplication.list(parentLevel, parentId,0,"parameter","asc",""));
	}
	
	/**
	 * update datasheet
	 * @param datasheetId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor"), @Group("planner") })
	public static Result deleteItem(Long id) {
		try {
			Datasheet.find.ref(id).delete();
		} catch (Exception e) {
			Logger.error("Error in datasheet delete",e.fillInStackTrace());
			return internalServerError("An error happened");
		}
		return ok("deleted");
	}
	
	/**
	 * 
	 * @param historyId
	 * @return
	 */
	public static Result maintParameters(Long historyId) {
    	ArrayList<ObjectNode> histParams = new ArrayList<ObjectNode>();
    	if(historyId<0) return ok("no paramateres");
    	
    	for(Datasheet i : Datasheet.find.where().eq("parentHistory.id",historyId).findList()) {
    		ObjectNode param = Json.newObject();
    		param.put("section",""); param.put("equipment",""); param.put("subunit",""); param.put("component","");
    		param.put("parameter",i.parameter);
    		if(i.parentSection!=null) {param.put("section",i.parentSection.name); param.put("compare", compare(i.value,"parentSection.id",i.parentSection.id,i.parameter));}
    		if(i.parentEquipment!=null) {param.put("equipment",i.parentEquipment.name); param.put("compare", compare(i.value,"parentEquipment.id",i.parentEquipment.id,i.parameter));}
    		if(i.parentSubunit!=null) {param.put("subunit",i.parentSubunit.name); param.put("compare", compare(i.value,"parentSubunit.id",i.parentSubunit.id,i.parameter));}
    		if(i.parentComponent!=null) {param.put("component",i.parentComponent.name); param.put("compare", compare(i.value,"parentComponent.id",i.parentComponent.id,i.parameter));}
    		if(i.value!=null) param.put("value",i.value); else param.put("value", "NA");
    		if(i.unit!=null) param.put("measurementUnit",i.unit.unitName + "("+i.unit.unitCode+")"); else param.put("measurementUnit","---");
    		param.put("did",i.id);
    		
    		histParams.add(param);
    	}
    	return ok(play.libs.Json.toJson(histParams));
	}
	
	private static String compare(String value, String field, Long id, String parameter) {
		Float minx, maxx,fvalue;
		try {
			Datasheet referenceDatasheet = Datasheet.find.where().isNull("parentHistory").eq(field, id).eq("parameter",parameter).findUnique();
			minx = Float.valueOf(referenceDatasheet.minValue);
			maxx = Float.valueOf(referenceDatasheet.maxValue);
			fvalue = Float.valueOf(value);
			if(fvalue.compareTo(minx) < 0) return "LAL";
			if(fvalue.compareTo(maxx) > 0) return "HAL";
		} catch (Exception except) {
			return "NA";
		}
		return "OK";
	}
}
