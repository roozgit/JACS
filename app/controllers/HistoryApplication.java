package controllers;


import static play.data.Form.form;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ValidationException;

import models.Components;
import models.EventTypes;
import models.History;
import models.Installations;
import models.Plants;
import models.Sections;
import models.SecurityRole;
import models.Subunits;
import models.Users;
import models.equipment.Equipments;
import models.failure.Failures;
import models.maintenance.MaintenanceCategories;
import models.maintenance.Maintenances;
import myUtils.DateTimeUtils;

import org.joda.time.DateTime;

import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Dynamic;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;

@SubjectPresent
public class HistoryApplication extends Controller {
	
	/**
	 * Converts dates to Gregorian for save in db
	 * @param his
	 */
	public static void parseDatesToGregorian(History his) {
		his.start = DateTimeUtils
				.getGregorianDateTime(his.start);
		his.end = DateTimeUtils
				.getGregorianDateTime(his.end);
	}
	
	/**
	 * converts dates to Shamsi to show in edit fill forms
	 * @param his
	 */
	public static void parseDateTimesAsIranianDateTimes(History his) {
		his.start = DateTimeUtils
				.getIranianDateTimeAsDate(his.start);
		his.end = DateTimeUtils
				.getIranianDateTimeAsDate(his.end);
	}
	
	
	/**
     * Checks if history event ends the same day which starts
     * @param start Date
     * @param end Date
     * @return Boolean: True if ends same day
     */
    private static Boolean endsSameDay(Date start, Date end){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        return dateFormat.format(start).equals(dateFormat.format(end));
    }
    
    /**
     * 
     * @return
     */
    public static Result plannedMaintsHistoryList(int page, String sortBy, String order) {
    	response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
    	int ppid = 25;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 20;
			}
		}
    	return ok(views.html.history.historyList.render(
				3,
				1L,
				History.findHistories(
						"parentInstallation.id", 1L, "", false, new Date(), new Date(),"PREVENTIVE_MAINTENANCE","planned")
						.orderBy(sortBy + " " + order)
	                    .findPagingList(ppid)
	                    .setFetchAhead(false)
	                    .getPage(page),
	                    sortBy, order, "",
	                    true,
	                    null
    			));						
		}

	/**
	 * shows ONE history item
	 * @param historyId
	 * @return
	 */
	public static Result historyIndex(Long historyId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		
		return ok(views.html.history.historyIndex.render(
				historyId,
				History.find.byId(historyId)
				));
				
	}
	
	/**
     * Returns list of history events for calendar view
     * @param start Long Timestamp of current view start
     * @param end Long Timestamp of current view end
     * @return Result
     */
    public static Result json(Integer parentLevel, Long parentId, String type, String occur) {
    	
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
		}
		Date start = new Date(1000*Long.valueOf(play.mvc.Controller.request().getQueryString("start")));
		Date end = new Date(1000*Long.valueOf(play.mvc.Controller.request().getQueryString("end")));
		
		List<History> resultList = History.findHistories(
				fieldName, parentId, "", true, start, end,type,occur)
				.findList();
        ArrayList<Map<Object, Serializable>> allEvents = new ArrayList<Map<Object, Serializable>>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (History event : resultList) {
            Map<Object, Serializable> eventRemapped = new HashMap<Object, Serializable>();
            eventRemapped.put("id", event.id);
            if(event.eventType==EventTypes.MAINTENANCE && event.maint!=null) {
            	
            	eventRemapped.put("title",event.maint.workOrderSerial);
            } else {
            	if(event.eventType==EventTypes.FAILURE && event.fail!=null) {
            		
            		String item =
        				(event.parentInstallation==null?"":event.parentInstallation.name) +
        				(event.parentPlant==null?"":event.parentPlant.name) +
        				(event.parentSection==null?"":event.parentSection.name) +
        				(event.parentEquipment==null?"":event.parentEquipment.name) +
        				(event.parentSubunit==null?"":event.parentSubunit.name) +
        				(event.parentComponent==null?"":event.parentComponent.name);
            		
            		eventRemapped.put("title", item+":"+event.fail.failureMode.failureModeCode);
            	} else
            		eventRemapped.put("title", event.eventType);
            }
            eventRemapped.put("start", df.format(event.start));
            eventRemapped.put("end", df.format(event.end));
            eventRemapped.put("allDay", event.allDay);
            eventRemapped.put("url", controllers.routes.HistoryApplication.historyIndex(event.id).toString());
            
        	if(event.eventType==EventTypes.FAILURE) {
            	eventRemapped.put("color", "red");
            } else if(event.eventType==EventTypes.NORMAL_OPERATION) {
            	if(event.isHappened==true) eventRemapped.put("color","green"); else eventRemapped.put("color","lightgreen");
            } else 	if(event.eventType==EventTypes.MAINTENANCE) {
            	if(event.maint.maintenanceCategory.equals(MaintenanceCategories.CORRECTIVE_MAINTENANCE)) {
            		if(event.isHappened==true)
            			eventRemapped.put("color","rgb(47,111,167)");
            		else
            			eventRemapped.put("color","lightblue");
            	}
            	if(event.maint.maintenanceCategory.equals(MaintenanceCategories.PREVENTIVE_MAINTENANCE)) {
            		if(event.isHappened==true)
            			eventRemapped.put("color","purple");
            		else
            			eventRemapped.put("color","lightblue");
            	}
            }
            
            allEvents.add(eventRemapped);
        }
        return ok(play.libs.Json.toJson(allEvents));
    }
	
    /**
     * Displays full calendar
     * @return Result
     */
    public static Result calendar(Integer parentLevel, Long parentId, String type, String occur) {
    	response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
    	String title = new String("");
		switch(parentLevel) {
		case 3:
			title = Installations.find.byId(parentId).name;
			break;
		case 4:
			title = Plants.find.byId(parentId).name;
			break;
		case 5:
			title = Sections.find.byId(parentId).name;
			break;
		case 6:
			title = Equipments.find.byId(parentId).name;
			break;
		case 7:
			title = Subunits.find.byId(parentId).name;
			break;
		case 8:
			title = Components.find.byId(parentId).name;
			break;
		}
		
        return ok(views.html.calendar.render(parentLevel, parentId, title));
    }
	
	/**
	 * Lists all history of an item
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
		default:
			return badRequest("No such taxonomy");
		}
		int ppid = 20;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 20;
			}
		}
		
		return ok(views.html.history.historyList.render(
				parentLevel,
				parentId,
				History.page(fieldName, parentId, page, ppid, sortBy, order,filter),
				sortBy, order, filter,
				false,
				History.findLastRoutines(parentLevel, parentId)
				));
	}
	
	/**
	 * Display create history form
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	public static Result createHistory(Integer parentLevel, Long parentId, Long maintenanceId , Long failureId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<History> newHistoryForm = form(History.class);
		return ok(views.html.history.historyCreateForm.render(
					parentLevel, parentId, maintenanceId, failureId, newHistoryForm
					));

	}
	
	/**
	 * save new history
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Dynamic(value = "logger")
	public static Result save(Integer parentLevel, Long parentId, Long maintenanceId , Long failureId) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {
				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (History.class.getField(key).getType()
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
					if(History.class.getField(key).getType()
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
		Form<History> newHistoryForm = new Form<History>(
				History.class).bind(newData);
		if (newHistoryForm.hasErrors()) {
			//History formHis = newHistoryForm.get();
			//parseDateTimesAsIranianDateTimes(formHis);
			//newHistoryForm.fill(formHis);
			newHistoryForm = new Form<History>(History.class);
			return badRequest(views.html.history.historyCreateForm.render(
					parentLevel, parentId, maintenanceId, failureId, newHistoryForm));
		}
		History his = newHistoryForm.get();
		
			//Begin saving
			Ebean.beginTransaction();
			try
			{
			
			if(maintenanceId!=-1L || failureId!=-1L) {
				his.systemEvent=true;
			} else {
				if(his.eventType!=EventTypes.NORMAL_OPERATION)
					throw new ValidationException("Only normal operation can be planned manually");
				his.systemEvent=false;
			}
			if(maintenanceId!=-1L) {
				his.maint=Maintenances.find.byId(maintenanceId);
				his.eventType=EventTypes.MAINTENANCE;
			} else if(failureId!=-1L) {
				his.fail=Failures.find.byId(failureId);
				his.eventType=EventTypes.FAILURE;
			}
			his.registrar=Users.findByUserName(session().get("userName"));
			
			switch(parentLevel) {
			case 3:
				his.parentInstallation = Installations.find.byId(parentId);
				if(his.parentInstallation==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 4:
				his.parentPlant = Plants.find.byId(parentId);
				if(his.parentPlant==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 5:
				his.parentSection = Sections.find.byId(parentId);
				if(his.parentSection==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 6:
				his.parentEquipment = Equipments.find.byId(parentId);
				if(his.parentEquipment==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 7:
				his.parentSubunit = Subunits.find.byId(parentId);
				if(his.parentSubunit==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 8:
				his.parentComponent = Components.find.byId(parentId);
				if(his.parentComponent==null) throw new ValidationException("Null parent can't be saved!");
				break;
			default:
				throw new ValidationException("History taxonomy not valid!");
			}
			his.save();
			flash("success","Log added");
			Ebean.commitTransaction();
			} catch (Exception e) {
				Logger.error("History save error",e.fillInStackTrace());
				flash("error","Can't save log");
				Ebean.rollbackTransaction();
			} finally {
				Ebean.endTransaction();
			}
			if(maintenanceId!=-1)
				return redirect(
						routes.MaintenanceApplication.maintenanceIndex(maintenanceId));
			else
			return redirect(
					routes.HistoryApplication.list(parentLevel, parentId,0,"eventType","asc",""));
	}

	
    /**
     * Saves in DB date changed by event drag
     * @return Result
     */
    public static Result move() {

        Long id = Long.valueOf(form().bindFromRequest().get("id"));
        int dayDelta = Integer.parseInt(form().bindFromRequest().get("dayDelta"));
        int minuteDelta = Integer.parseInt(form().bindFromRequest().get("minuteDelta"));

        History event = History.find.byId(id);
        Users curu = Users.findByUserName(session().get("userName"));
        if(event.registrar!=null &&
        		(event.registrar.id==curu.id ||
        		curu.roles.contains(SecurityRole.findByName("admin")) ||
        		curu.roles.contains(SecurityRole.findByName("planner"))
        				)) {
	        event.start = new DateTime(event.start).plusDays(dayDelta).plusMinutes(minuteDelta).toDate();
	        event.end = new DateTime(event.end).plusDays(dayDelta).plusMinutes(minuteDelta).toDate();
	        event.allDay = Boolean.valueOf(form().bindFromRequest().get("allDay"));
	        event.endsSameDay = endsSameDay(event.start, event.end);
	        event.update();
	        return ok("changed");
        } else {
            return badRequest("You can not move this event!");
        }

        
    }
    
    /**
     * Saves in DB end date changed by event resize
     * @return Result
     */
    public static Result resize() {

        Long id = Long.valueOf(form().bindFromRequest().get("id"));
        int dayDelta = Integer.parseInt(form().bindFromRequest().get("dayDelta"));
        int minuteDelta = Integer.parseInt(form().bindFromRequest().get("minuteDelta"));

        History event = History.find.byId(id);
        Users curu = Users.findByUserName(session().get("userName"));
        if(event.registrar!=null &&
        		(event.registrar.id==curu.id ||
        		curu.roles.contains(SecurityRole.findByName("admin")) ||
        		curu.roles.contains(SecurityRole.findByName("planner"))
        				)) {
        event.end = new DateTime(event.end).plusDays(dayDelta).plusMinutes(minuteDelta).toDate();
        event.endsSameDay = endsSameDay(event.start, event.end);
        event.update();
        return ok("changed");
        } else {
        	return badRequest("You can not resize this event!");
        }
    }
    
	/**
	 * Display edit history form
	 * @param historyId
	 * @return
	 */
	public static Result editHistory(Long historyId, String msg) {
			response().setHeader("Cache-Control",
					"no-store, no-cache, must-revalidate");
			response().setHeader("Pragma", "no-cache");
			History his = History.find.byId(historyId);
			parseDateTimesAsIranianDateTimes(his);
			Form<History> edithistoryForm = form(History.class).fill(his);
			Users curu = Users.findByUserName(session().get("userName"));
	        if(History.find.byId(historyId).registrar!=null &&
	        		(History.find.byId(historyId).registrar.id==curu.id ||
	        		curu.roles.contains(SecurityRole.findByName("admin")) ||
	        		curu.roles.contains(SecurityRole.findByName("planner"))
	        				)) {
			return ok(views.html.history.historyEdit.render(
					historyId, edithistoryForm,msg
					));
			} else {
				flash("error","Only history creator can edit it!");
				return redirect(routes.HistoryApplication.historyIndex(historyId));
			}

	}
	
	/**
	 * update history
	 * @param historyId
	 * @return
	 */
	public static Result update(Long historyId) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {
				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (History.class.getField(key).getType()
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
					if(History.class.getField(key).getType()
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
		Form<History> updateForm = new Form<History>(
				History.class).bind(newData);
		if (updateForm.hasErrors()) {
			History hitemp = History.find.byId(historyId);
			parseDateTimesAsIranianDateTimes(hitemp);
			updateForm = form(History.class).fill(hitemp);
			return badRequest(views.html.history.historyEdit.render(
					historyId, updateForm,"Error"
					));
		}
		History oldHis = History.find.byId(historyId);
		History his = updateForm.get();
			
		Ebean.beginTransaction();
		try {
			Users curu = Users.findByUserName(session().get("userName"));
	        if(History.find.byId(historyId).registrar.id!=curu.id &&
	        		!curu.roles.contains(SecurityRole.findByName("admin")) &&
	        		!curu.roles.contains(SecurityRole.findByName("planner"))) {
				throw new ValidationException("Only history creator can update it!");
			}
	        
	        if(his.fail != null && his.isHappened==false) {
	        	throw new ValidationException("A failure can not be in this state");
	        }
	        
	        if(oldHis.eventType != his.eventType) {
	        	throw new ValidationException("Event type can never be changed");
	        }
	        
			his.update(historyId);
			Ebean.commitTransaction();
			flash("success","History updated");
		} catch (Exception ex) {
			Logger.error("Failure update error",ex.fillInStackTrace());
			flash("error","History update failed");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.HistoryApplication.historyIndex(historyId));
	}
	
	/**
	 * 
	 * @param id
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	public static Result deleteH(Long id, Integer parentLevel, Long parentId, String calType) {
		Long maintId = -1L;
		Long failId=-1L;
		Failures todelFail = new Failures();
		Ebean.beginTransaction();
		try {
			History todel = History.find.byId(id);
			
			Users curu = Users.findByUserName(session().get("userName"));
			if(History.find.byId(id).registrar.id!=curu.id &&
	        		!curu.roles.contains(SecurityRole.findByName("admin")) &&
	        		!curu.roles.contains(SecurityRole.findByName("planner"))) {
				throw new ValidationException("Only history creator can update it!");
			}
						
			if(todel.fail!=null) {
				failId = todel.fail.id;
				todelFail = todel.fail;
				for(Maintenances mm: Maintenances.find.all()) {
					if(mm.correspondingFailure.contains(todelFail)) {
						mm.correspondingFailure.remove(todelFail);
						mm.saveManyToManyAssociations("correspondingFailure");
					}
				}
				todelFail.deleteManyToManyAssociations("failureCauses");
			}
			todel.delete();
			if(failId!=-1L) todelFail.delete();
			if(todel.maint!=null) {
				maintId = todel.maint.id;
			}
			flash("success","Delete successful.");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("History delete error",e.fillInStackTrace());
			flash ("error", "Can't delete record. Please ensure you have proper rights to do "
					+ "this operation. Also, ensure you have deleted all measurement records before deleting a history record. If you still get an error"
					+ " after deleting parameter measurements, please contact system administrator");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		if(maintId!=-1L)
			return redirect(routes.MaintenanceApplication.maintenanceIndex(maintId));
		else
			return redirect(routes.HistoryApplication.list(parentLevel,parentId,0,"eventType","asc",""));
		//return redirect(routes.HistoryApplication.calendar(parentLevel,  parentId, calType));
				
	}
	
}