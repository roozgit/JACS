package controllers;

import static play.data.Form.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import models.EventTypes;
import models.History;
import models.OperationStates;
import models.Plants;
import models.Sections;
import models.Users;
import models.equipment.EquipmentClass;
import models.equipment.Equipments;
import models.maintenance.MaintenanceCategories;
import models.maintenance.MaintenanceGroups;
import models.maintenance.Maintenances;
import models.maintenance.PreventiveMaintenances;
import myUtils.DateTimeUtils;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SubjectPresent
public class PreventiveMaintenanceApplication extends Controller {
	
	/**
	 * shows ONE pm item
	 * @param pmRoutineId
	 * @return
	 */
	public static Result pmIndex(Long pmId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		
		return ok(views.html.maintenances.pmIndex.render(
				PreventiveMaintenances.find.byId(pmId)
				));
				
	}
	
/**
 * Lists all pm routines of an item
 * @param parentLevel id of level for which we are creating pm.
 * @param parentId
 * @return
 */
	public static Result list(Integer parentLevel, Long parentId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		String fieldName = new String();
		switch(parentLevel) {
		case -1:
			fieldName="pmClass.id";
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
		}
		List<PreventiveMaintenances> classLevelList = new ArrayList<PreventiveMaintenances>();
		
		List<PreventiveMaintenances> itemList =
				PreventiveMaintenances.find.where()
				.eq(fieldName, parentId)
				.findList();
		if(fieldName=="parentEquipment.id") {
			Equipments ce = Equipments.find.byId(parentId);
			classLevelList =
					PreventiveMaintenances.find.where()
					.eq("pmClass.id", ce.equipmentClass.id)
					.findList();
		}
		
		
		return ok(views.html.maintenances.pmList.render(
				parentLevel,
				parentId,
				classLevelList,
				itemList
				));
	}
	
	
	
	/**
	 * Display create history form
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result createPM(Integer parentLevel, Long parentId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<PreventiveMaintenances> newPMForm = form(PreventiveMaintenances.class);
		return ok(views.html.maintenances.createPMForm.render(
					parentLevel, parentId, newPMForm
					));

	}
	
	/**
	 * save new history
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result save(Integer parentLevel, Long parentId) {
			Form<PreventiveMaintenances> newPMForm = form(PreventiveMaintenances.class).bindFromRequest();
			if (newPMForm.hasErrors()) {
				return badRequest(views.html.maintenances.createPMForm.render(
						parentLevel, parentId, newPMForm));
			}
			PreventiveMaintenances pm = newPMForm.get();
			switch(parentLevel) {
			case -1:
				pm.pmClass = EquipmentClass.find.byId(parentId);
				pm.save();
				flash("success","Class level PM added");
				return redirect(routes.EquipmentClassApplication.equipmentClassIndex(parentId));
			case 4:
				pm.parentPlant = Plants.find.byId(parentId);
				pm.save();
				flash("success","PM added");
				return redirect(routes.PreventiveMaintenanceApplication.list(parentLevel, parentId));
				
			case 5:
				pm.parentSection = Sections.find.byId(parentId);
				pm.save();
				flash("success","PM added");
				return redirect(routes.PreventiveMaintenanceApplication.list(parentLevel, parentId));
				
			case 6:
				pm.parentEquipment = Equipments.find.byId(parentId);
				pm.save();
				flash("success","PM added");
				return redirect(routes.PreventiveMaintenanceApplication.list(parentLevel, parentId));
			}
		return ok("NOTHING"); //dummy return: should never happen
	}

	
	/**
	 * Display edit history form
	 * @param historyId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result editPM(Long pmId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
			Form<PreventiveMaintenances> editPMForm = form(PreventiveMaintenances.class).
					fill(PreventiveMaintenances.find.byId(pmId));
			return ok(views.html.maintenances.pmEdit.render(
					pmId, editPMForm
					));

	}
	
	/**
	 * update history
	 * @param historyId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result update(Long pmId) {

			Form<PreventiveMaintenances> editPM = form(PreventiveMaintenances.class).bindFromRequest();
			
			if (editPM.hasErrors()) {
				return badRequest(views.html.maintenances.pmEdit.render(
						pmId, editPM));
			}
			PreventiveMaintenances pm = editPM.get();
			pm.update(pmId);
			flash("success","PM updated");
		return redirect(routes.PreventiveMaintenanceApplication.pmIndex(pmId));
	}
	
	/**
	 * 
	 * @param pmClassId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result planPmParams(String level, Long pmParentId, Long pmRoutineId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Float intOpH = PreventiveMaintenances.find.byId(pmRoutineId).intervalOperationHours;
		if(intOpH!=null && (Float.compare(intOpH, 0F)!=0))
			return badRequest("This PM must be planned automatically!");
		return ok(views.html.maintenances.planPmParamsForm.render(level, pmParentId,pmRoutineId));
	}
	
	/**
	 * 
	 * @param pmClassId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result calculatePmPlanJson(String level, Long pmParentId, Long pmRoutineId,String startDate, String endDate, String pmsPerDay, String maintLength) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY/MM/dd");
		DateTimeFormatter jsonFormatter = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm");
		DateTime start,end;
		try {
			start  = formatter.parseDateTime(DateTimeUtils.getGregorianDateTimeAsString(startDate));
			end= formatter.parseDateTime(DateTimeUtils.getGregorianDateTimeAsString(endDate)).plusDays(1);
		} catch (IllegalArgumentException e) {
			start = new DateTime();
			end = new DateTime();
		}

		Integer numOfPmsPerDay = 0;
		Integer rlength = 1;
		try {
		  numOfPmsPerDay = Integer.valueOf(pmsPerDay);
		  rlength = Integer.valueOf(maintLength);
		} catch (Exception except){
		  numOfPmsPerDay = 0;
		  rlength=1;
		}
		
		int totalEquipmentsToBePlanned =0;
		List<Equipments> allToBePlanned = new ArrayList<Equipments>();
		switch(level) {
		case "equipmentClass":
			totalEquipmentsToBePlanned = Equipments.find.where().eq("equipmentClass.id",pmParentId).findRowCount();
			allToBePlanned = Equipments.find.where().eq("equipmentClass.id",pmParentId).orderBy("section.id").findList();
			break;
		case "equipment":
			totalEquipmentsToBePlanned = 1;
			allToBePlanned = Equipments.find.where().eq("id",pmParentId).findList();
			break;
		case "section":
			totalEquipmentsToBePlanned = Equipments.find.where().eq("section.id", pmParentId).findRowCount();
			allToBePlanned = Equipments.find.where().eq("section.id", pmParentId).findList();
			break;
		case "plant":
			totalEquipmentsToBePlanned = Equipments.find.where().eq("section.plant.id", pmParentId).findRowCount();
			allToBePlanned = Equipments.find.where().eq("section.plant.id", pmParentId).findList();
			break;
		default:
			totalEquipmentsToBePlanned = 0;
		}

		if (totalEquipmentsToBePlanned==0) return ok("Nothing to plan for");
		
		Integer duration = Days.daysBetween(start.toLocalDate(), end.toLocalDate()).getDays();
		if(duration<=0) return badRequest("End date can't be before start date!");
		Float fduration = Float.valueOf(duration);
		
		if(fduration.compareTo(PreventiveMaintenances.find.byId(pmRoutineId).intervalDays) > 0) return badRequest("Time between PMs is too short"); 
		int dailyPmQuota = (int) Math.ceil((float)totalEquipmentsToBePlanned / (float) duration);
		if ( dailyPmQuota > numOfPmsPerDay) return ok("PMs per day too low");
		
		
		Map<Integer, List<Equipments>> plan = new TreeMap<Integer, List<Equipments>>();
		
		Integer day = 1;
		int index=0;
		while(day <=duration) {
				int todayLength = (index+numOfPmsPerDay>allToBePlanned.size()) ? allToBePlanned.size() : index+numOfPmsPerDay;
				if(index>=todayLength) break;
				plan.put(day, allToBePlanned.subList(index, todayLength));
				index += numOfPmsPerDay;
				day++;
		}
		
		//Convert generated map to JSON and send to calendar
        ArrayList<ObjectNode> allPms = new ArrayList<ObjectNode>();

        for (Integer key : plan.keySet()) {
        	for(Equipments e : plan.get(key)) {
        		ObjectNode node = Json.newObject();
        		node.put("id", e.id);
        		node.put("title",e.name);
        		node.put("start",jsonFormatter.print(start.plusDays(key-1).plusHours(8)));
        		node.put("end",jsonFormatter.print(start.plusDays(key-1).plusHours(8+rlength)));
        		node.put("allDay",false);
        		allPms.add(node);
        	}
            
        }

		return ok(play.libs.Json.toJson(allPms));
	}
	
	/**
	 * 
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result savePlannedMaints(Long pmRoutineId, String periods, Boolean isProject, String hiddenStart, String hiddenStop) {
		ObjectNode result = Json.newObject();
		DateTimeFormatter jsonFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		DateTimeFormatter simpleFormatter = DateTimeFormat.forPattern("yyyy/MM/dd");
		MaintenanceGroups mapa = new MaintenanceGroups();
		Integer pmPeriods =0;
		try {
		  pmPeriods = Integer.valueOf(periods);
		} catch (Exception e){
			result.put("message",e.getMessage());
		   return badRequest(result);
		}
		if(pmPeriods<=0) {
			result.put("message","Number of periods invalid");
			return badRequest(result);
		}
		JsonNode json = request().body().asJson();
		if(json.size()==0) {
			result.put("message","No events");
			badRequest(result);
		}
		DateTime originalStartDate;
		DateTime originalEndDate;
		try {
			originalStartDate = simpleFormatter.parseDateTime(DateTimeUtils.getGregorianDateTimeAsString(hiddenStart));
			originalEndDate = simpleFormatter.parseDateTime(DateTimeUtils.getGregorianDateTimeAsString(hiddenStop)).plusDays(1);
		} catch (Exception x) {
			result.put("message","Invalid dates");
			return badRequest(result);
		}
		
		PreventiveMaintenances thisPmRoutine = PreventiveMaintenances.find.byId(pmRoutineId);
		Ebean.beginTransaction();
		try {
			if(isProject==true) {
				mapa.name=thisPmRoutine.name+"--P: "+periods+" periods"+"--by: "+Users.findByUserName(session().get("userName")).userName;
				mapa.percentComplete=0f;
				mapa.save();
			}
			for(Integer k=0;k<pmPeriods;k++) {
				for(int i=0;i<json.size();i++) {
					Long equId = json.get(i).get("id").asLong();
					Maintenances newMa = new Maintenances();
					Equipments se = Equipments.find.byId(equId);
					newMa.maintainedEquipment = se;
					newMa.maintainedSection = se.section;
					newMa.maintenanceCategory = MaintenanceCategories.PREVENTIVE_MAINTENANCE;
					newMa.maintenancePriority=0;
					newMa.classLevelPMRoutine = PreventiveMaintenances.find.byId(pmRoutineId);
					newMa.requestDate = new DateTime(jsonFormatter.parseDateTime(json.get(i).get("start").asText())).plusDays(k*Math.round((Float.valueOf(thisPmRoutine.intervalDays.toString())))).toDate();
					newMa.responsibleDiscipline = thisPmRoutine.actingDiscipline;
					newMa.maintenanceStatus = null;
					newMa.workOrderSerial = MaintenanceApplication.calculateWorkOrderSerial("PREVENTIVE_MAINTENANCE").toString();
					newMa.workRequestSerial = MaintenanceApplication.calculateWorkRequestSerial("PREVENTIVE_MAINTENANCE").toString();
					if(isProject==true) newMa.maintenanceGroup =mapa;
					newMa.save();
					
					History newHi = new History();					
					newHi.allDay=false;
					newHi.eventType = EventTypes.MAINTENANCE;
					newHi.isHappened=false;
					newHi.parentEquipment = se;
					newHi.maint=newMa;
					newHi.registrar=Users.findByUserName(session().get("userName"));
					newHi.state = OperationStates.RUNNING;
					newHi.systemEvent=false;
					if(k==0) {
						DateTime jsonStartDate = new DateTime(jsonFormatter.parseDateTime(json.get(i).get("start").asText()));
						DateTime jsonEndDate = new DateTime(jsonFormatter.parseDateTime(json.get(i).get("end").asText()));
						if(originalStartDate.getMillis()> jsonStartDate.getMillis() || originalEndDate.getMillis()<jsonEndDate.getMillis())
							throw new IndexOutOfBoundsException("Start or end date out of boundary");
					}
					newHi.start = new DateTime(jsonFormatter.parseDateTime(json.get(i).get("start").asText())).plusDays(k*Math.round((Float.valueOf(thisPmRoutine.intervalDays.toString())))).toDate();
					newHi.end = new DateTime(jsonFormatter.parseDateTime(json.get(i).get("end").asText())).plusDays(k*Math.round((Float.valueOf(thisPmRoutine.intervalDays.toString())))).toDate();
					newHi.save();
				}
			}
			result.put("message","Planned!");
			Ebean.commitTransaction();
		} catch (Exception exc) {
			Logger.error("Couldn't save all planned maintenances",exc.fillInStackTrace());
			result.put("message",exc.getMessage());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return ok(result);
	}
		
}
