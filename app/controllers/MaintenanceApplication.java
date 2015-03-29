package controllers;

import static play.data.Form.form;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ValidationException;

import org.joda.time.DateTime;

import models.Blobs;
import models.EventTypes;
import models.History;
import models.InventoryEvents;
import models.Lounge;
import models.PartHistory;
import models.Parts;
import models.Sections;
import models.SecurityRole;
import models.Users;
import models.equipment.Equipments;
import models.maintenance.HoldReasons;
import models.maintenance.MaintenanceCategories;
import models.maintenance.MaintenanceGroups;
import models.maintenance.MaintenanceStatus;
import models.maintenance.MaintenanceWorkflow;
import models.maintenance.Maintenances;
import models.maintenance.MaintenancesComponents;
import models.maintenance.MaintenancesParts;
import models.maintenance.MaintenancesSubunits;
import models.maintenance.MaintenancesUsers;
import models.maintenance.PreventiveMaintenances;
import models.maintenance.WorkflowTree;
import myUtils.DateTimeUtils;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Dynamic;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SubjectPresent
public class MaintenanceApplication extends Controller {
	
	/**
     * This result directly redirect to repairTool list home.
     */
    public static final Result GO_HOME = redirect(
        routes.MaintenanceApplication.list(0, "workOrderSerial", "asc", "", "", "", "", "", "", "","",""));
    
    /**
     * Displays properties of one Maintenance
     * @param id
     * @return
     */
	public static Result maintenanceIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Maintenances mtem = Maintenances.find.byId(id);
		if(mtem==null) {
			flash("error","Requested maintenance not found. If you created a new maintenance, you should " +
						"consult the error log or contact system administrator");
			return GO_HOME;
		}
		/*Ebean.beginTransaction();
		try {
			Ebean
	         .createUpdate(Maintenances.class, "UPDATE maintenances SET calculated_total_man_hours = :ctm, " +
	        		 "calculated_total_time_to_repair = :cttr WHERE id=:id")
	         .setParameter("ctm", Maintenances.calculateManHour(id))
	         .setParameter("cttr", Maintenances.calculateTTR(id))
	         .setParameter("id",id)
	         .execute();
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Index rendering error",e.fillInStackTrace());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}*/
		//List of prereq maints
		List<Maintenances> prereqMaints = Maintenances.find.where()
				.eq("dependentMaintenances.id",id).findList();
		//Maint Logs
		History hisForMaint = History.find.where()
				.eq("maint.id", id)
				.eq("isHappened",true)
				.findUnique();
		History hisForMaintPlanned = History.find.where()
				.eq("maint.id", id)
				.eq("isHappened",false)
				.findUnique();
		session().put("refererPage",request().uri());
		return ok(views.html.maintenances.maintenanceIndex
				.render(mtem,
						(hisForMaint==null) ? "" : myUtils.DateTimeUtils.getIranianDateTime(hisForMaint.start),
						(hisForMaint==null) ? "" : myUtils.DateTimeUtils.getIranianDateTime(hisForMaint.end),
						(hisForMaintPlanned==null) ? "" : myUtils.DateTimeUtils.getIranianDateTime(hisForMaintPlanned.start),
						(hisForMaintPlanned==null) ? "" : myUtils.DateTimeUtils.getIranianDateTime(hisForMaintPlanned.end),		
						prereqMaints
						));
	}

	private static void  updateManHourAndTtr(Long id) {
		Ebean.beginTransaction();
		try {
			Ebean
	         .createUpdate(Maintenances.class, "UPDATE maintenances SET calculated_total_man_hours = :ctm, " +
	        		 "calculated_total_time_to_repair = :cttr WHERE id=:id")
	         .setParameter("ctm", Maintenances.calculateManHour(id))
	         .setParameter("cttr", Maintenances.calculateTTR(id))
	         .setParameter("id",id)
	         .execute();
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Index rendering error",e.fillInStackTrace());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
	}
	/**
	 * Returns a list of 50 maints for use in maintenanceList or filtered queries
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
	public static Result list(int page, String sortBy, String order, String filter1, String filter2
			, String filter3, String filter4, String filter5,
			String filter6, String filter7, String filter8, String filter9) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		int ppid = 50;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 50;
			}
		}
		return ok(
				views.html.maintenances.maintenanceList.render(
						Maintenances.page(page, ppid, sortBy , order, filter1, filter2, filter3
								, filter4, filter5, filter6, filter7,filter8,filter9),
						sortBy, order, filter1, filter2, filter3,
						filter4, filter5, filter6, filter7,filter8,filter9
						));
	}
	
	/**
	 * 
	 * @param category
	 * @return
	 */
	public static Long calculateWorkOrderSerial(String category) {
		long mNum = 0;
		long serial =0;
		long constant=9300000;
		
		switch(category) {
		case "CORRECTIVE_MAINTENANCE":
			constant = 9300000;
			break;
		case "PREVENTIVE_MAINTENANCE":
			constant = 9500000;
			break;
		}
		try {
			for(Maintenances m : Maintenances.find.where()
					.isNotNull("id")
					.eq("maintenanceCategory", category)
					.contains("workOrderSerial", String.valueOf(constant).substring(0,2))
					.select("workOrderSerial").findList()) {
				
				String numStr = m.workOrderSerial.split("/")[0];
				if (Long.parseLong(numStr.substring(2)) > mNum)
					mNum = Long.parseLong(numStr.substring(2));
			}
			mNum++;
			} catch(Exception NumberFormatException) {
				Logger.error("Work order serial calc. error",NumberFormatException.fillInStackTrace());
				mNum=0;
			} finally {
				serial =mNum+constant;
				}
		return serial;
	}
	
	/**
	 * 
	 */
	public static Long calculateWorkRequestSerial(String category) {
		long mNum = 0;
		long serial =0;
		long constant=0;
		
		switch(category) {
		case "CORRECTIVE_MAINTENANCE":
			constant = 9300000;
			break;
		case "PREVENTIVE_MAINTENANCE":
			constant = 9500000;
			break;
		}
		try {
			for(Maintenances m : Maintenances.find.where()
					.isNotNull("id")
					.eq("maintenanceCategory", category)
					.contains("workOrderSerial", String.valueOf(constant).substring(0,2))
					.select("workRequestSerial").findList()) {
				
				String numStr = m.workRequestSerial.split("/")[0];
				if (Long.parseLong(numStr.substring(2)) > mNum)
					mNum = Long.parseLong(numStr.substring(2));
			}
			mNum++;
			} catch(Exception NumberFormatException) {
				Logger.error("Work request serial calc. error",NumberFormatException.fillInStackTrace());
				mNum=0;
			} finally {
				serial =mNum+constant;
				}
		return serial;
	}
	
	public static Result calculateSerials(String category) {
		ObjectNode node = Json.newObject();
		node.put("workRequestSerial", String.valueOf(calculateWorkRequestSerial(category)));
		node.put("workOrderSerial",String.valueOf(calculateWorkOrderSerial(category)));
		return ok(play.libs.Json.toJson(node));
	}
	/**
	 * Creates a maintenance form and returns it for display
	 * 
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("requester"), @Group("planner"),@Group("pm.requester")})
	public static Result createMaintenance(Long sid, Long eid) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Maintenances filler = new Maintenances();
			filler.workOrderSerial=
					String.valueOf(calculateWorkOrderSerial("CORRECTIVE_MAINTENANCE"));
			filler.workRequestSerial =
					String.valueOf(calculateWorkRequestSerial("CORRECTIVE_MAINTENANCE"));
			if(eid>0) filler.maintainedEquipment=Equipments.find.byId(eid);
			if(sid>0) filler.maintainedSection = Sections.find.byId(sid);
		Form<Maintenances> newMaintenanceForm = form(Maintenances.class).fill(filler);
		return ok(views.html.maintenances.createMaintenanceForm
				.render(sid, eid,newMaintenanceForm));
	}
	
	/**
	 * Creates a maintenance form and returns it for display
	 * 
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner"), @Group("requester"), @Group("pm.requester")})
	public static Result createAjaxMaintenance(Long sid, Long eid) {
		return createMaintenance(sid, eid);
	}

	/**
	 * Saves maintenance
	 * 
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner"), @Group("requester"), @Group("pm.requester")})
	public static Result save(Long sid, Long eid) {
		Long thisMaintId=-1L;
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (Maintenances.class.getField(key).getType()
								.toString()
								.compareTo("interface java.util.List") == 0) {
							newData.put(key + "[0]" + ".id", value[0]);
						} else {
							newData.put(key, value[0]);
						}
					} catch (NoSuchFieldException | SecurityException e) {
						newData.put(key, value[0]);
					}
					try {
						if(Maintenances.class.getField(key).getType()
								.toString()
								.compareTo("class java.util.Date") == 0) {
							newData.put(key,DateTimeUtils.getGregorianDateTimeAsString(value[0]).toString());
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
		Form<Maintenances> newMaintenanceForm = new Form<Maintenances>(
				Maintenances.class).bind(newData);
		if (newMaintenanceForm.hasErrors()) {
			return badRequest(views.html.maintenances.createMaintenanceForm
					.render(sid, eid,newMaintenanceForm));
		}
		Ebean.beginTransaction();
		try {
			Users u = Users.find.where()
					.eq("userName", session().get("userName")).findUnique();
			Maintenances m = newMaintenanceForm.get();
			if(m.maintenancePriority==null) m.maintenancePriority=0;
			
			if(!serialNoValidator(m.workOrderSerial) || !serialNoValidator(m.workRequestSerial)) {
				throw new ValidationException("Invalid work order serial number");
			}
			
			//Only planning personnel can request pm pd or modification
			if(!u.roles.contains(SecurityRole.findByName("pm.requester")) &&
					!u.roles.contains(SecurityRole.findByName("admin")) &&
					(m.maintenanceCategory!=MaintenanceCategories.CORRECTIVE_MAINTENANCE)) {
				throw new ValidationException("Only planning personnel can request this type of work");
			}
			
			//If not admin and has not sent to cartable, error!
			if(!newData.containsKey("workflowStage.id") && !u.roles.contains(SecurityRole.findByName("admin")) &&
					!u.roles.contains(SecurityRole.findByName("planner"))) {
				throw new ValidationException("Must choose maintenance recepient!");
			}
			
			//check if cartable is needed or not
			if(newData.containsKey("workflowStage.id")) {
				MaintenanceWorkflow mwf = new MaintenanceWorkflow();
				WorkflowTree thisWFS;
				//prevent user from selecting work flow related to other work types
				if(m.workflowStage!=null) {
					thisWFS = WorkflowTree.find.byId(m.workflowStage.id);
					if(m.maintenanceCategory!=thisWFS.treeCategory) {
						throw new ValidationException("Wrong workflow selected");
					}
				} else thisWFS = null;
				mwf.user = u;
				mwf.maintenance=m;
				mwf.actionDate = new Date();
				mwf.acceptReject=true;
				mwf.workflowStage = thisWFS;
				mwf.save();
				m.workflowStage = thisWFS;
			}
			if(m.requestDate!=null) {
				m.requestDate = DateTimeUtils.getGregorianDateTime(m.requestDate);
				m.planningComment = Users.findByUserName(session().get("userName")).userName.toString();
			} else {
				m.requestDate = new Date();
				m.timeToRepair=0F;
				m.maintenanceStatus=MaintenanceStatus.IN_PROGRESS;
			}
			
			//save initial planned date based on priority setting
			History planHistory = new History();
			planHistory.isHappened=false;
			planHistory.eventType=EventTypes.MAINTENANCE;
			planHistory.allDay=false;
			planHistory.maint=m;
			planHistory.registrar=Users.findByUserName(session().get("userName"));
			planHistory.systemEvent=true;
			planHistory.start=new Date(m.requestDate.getTime()+ m.maintenancePriority*24*3600*1000);
			planHistory.end = new Date(planHistory.start.getTime()+6*3600*1000);
			
			//Force section based on equipment and throw exception if neither section or equipment are present
			if(newData.get("maintainedEquipment.id").length()!=0) {
				Equipments eideq = Equipments.find.byId(Long.parseLong(newData.get("maintainedEquipment.id")));
				if(eideq==null)
					throw new ValidationException("Equipment not found!");
				m.maintainedEquipment=eideq;
				m.maintainedSection = eideq.section;
				planHistory.parentEquipment=eideq;
				Long sumTime = 0L;
				int counter=0;
				for(History hk: History.find.where()
						.eq("parentEquipment",m.maintainedEquipment)
						.eq("eventType",EventTypes.MAINTENANCE)
						.eq("maint.responsibleDiscipline",m.responsibleDiscipline)
						.eq("isHappened",true)
						.orderBy("start desc")
						.findPagingList(10)
						.getAsList()) {
					sumTime += hk.end.getTime()-hk.start.getTime();
					counter++;
				}
				if(counter>0) planHistory.end = new Date(planHistory.start.getTime()+sumTime/counter);
			} else if(newData.get("maintainedSection.id").length()!=0) {
				planHistory.parentSection=m.maintainedSection;
				Long sumTime = 0L;
				int counter=0;
				for(History hk: History.find.where()
						.eq("parentSection",m.maintainedSection)
						.eq("maint.responsibleDiscipline",m.responsibleDiscipline)
						.eq("eventType",EventTypes.MAINTENANCE)
						.eq("isHappened",true)
						.orderBy("start desc")
						.findPagingList(10)
						.getAsList()) {
					sumTime += hk.end.getTime()-hk.start.getTime();
					counter++;
				}
				if(counter>0) planHistory.end = new Date(planHistory.start.getTime()+sumTime/counter);
			} else throw new ValidationException("Nothing to maintain!");
			//save planHistory start and end.for `end` find last 10 work orders and get their mean for estimated end time
			
			planHistory.save();
			
			//W.O rule checks
			if(m.maintenanceStatus==null) m.maintenanceStatus = MaintenanceStatus.IN_PROGRESS;
			switch(m.maintenanceStatus.toString().toLowerCase()) {
			case "finished":
			case "in_progress":
					if(newData.containsKey("holdReason") && newData.containsKey("holdReasonComment")) {
						if(newData.get("holdReason").length()>0)
							throw new ValidationException("HOLD_REASON detected!");
						if(m.holdReasonComment.length()>0 &&
								!m.maintenanceStatus.equals(MaintenanceStatus.HOLD))
							throw new ValidationException("Hold comment added. Please set status to HOLD");
					
					}
			break;
			
			}
			if(m.maintenanceStatus.equals(MaintenanceStatus.HOLD) &&
					m.holdReason==null)
				throw new ValidationException("Can't hold without reason");
			
			//Can't choose both specific routine and class level routine for PM
			if(m.maintenanceCategory.equals(MaintenanceCategories.PREVENTIVE_MAINTENANCE)) {
				if(newData.containsKey("pmRoutine.id") && newData.containsKey("classLevelPMRoutine.id")) 
					if(!newData.get("pmRoutine.id").isEmpty() && !newData.get("classLevelPMRoutine.id").isEmpty())					
						throw new ValidationException("Only one kind of routine can be chosen!");
			}
			//End of rule check
			
			//save failures
			m.save();
			thisMaintId = m.id;
			Maintenances lastUp = Maintenances.find.byId(thisMaintId);
			lastUp.saveManyToManyAssociations("correspondingFailure");
			if(lastUp.maintainedEquipment!=null) {
				lastUp.maintainedSection = Sections.find.byId(Equipments.find.byId(lastUp.maintainedEquipment.id).section.id);
				lastUp.update();
			}
			
			flash("success", "Maintenace request created");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Error in creating maintenance",e.fillInStackTrace());
			flash("error", e.getMessage());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		updateManHourAndTtr(thisMaintId);
		return redirect(
				routes.MaintenanceApplication.maintenanceIndex(thisMaintId));
	}
	
	/**
	 * 
	 * @param serial
	 * @return
	 */
	private static boolean serialNoValidator(String serial) {
		return serial.matches("\\d+(/\\d+)?");
	}

	/**
	 * Displays edit maintenance form
	 * 
	 * @param id
	 * @return
	 */
	@Dynamic(value = "maintenancePrivilege")
	public static Result editMaintenance(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Long eid = -1L;
		Long seid = -1L;
		Maintenances filler = Maintenances.find.byId(id);
		if(filler.maintainedEquipment!=null) eid = filler.maintainedEquipment.id;
		seid = filler.maintainedSection.id;
		filler.requestDate= DateTimeUtils.getIranianDateAsDate(filler.requestDate);
		Form<Maintenances> maintenanceForm = form(Maintenances.class).fill(filler);

		return ok(views.html.maintenances.editMaintenance.render(seid,eid, id,maintenanceForm));
	}

	/**
	 * Updates current maintenance
	 * 
	 * @param id
	 * @return
	 */
	@Dynamic(value = "maintenancePrivilege")
	public static Result update(Long eid, Long id) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (Maintenances.class.getField(key).getType()
								.toString()
								.compareTo("interface java.util.List") == 0) {
							newData.put(key + "[0]" + ".id", value[0]);
						} else {
							newData.put(key, value[0]);
						}
					} catch (NoSuchFieldException | SecurityException e) {
						newData.put(key, value[0]);
					}
					try {
						if(Maintenances.class.getField(key).getType()
								.toString()
								.compareTo("class java.util.Date") == 0) {
							newData.put(key,DateTimeUtils.getGregorianDateTimeAsString(value[0]).toString());
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
		Form<Maintenances> updateForm = new Form<Maintenances>(
				Maintenances.class).bind(newData);
		if (updateForm.hasErrors()) {
			Long seid = -1L;
			if(eid!=-1L) seid = Equipments.find.byId(eid).section.id;
			return badRequest(views.html.maintenances.editMaintenance.render(
					seid,eid, id, updateForm));
		}
		Ebean.beginTransaction();
		try {
			Maintenances m = updateForm.get();
			Maintenances oldma = Maintenances.find.byId(id);
			switch (m.maintenanceCategory.toString()) {
			case "CORRECTIVE_MAINTENANCE":
			case "PREVENTIVE_MAINTENANCE":
			case "PREDICTIVE_MAINTENANCE":
			case "PRESERVATION":
			case "MODIFICATION":
			case "OTHER":
			break;

			default:
				throw new ValidationException("No such category");
			}
			
			//User can't update request fields, if he has inCharge or Verifier roles
			if(oldma.workflowStage!=null)
				if(oldma.workflowStage.receivingRole!=null)
					if(oldma.workflowStage.receivingRole.getName().toLowerCase().contains("incharge") ||
						oldma.workflowStage.receivingRole.getName().toLowerCase().contains("verifier") ||
							(oldma.workflowStage.receivingRole.getName().toLowerCase().contains("authorizer") && 
							!oldma.workflowStage.receivingRole.getName().toLowerCase().equals("maintenance.authorizer"))
							) {
						if(m.maintainedSection.id != oldma.maintainedSection.id)
								throw new ValidationException("Can't change section at this stage");
						
						if(newData.containsKey("maintainedEquipment.id"))
							if(oldma.maintainedEquipment!=null) {
								if(newData.get("maintainedEquipment.id").length()!=0) {
									if(Long.valueOf(newData.get("maintainedEquipment.id"))
										!= oldma.maintainedEquipment.id)
										throw new ValidationException("Can't change equipment at this stage");
							}
						} else {
							if(newData.get("maintainedEquipment.id").length()!=0)
								throw new ValidationException("Can't change equipment at this stage");
						}
						
						if(newData.containsKey("responsibleDiscipline.id"))
						if(m.responsibleDiscipline.id != oldma.responsibleDiscipline.id)
							throw new ValidationException("Can't change discipline at this stage");
						
						if(newData.containsKey("workOrderDescription"))
						if(!m.workOrderDescription.equals(oldma.workOrderDescription))
							throw new ValidationException("Can't change description at this stage");
						
						if(newData.containsKey("maintenancePriority"))
						if(m.maintenancePriority != oldma.maintenancePriority)
							throw new ValidationException("Can't change priority at this stage");
					
				}
			//End of verifier and incharge limitations check
			
			/*/Can't change person in charge if not authorizer or from another discipline or admin----disabled for now
			if(newData.containsKey("responsiblePerson.id")) 
				if(!oldma.responsiblePerson.equals(m.responsiblePerson)) {
					Long resPersonId = Long.valueOf(newData.get("responsiblePerson.id"));
					Users curu = Users.findByUserName(session().get("userName").toString());
					if(! (curu.roles.contains(SecurityRole.findByName("admin")) || curu.discipline.id.equals(Users.find.byId(resPersonId)))) {
						throw new ValidationException("Only someone from same discipline can appoint in Charge person");
					}
				}*/
			
			//Can't change maintenance category from CM to other types
			if(!m.maintenanceCategory.equals(oldma.maintenanceCategory))
				throw new ValidationException("Can't change maintenance category");
			//Can't choose both specific routine and class level routine for PM
			if(m.maintenanceCategory.equals(MaintenanceCategories.PREVENTIVE_MAINTENANCE)) {
				if(newData.get("pmRoutine.id").length()>0 && newData.get("classLevelPMRoutine.id").length()>0)
					throw new ValidationException("Can't assign two types of routines to maintenace");
			}
			//Dependent maintenances logic and other rules!
				switch(m.maintenanceStatus.toString().toLowerCase()) {
				case "in_progress":
				case "finished":
					for(Maintenances checkMa : Maintenances.find.where()
							.eq("dependentMaintenances.id",id)
							.findList())
						if(!checkMa.maintenanceStatus.equals(MaintenanceStatus.FINISHED))
							throw new ValidationException("Prerequisite maintenance not finished!");
					if(newData.get("holdReason").length()>0)
						throw new ValidationException("HOLD_REASON detected!");
					break;
				}
			//Can't give hold reason without holding W.O
			if(m.holdReasonComment.length()>0 && !m.maintenanceStatus.equals(MaintenanceStatus.HOLD)) {
				throw new ValidationException("Hold comment added. Please set status to HOLD");
			}
			//Can't hold without a reason
			if(m.maintenanceStatus.equals(MaintenanceStatus.HOLD) &&
					newData.get("holdReason").length()==0) {
						throw new ValidationException("Can't hold without reason!");
					}
			//Check for null state or invalid state before update
			if(!newData.containsKey("maintenanceStatus")) throw new ValidationException("Maintenance without status is invalid!");
			else {
				String mStatus = newData.get("maintenanceStatus");
				boolean testValidStatus = false;
				for(MaintenanceStatus ms : MaintenanceStatus.values()) {
					if(mStatus.equals(ms.toString())) {
						testValidStatus = true;
						break;
					}
				}
				if(testValidStatus==false) throw new ValidationException("Invalid maintenance status!");
			}
			//Change state logic. Each change of maintenance status shall be logged
			//in planning comment field
			if(oldma.maintenanceStatus!=null) {
				if(!m.maintenanceStatus.equals(oldma.maintenanceStatus)) {
					m.planningComment = oldma.planningComment + "\r\n" +
						oldma.maintenanceStatus.toString() + "--->" + m.maintenanceStatus.toString() +
						"----" + myUtils.DateTimeUtils.getIranianDateTime(new Date()) + "---" +
						Users.findByUserName(session().get("userName")).userName.toString();
				}
			} else m.planningComment="Maintenance started ---" + myUtils.DateTimeUtils.getIranianDateTime(new Date()) + "---" +
					Users.findByUserName(session().get("userName")).userName.toString();
			//End of logic
			
			//Equipment or section?
			if(m.maintainedEquipment==null && m.maintainedSection==null &&
					oldma.maintainedEquipment==null && oldma.maintainedSection==null)
				throw new ValidationException("Nothing to maintain!");
			
			//Check if maintenance is logged, in that case, maintained item can not be changed
			
			if(oldma.maintainedEquipment!=null) {
				if((History.find.where()
						.eq("maint.id",oldma.id)
						.findRowCount()!=0)) {
					if(newData.containsKey("maintainedEquipment.id") && newData.get("maintainedEquipment.id").isEmpty())
						throw new ValidationException("Please delete log before this operation");
					
					if(newData.containsKey("maintainedEquipment.id") && newData.get("maintainedEquipment.id").length()!=0) {
						if(!oldma.maintainedEquipment.id.equals(Long.valueOf(newData.get("maintainedEquipment.id")))) 
							throw new ValidationException("Please delete log before this operation");
					}
				}
			} else if(oldma.maintainedSection!=null) {
				if((History.find.where()
						.eq("maint.id",oldma.id)
						.findRowCount()!=0)) {
					if(newData.containsKey("maintainedEquipment.id") && newData.get("maintainedEquipment.id").length()!=0) throw new ValidationException("Please delete log before this operation");
					if(newData.containsKey("maintainedSection.id")  && newData.get("maintainedSection.id").length()!=0) {
						if(!oldma.maintainedSection.id.equals(Long.valueOf(newData.get("maintainedSection.id")))) 
							throw new ValidationException("Please delete log before this operation");
					}
				}
			}
			//do other operations and save maintenance
			m.calculatedTotalManHours=Maintenances.calculateManHour(id);
			m.calculatedTotalTimeToRepair=Maintenances.calculateTTR(id);
			m.id = id;
			m.saveManyToManyAssociations("files");
			m.saveManyToManyAssociations("equipmentResources");
			m.saveManyToManyAssociations("dependentMaintenances");
			m.saveManyToManyAssociations("correspondingFailure");
			m.update(id);
			if(newData.get("holdReason").length()==0) {
			       Ebean
			         .createUpdate(Maintenances.class, "UPDATE maintenances SET hold_reason=null where id=:id")
			         .setParameter("id", id).execute();
			} else {
				//Can't finish maintenance when there is still a reason to hold
				if(!m.maintenanceStatus.equals(MaintenanceStatus.HOLD))
					throw new ValidationException("Can't change status when there is a reason for HOLD");
				}
			//Force state of all dependent maints to HOLD
			if(m.dependentMaintenances.size()>0 && m.maintenanceStatus!=MaintenanceStatus.FINISHED)
			for(Maintenances toHoldMaint : m.dependentMaintenances) {
				toHoldMaint.maintenanceStatus = MaintenanceStatus.HOLD;
				toHoldMaint.holdReason=HoldReasons.OTHER_MAINTENANCE;
				toHoldMaint.holdReasonComment = m.workOrderSerial;
				toHoldMaint.update();
			}
			
			flash("success", "Maintenace updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Maintenance update error",e.fillInStackTrace());
			flash("error", "Maintenance update error:" + e.getLocalizedMessage());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		updateManHourAndTtr(id);
		return redirect(routes.MaintenanceApplication.maintenanceIndex(id));
	}
	
	/**
	 * 
	 * @param mid
	 * @return
	 */
	@Dynamic(value = "maintenancePrivilege")
	public static Result referWorkTo(Long mid) {
		DynamicForm requestData = form().bindFromRequest();
		String stageId = requestData.get("workflowStage.id");
		if(stageId==null) {
			flash("error","Workflow form fill error!");
			return redirect(routes.MaintenanceApplication.maintenanceIndex(mid));
		}
		Maintenances m = Maintenances.find.byId(mid);
		WorkflowTree newWFS = WorkflowTree.find.byId(Long.valueOf(stageId));
		WorkflowTree oldWFS = m.workflowStage;
		Ebean.beginTransaction();
		try {
			if(!newWFS.equals(oldWFS)) {
				
				MaintenanceWorkflow mwf = new MaintenanceWorkflow();
				
				String deciding = new String();
				String referring = new String();
				String receiving = new String();
				
				
				//If we are back to step 1 then set deciding value to first workflow stage
				//else deciding is current workflow tree stage deciding value
				if(WorkflowTree.find.byId(newWFS.id).referringRole == null) {
					referring = "";
				} else {
					referring = WorkflowTree.find.byId(newWFS.id).referringRole.getName();
				}
				deciding = WorkflowTree.find.byId(newWFS.id).decidingRole.getName();
				
				//If we are at the end of workflow tree, set receiving value to last workflow stage
				//else deciding is current workflow tree stage deciding value
				if(WorkflowTree.find.byId(newWFS.id).receivingRole == null) {
					if(m.holdReason!=null) throw new ValidationException("Work is still ON HOLD. Remove HOLD before proceeding!");
					if(m.holdReasonComment!=null)
						if(m.holdReasonComment.length()!=0)
							throw new ValidationException("There is still comment for HOLD. Remove hold comment before proceeding!");
					receiving = "";
					if(History.find.where().eq("maint.id",mid).eq("isHappened",true).findRowCount()==0)
						throw new ValidationException("Can't finish! Maintenance is not LOGGED(Please log START & END dates)");
					m.maintenanceStatus=MaintenanceStatus.FINISHED;
				} else {
					receiving = WorkflowTree.find.byId(newWFS.id).receivingRole.getName();
				}
				
				if(oldWFS.receivingRole!=null) {
					if(deciding.equals(oldWFS.receivingRole.getName()) &&
							referring.equals(oldWFS.decidingRole.getName())) {
						mwf.acceptReject=true;
					} else if(oldWFS.referringRole!=null) {
						if(deciding.equals(oldWFS.referringRole.getName()) &&
								receiving.equals(oldWFS.decidingRole.getName())) {
							mwf.acceptReject=false;
						} else {
							throw new ValidationException("You can't refer to specified role! -1");
						}
					} else {
						throw new ValidationException("You can't refer to specified role! -2");
					}
				} else {
					if(deciding.equals(oldWFS.referringRole.getName()) &&
							receiving.equals(oldWFS.decidingRole.getName())) {
						m.maintenanceStatus=MaintenanceStatus.IN_PROGRESS;
						mwf.acceptReject=false;
					} else {
						throw new ValidationException("You can't refer to specified role! -3");
					}
				}
				//If maintenance is hold, it can't go forward, it can only go backward
				if(m.maintenanceStatus.equals(MaintenanceStatus.HOLD)) {
					if(newWFS.id>oldWFS.id)
						throw new ValidationException("Can't pass HOLD maintenance to next stage");
				}
				
				//Canceled maints can't be referred
				if(m.maintenanceStatus.equals(MaintenanceStatus.CANCEL)) {
					throw new ValidationException("Can't refer canceled maints");
				}
				
				mwf.user= Users.find.where().eq("userName", session().get("userName")).findUnique();
				if(mwf.user==null) throw new ValidationException("User is null");
				mwf.maintenance=m;
				mwf.actionDate=new Date();
				mwf.workflowStage = newWFS;
				mwf.rejectReason= requestData.field("reason").valueOr("");
				mwf.save();
				m.workflowStage = newWFS;
				m.update();
				//If decider is an Authorizer, then he/she must appoint person in charge
				if(oldWFS.receivingRole!=null) {
					if((oldWFS.decidingRole.getName().toLowerCase().contains("authorizer") ||
							oldWFS.receivingRole.getName().toLowerCase().contains("authorizer")
							) &&
							newWFS.id > oldWFS.id) {
						if(m.responsiblePerson == null) {
							throw new ValidationException("Please appoint person in charge by editing maintenance");
						}
					
					}
				}
				
				//Prevent user to traverse the tree in different order than the forward one.
				//Backward traverse must be reverse of forward traverse.
				if(mwf.acceptReject==false) {
					List<Long> stageIds = new ArrayList<Long>();
					List<MaintenanceWorkflow> allmwfs = MaintenanceWorkflow.find
								.where()
								.eq("maintenance.id",mid)
								.order("actionDate desc")
								.findList();
					if(allmwfs.size()>3) {
						for(MaintenanceWorkflow mmwf : allmwfs) {
							stageIds.add(mmwf.workflowStage.id);
						}
						Long currentStage = stageIds.get(0);
						Long previousStage = stageIds.get(1);
						//System.out.println("curstage="+currentStage);
						//System.out.println("prevStage="+previousStage);
						boolean flag=false;
						for(int i=2;i<stageIds.size()-1;i++) {
							if(stageIds.get(i)==previousStage && stageIds.get(i+1)==currentStage) flag=true;
						}
						if(flag==false) throw new ValidationException("You must go back to where you came from!");
					}
				}
				//In workflow we can't jump from one category to another!
				if(!WorkflowTree.find.byId(newWFS.id).treeCategory.equals(oldWFS.treeCategory)) {
					throw new ValidationException("Can't jump between workflows!");
				}
			}
			//End of workflow logic
			flash("success", "Maintenace updated");
			Ebean.commitTransaction();
			if(newWFS.description!=null)
				Lounge.remoteMessage(new Lounge.Talk(newWFS.description," :NEW"));
		} catch (Exception e) {
			Logger.error("Work referring error",e.fillInStackTrace());
			flash("error", e.getMessage());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.MaintenanceApplication.maintenanceIndex(mid));
	}
	
	/**
	 * 
	 * @param maintenanceId
	 * @return
	 */
	public static Result workflowIndex(Long maintenanceId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.maintenances.workflowIndex.render(
				maintenanceId, MaintenanceWorkflow.workflowList(maintenanceId)
				));
	}
	
	/**
	 * 
	 * @param maintenanceId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result deleteWorkflow(Long maintenanceId) {
		Ebean.beginTransaction();
		try {
			Maintenances m = Maintenances.find.byId(maintenanceId);
			m.workflowStage=null;
			for(MaintenanceWorkflow mwf : MaintenanceWorkflow.workflowList(maintenanceId)) {
				mwf.delete();
			}
			m.update();
			flash("success","Workflow deleted");
			Ebean.commitTransaction();
		} catch (Exception exc) {
			Logger.error("Portfolio create error",exc.fillInStackTrace());
			flash("error","Couldn't delete workflow");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.MaintenanceApplication.maintenanceIndex(maintenanceId));
	}
	
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result createWorkflow(Long maintenanceId) {
		Ebean.beginTransaction();
		try {
			Maintenances m = Maintenances.find.byId(maintenanceId);
			MaintenanceWorkflow mwf = new MaintenanceWorkflow();
			WorkflowTree wfs = WorkflowTree.find.where()
					.eq("treeCategory",m.maintenanceCategory)
					.isNull("referringRole")
					.findUnique();
			if(wfs==null) throw new ValidationException("Didn't find workflow!");
			m.workflowStage = mwf.workflowStage= wfs;
			m.maintenanceStatus=MaintenanceStatus.IN_PROGRESS;
			mwf.actionDate = new Date();
			mwf.acceptReject=true;
			mwf.maintenance =m;
			mwf.user = Users.findByUserName(session().get("userName"));
			mwf.save();
			m.update();
			flash("success","Workflow created");
			Ebean.commitTransaction();
		} catch (Exception exc) {
			Logger.error("Portfolio create error",exc.fillInStackTrace());
			flash("error","Can not create workflow");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.MaintenanceApplication.maintenanceIndex(maintenanceId));		
	}
	/**
	 * Create maintenance<->users information form
	 * 
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result createMaintenanceUser(Long maintenanceId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenancesUsers> muForm = form(MaintenancesUsers.class);
		return ok(views.html.maintenances.createMaintenanceUserForm.render(
				maintenanceId, muForm));
	}

	/**
	 * Saves maintenance<->user
	 * 
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result saveMaintenancesUsers(Long maintenanceId) {
		Form<MaintenancesUsers> newMUForm = form(MaintenancesUsers.class)
				.bindFromRequest();
		if (newMUForm.hasErrors()) {
			return badRequest(views.html.maintenances.createMaintenanceUserForm
					.render(maintenanceId, newMUForm));
		}
		if (newMUForm.field("user.id").value().length() == 0) {
			return badRequest(views.html.maintenances.createMaintenanceUserForm
					.render(maintenanceId, newMUForm));
		}
		Ebean.beginTransaction();
		try {
			MaintenancesUsers mu = newMUForm.get();
			mu.maintenance = Maintenances.find.byId(maintenanceId);
			mu.save();
			flash("success", "personnel info updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Error in saving maintenance-users",e.fillInStackTrace());
			flash("error", "Can't save maintenance users");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		updateManHourAndTtr(maintenanceId);
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintenanceReport");
	}

	/**
	 * Displays maintenance<->users edit form
	 * 
	 * @param id
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result editMaintenanceUser(Long id, Long maintenanceId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenancesUsers> muForm = form(MaintenancesUsers.class).fill(
				MaintenancesUsers.find.byId(id));

		return ok(views.html.maintenances.editMaintenanceUser.render(id,
				maintenanceId, muForm));
	}

	/**
	 * update maintenance<->users
	 * 
	 * @param id
	 * @param maintenanceId
	 * @return
	 * */
	@Dynamic(value = "maintenanceInCharge")
	public static Result updateMaintenancesUsers(Long id, Long maintenanceId) {
		Form<MaintenancesUsers> muForm = form(MaintenancesUsers.class)
				.bindFromRequest();
		if (muForm.hasErrors()) {
			return badRequest(views.html.maintenances.editMaintenanceUser
					.render(id, maintenanceId, muForm));
		}

		if (muForm.field("user.id").value().length() == 0) {
			return badRequest(views.html.maintenances.editMaintenanceUser
					.render(id, maintenanceId, muForm));
		}
		Ebean.beginTransaction();
		try {
			MaintenancesUsers mu = muForm.get();
			mu.update(id);
			flash("success", "personnel info updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("MaintenanceUsers update error",e.fillInStackTrace());
			flash("error", "Can't update maintenance-personnel");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		updateManHourAndTtr(maintenanceId);
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintenanceReport");
	}
	
	/**
	 * Removes maintenance-user
	 * @param muId
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result deleteMaintenanceUser(Long muId, Long maintenanceId) {
		Ebean
        .createUpdate(MaintenancesUsers.class, "DELETE FROM maintenances_users WHERE id=:id")
        .setParameter("id", muId).execute();
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintenanceReport");
	}
	
	/**
	 * Display maintenance <-> part create form
	 * 
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result createMaintenancePart(Long maintenanceId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenancesParts> mpForm = form(MaintenancesParts.class);
		return ok(views.html.maintenances.createMaintenancePartForm.render(
				maintenanceId, mpForm));
	}

	/**
	 * Saves maintenances <-> parts
	 * 
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result saveMaintenancesParts(Long maintenanceId) {
		Form<MaintenancesParts> newMPForm = form(MaintenancesParts.class)
				.bindFromRequest();
		if (newMPForm.hasErrors()) {
			return badRequest(views.html.maintenances.createMaintenancePartForm
					.render(maintenanceId, newMPForm));
		}
		if (newMPForm.field("part.id").value().length() == 0) {
			return badRequest(views.html.maintenances.createMaintenancePartForm
					.render(maintenanceId, newMPForm));
		}
		Ebean.beginTransaction();
		try {
			MaintenancesParts mp = newMPForm.get();
			mp.maintenance = Maintenances.find.byId(maintenanceId);
			mp.stockFlag = false;
			mp.save();
			flash("success", "parts added.");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("MaintenanceParts save error",e.fillInStackTrace());
			flash("error", "Can't add parts to this maintenance");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintenanceReport");
	}

	/**
	 * maintenances <-> parts edit form display
	 * 
	 * @param id
	 * @param maintenanceId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("warehouse"), @Group("planner") })
	public static Result editMaintenancePart(Long id, Long maintenanceId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenancesParts> mpForm = form(MaintenancesParts.class).fill(
				MaintenancesParts.find.byId(id));

		return ok(views.html.maintenances.editMaintenancePart.render(id,
				maintenanceId, mpForm));
	}

	/**
	 * Updates maintenances <-> parts
	 * 
	 * @param id
	 * @param maintenanceId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("warehouse"), @Group("planner") })
	public static Result updateMaintenancesParts(Long id, Long maintenanceId) {
		Form<MaintenancesParts> mpForm = form(MaintenancesParts.class)
				.bindFromRequest();
		if (mpForm.hasErrors()) {
			return badRequest(views.html.maintenances.editMaintenancePart
					.render(id, maintenanceId, mpForm));
		}

		Ebean.beginTransaction();
		try {
			MaintenancesParts mp = mpForm.get();
			MaintenancesParts old_mp = MaintenancesParts.find.byId(id);
			Parts par = Parts.find.where().eq("name", old_mp.part.name)
					.findUnique();
			float oldQuantity = old_mp.quantity;
			float newQuantity = mp.quantity;
			float rq = par.remainingQuantity;
			float updateVal = 0;
			
			if (old_mp.stockFlag == true && oldQuantity != newQuantity) {
				if(newQuantity<0)
					throw new ValidationException("Can't use negative values for received parts here");
				mp.part=old_mp.part;
				PartHistory ph = new PartHistory();
				ph.parentPart=par;
				ph.parentMaintenance=Maintenances.find.byId(maintenanceId);
				ph.commenceDate = new Date();
				ph.registrar=Users.findByUserName(session().get("userName"));
				ph.requester= ph.registrar;
				if(oldQuantity < newQuantity) {
					ph.eventType=InventoryEvents.find.byId(1L);
					ph.stockBalance= oldQuantity - newQuantity;
					updateVal = rq +oldQuantity - newQuantity;
				} else if(oldQuantity > newQuantity) {
					ph.eventType=InventoryEvents.find.byId(0L);
					ph.stockBalance= newQuantity - oldQuantity;
					updateVal = rq - (newQuantity - oldQuantity);
					if(updateVal<0)
						throw new ValidationException(
								"Requested parts are more than stock!");
				}
				ph.remainingStock=par.remainingQuantity;
				ph.save();
				Ebean
		         .createUpdate(Parts.class, "UPDATE parts SET remaining_quantity=:requ WHERE id=:id")
		         .setParameter("requ", updateVal)
		         .setParameter("id", par.id)
		         .execute();
				mp.stockFlag=true;
				mp.update(id);
				flash("success",
						ph.stockBalance + " of " + par.name + " received from warehouse");
				Ebean.commitTransaction();
			} else {
				mp.update(id);
				flash("success","No changes made");
				Ebean.commitTransaction();
			}
		} catch (Exception e) {
			Logger.error("Error in part-maintenance update",e.fillInStackTrace());
			flash("error","Error in part-maintenance update");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.MaintenanceApplication.maintenanceIndex(maintenanceId)+"#tabs-maintenanceReport");
	}
	@Restrict({ @Group("admin"), @Group("planner") })
	public static Result deleteMaintenancePart(Long mpId, Long maintenanceId) {
		MaintenancesParts mptemp = MaintenancesParts.find.where().eq("id",mpId).select("id").findUnique();
		if(mptemp.stockFlag==false || mptemp.quantity.compareTo(0F)==0) {
			Ebean
	         .createUpdate(MaintenancesParts.class, "DELETE FROM maintenances_parts WHERE id=:id")
	         .setParameter("id", mpId).execute();
		} else {
			flash("error", "This part has already been received from warehouse");
		}
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintenanceReport");
	}

	/**
	 * Display maintenance <-> components create form
	 * 
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result createMaintenanceComponent(Long maintenanceId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenancesComponents> mcForm = form(MaintenancesComponents.class);
		return ok(views.html.maintenances.createMaintenanceComponentForm
				.render(maintenanceId, mcForm));
	}

	/**
	 * Saves maintenances <-> components
	 * 
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result saveMaintenancesComponents(Long maintenanceId) {
		Form<MaintenancesComponents> newMCForm = form(
				MaintenancesComponents.class).bindFromRequest();
		if (newMCForm.hasErrors()) {
			return badRequest(views.html.maintenances.createMaintenanceComponentForm
					.render(maintenanceId, newMCForm));
		}
		if (newMCForm.field("component.id").value().length() == 0) {
			return badRequest(views.html.maintenances.createMaintenanceComponentForm
					.render(maintenanceId, newMCForm));
		}
		Ebean.beginTransaction();
		try {
			MaintenancesComponents mc = newMCForm.get();
			mc.maintenance = Maintenances.find.byId(maintenanceId);
			mc.save();
			flash("success", "component repair record updated!");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("MaintenanceComponents save error",e.fillInStackTrace());
			flash("error", "Error in adding component to this maintenance");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		updateManHourAndTtr(maintenanceId);
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintainedItems");
	}

	/**
	 * maintenances <-> components edit form display
	 * 
	 * @param id
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result editMaintenanceComponent(Long id, Long maintenanceId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenancesComponents> mcForm = form(MaintenancesComponents.class)
				.fill(MaintenancesComponents.find.byId(id));

		return ok(views.html.maintenances.editMaintenanceComponent.render(id,
				maintenanceId, mcForm));
	}

	/**
	 * Updates maintenances <-> components
	 * 
	 * @param id
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result updateMaintenancesComponents(Long id,
			Long maintenanceId) {
		Form<MaintenancesComponents> mcForm = form(MaintenancesComponents.class)
				.bindFromRequest();
		if (mcForm.hasErrors()) {
			return badRequest(views.html.maintenances.editMaintenanceComponent
					.render(id, maintenanceId, mcForm));
		}
		if (mcForm.field("component.id").value().length() == 0) {
			return badRequest(views.html.maintenances.editMaintenanceComponent
					.render(id, maintenanceId, mcForm));
		}
		Ebean.beginTransaction();
		try {
			MaintenancesComponents mc = mcForm.get();
			mc.update(id);
			flash("success", "Component repair record updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("MaintenanceComponents update error",e.fillInStackTrace());
			flash("error", "Can't update components for this maintenance");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		updateManHourAndTtr(maintenanceId);
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintainedItems");
	}
	
	/**
	 * delete maintenance-component
	 * @param muId
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result deleteMaintenanceComponent(Long mcId, Long maintenanceId) {
		MaintenancesComponents.find.byId(mcId).delete();
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintainedItems");
	}


	/**
	 * Display maintenance <-> subunits create form
	 * 
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result createMaintenanceSubunit(Long maintenanceId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenancesSubunits> msForm = form(MaintenancesSubunits.class);
		return ok(views.html.maintenances.createMaintenanceSubunitForm.render(
				maintenanceId, msForm));
	}

	/**
	 * Saves maintenances <-> subunits
	 * 
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result saveMaintenancesSubunits(Long maintenanceId) {
		Form<MaintenancesSubunits> newMSForm = form(MaintenancesSubunits.class)
				.bindFromRequest();
		if (newMSForm.hasErrors()) {
			return badRequest(views.html.maintenances.createMaintenanceSubunitForm
					.render(maintenanceId, newMSForm));
		}
		if (newMSForm.field("subunit.id").value().length() == 0) {
			return badRequest(views.html.maintenances.createMaintenanceSubunitForm
					.render(maintenanceId, newMSForm));
		}
		Ebean.beginTransaction();
		try {
			MaintenancesSubunits ms = newMSForm.get();
			ms.maintenance = Maintenances.find.byId(maintenanceId);
			ms.save();
			flash("success", "subunit repair record saved!");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("MaintenanceSubunits save error",e.fillInStackTrace());
			flash("error", "Error in subunit repair record save");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		updateManHourAndTtr(maintenanceId);
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintainedItems");
	}

	/**
	 * maintenances <-> subunits edit form display
	 * 
	 * @param id
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result editMaintenanceSubunit(Long id, Long maintenanceId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenancesSubunits> msForm = form(MaintenancesSubunits.class)
				.fill(MaintenancesSubunits.find.byId(id));

		return ok(views.html.maintenances.editMaintenanceSubunit.render(id,
				maintenanceId, msForm));
	}

	/**
	 * Updates maintenances <-> subunits
	 * 
	 * @param id
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result updateMaintenancesSubunits(Long id, Long maintenanceId) {
		Form<MaintenancesSubunits> msForm = form(MaintenancesSubunits.class)
				.bindFromRequest();
		if (msForm.hasErrors()) {
			return badRequest(views.html.maintenances.editMaintenanceSubunit
					.render(id, maintenanceId, msForm));
		}
		if (msForm.field("subunit.id").value().length() == 0) {
			return badRequest(views.html.maintenances.editMaintenanceSubunit
					.render(id, maintenanceId, msForm));
		}
		Ebean.beginTransaction();
		try {
			MaintenancesSubunits ms = msForm.get();
			ms.update(id);
			flash("success", "Subunit repair record updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("MaintenanceSubunits update error",e.fillInStackTrace());
			flash("error", "Error in updating subunits of this maintenance");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		updateManHourAndTtr(maintenanceId);
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintainedItems");
	}
	
	/**
	 * delete maintenance-subunit
	 * @param mcId
	 * @param maintenanceId
	 * @return
	 */
	@Dynamic(value = "maintenanceInCharge")
	public static Result deleteMaintenanceSubunit(Long msId, Long maintenanceId) {
		MaintenancesSubunits.find.byId(msId).delete();
		return redirect(routes.MaintenanceApplication
				.maintenanceIndex(maintenanceId)+"#tabs-maintainedItems");
	}
	
	/**
     * 
     * @param
     * @param
     * @return
     */
    public static Result sectionEquipmentFiller(Long sectionId) {
    	ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	
    	for(Equipments in : Equipments.find.where().eq("section.id", sectionId)
    			.orderBy("name").findList()) {
    		ObjectNode node = Json.newObject();
    		node.put("id", in.id);
    		node.put("value", in.name);
    		allnodes.add(node);
    	}
    	return ok(play.libs.Json.toJson(allnodes));
        }
    
    /**
     * 
     * @param
     * @param
     * @return
     */
    public static Result equipmentRoutineFiller(Long equipmentId) {
    	ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	Equipments eqo = Equipments.find.byId(equipmentId);
    	Long eqoSId = -1L;
    	if(eqo!=null)
    		eqoSId = eqo.section.id;
    	for(PreventiveMaintenances pm : PreventiveMaintenances.find.where()
    			.or(Expr.eq("parentSection.id",eqoSId),
    				Expr.eq("parentEquipment.id", equipmentId))
    			.findList()) {
    		ObjectNode node = Json.newObject();
    		node.put("id", pm.id);
    		if(pm.parentEquipment!=null)
    			node.put("value", pm.name+"---"+pm.parentEquipment.name);
    		if(pm.parentSection!=null)
    			node.put("value",pm.name+"---"+pm.parentSection.name);
    		if(pm.parentPlant!=null)
    			node.put("value",pm.name+"---"+pm.parentPlant.name);
    		allnodes.add(node);
    	}
    	return ok(play.libs.Json.toJson(allnodes));
        }
    
    /**
     * 
     * @param
     * @param
     * @return
     */
    public static Result equipmentClassRoutineFiller(Long equipmentId) {
    	ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	Long ecid;
    	ecid = Equipments.find.byId(equipmentId).equipmentClass.id;
    	for(PreventiveMaintenances pm: PreventiveMaintenances.find.where()
    			.eq("pmClass.id",ecid).findList()) {
       		ObjectNode node = Json.newObject();
    		node.put("id", pm.id);
    		if(pm.pmClass!=null)
    			node.put("value", pm.name + "---"+pm.pmClass.name);
    		allnodes.add(node);
    	}
    	return ok(play.libs.Json.toJson(allnodes));
        }
    
    /**
     * 
     * @param
     * @param
     * @return
     */
    public static Result sectionRoutineFiller(Long sectionId) {
    	ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	for(PreventiveMaintenances pm : PreventiveMaintenances.find.where().eq("parentSection.id", sectionId).findList()) {
    		ObjectNode node = Json.newObject();
    		node.put("id", pm.id);
    		if(pm.parentSection!=null)
    			node.put("value", pm.name+"---"+pm.parentSection.name);
    		allnodes.add(node);
    	}
    	return ok(play.libs.Json.toJson(allnodes));
        }
    
    @Dynamic(value = "maintenanceInCharge")
    public static Result logMaintenance(Integer parentLevel, Long parentId, Long maintenanceId) {
    	return redirect(
    			routes.HistoryApplication.createHistory(parentLevel, parentId, maintenanceId, -1L));
    }
    
    /**
	 * Uploads file directly for current maintenance
	 * @return
	 */
    @Dynamic(value = "maintenancePrivilege")
	public static Result upload(Long maintenanceId) {
		MultipartFormData body = request().body().asMultipartFormData();
			FilePart uploadedFile = body.getFile("file");
			String un = session().get("userName");
			Maintenances ma = Maintenances.find.byId(maintenanceId);
			try {
				ma.files.add(
						Blobs.find.byId(myUtils.FileUtils.upload(uploadedFile, un)));
				ma.saveManyToManyAssociations("files");
				ma.update();
				
			} catch (IOException e1) {
				Logger.error("Maintenance file upload error",e1.fillInStackTrace());
				flash("error","Can't upload file for this maintenance");
				
			} catch(IllegalStateException e2) {
				Blobs fblob = Blobs.find.where().eq("name",uploadedFile.getFilename()).findUnique();
				if(!ma.files.contains(fblob)) {
					ma.files.add(fblob);
					ma.saveManyToManyAssociations("files");
					ma.update();
					flash("success",e2.getMessage()+" File added to data source anyway.");
					
				} else flash("error", e2.getMessage()+ " File already is a data source");
				
			}
		 	return redirect(routes.MaintenanceApplication.maintenanceIndex(maintenanceId)+"#tabs-dataSources");
	}
    
    /**
     * 
     * @return
     */
    public static Result showUnloggedMaintenances(String currentHours,String currentDays,String currentMonths) {
    	response().setHeader("Cache-Control",
    			"no-store, no-cache, must-revalidate");
    	response().setHeader("Pragma", "no-cache");
    	List<Maintenances> unloggedMaints = new ArrayList<Maintenances>();
		for(Maintenances m : Maintenances.find.where()
				.eq("maintenanceStatus",MaintenanceStatus.FINISHED)
				.findList()) {
			if(History.find.where().eq("maint.id",m.id).eq("isHappened",true).findRowCount() == 0)
				unloggedMaints.add(m);
		}
		DateTime rightNow = new DateTime();
		Integer curHours, curDays, curMonths;
		try {
			curHours = Integer.valueOf(currentHours);
			curDays = Integer.valueOf(currentDays);
			curMonths = Integer.valueOf(currentMonths);
		} catch (Exception exc) {
			curHours = 0;
			curDays = 0;
			curMonths = 0;
		}
		List<History> delayedList = History.find.where()
			.isNotNull("maint")
			.eq("isHappened", false)
			.le("start", rightNow.minusHours(curHours).minusDays(curDays).minusMonths(curMonths).toDate())
			.findList();
    	return ok(
    			views.html.maintenances.showUnloggedMaints.render(
    					unloggedMaints,delayedList,currentHours,currentDays,currentMonths));
    }
    
    /**
	 * Removes association and probably the file itself from equipment-blob many-to-many
	 * @param equipmentId
	 * @param blobId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result deleteAssociation(Long maintenanceId, Long blobId) {
		Maintenances maa = Maintenances.find.byId(maintenanceId);
		Ebean.beginTransaction();
		try {
			maa.files.remove(
					Blobs.find.byId(blobId)
					);
			maa.saveManyToManyAssociations("files");
			maa.update(maintenanceId);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Maintenance blob upload error",e.fillInStackTrace());
			flash("error","Couldn't remove association");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		try {
			String fileName = Blobs.find.byId(blobId).blobFile;
			Blobs.find.ref(blobId).delete();
			File ftoDel = new File(fileName);
			ftoDel.delete();
		} catch (Exception e) {
			Logger.error("Maintenance association removal error",e.fillInStackTrace());
		}
		return redirect(routes.MaintenanceApplication.maintenanceIndex(maintenanceId)+"#tabs-dataSources");
	}
	
	@Restrict({ @Group("admin")})
	public static Result delete(Long maintId) {
		if(Maintenances.delete(maintId)==true)
			flash("success","Maintenance successfully deleted");
		else
			flash("error","Can't delete maintenance. Remove all associated subunits, components, parts, and personnel and then try again");
		return GO_HOME;
	}
	
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result removeFromGroup (Long maintId, Long groupId) {
		Maintenances maintToRemove = Maintenances.find.byId(maintId);
		MaintenanceGroups groupList = MaintenanceGroups.find.byId(groupId);
		maintToRemove.maintenanceGroup=null;
		maintToRemove.update();
		groupList.memberMaints.remove(maintToRemove);
		groupList.update();
		return redirect (routes.MaintenanceGroupApplication.maintenanceGroupIndex(groupId));
	}
	
	/**
     * 
     * @param equipmentId
     * @return
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result mttr(Long equipmentId) {
    	ObjectNode node = Json.newObject();
    	Float mttr = 0.0f;
    	List<Maintenances> emaints = Maintenances.find.where()
    			.eq("maintainedEquipment.id",equipmentId)
    			.eq("maintenanceCategory",MaintenanceCategories.CORRECTIVE_MAINTENANCE)
    			.eq("maintenanceStatus",MaintenanceStatus.FINISHED)
    			.findList();
    	Integer numOfMaints = emaints.size();
    	Long totalMaintTime = 0L;
    	if(numOfMaints==0) return ok("undefined");
    	
    	for(int i=0;i<numOfMaints;i++) {
    		History mh = History.find.where().eq("maint.id",emaints.get(i).id).eq("isHappened", true).findUnique();
    		if(mh!=null) totalMaintTime += (mh.end.getTime()-mh.start.getTime())/(3600000);
    	}
    	
    	//mettr calculation
    	mttr = Float.valueOf(totalMaintTime/numOfMaints);
    	node.put("mttr",mttr.toString());
    	
    	return ok(play.libs.Json.toJson(node));
    }
	
}