package controllers;


import static play.data.Form.form;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.SecurityRole;
import models.Shifts;
import models.Users;
import myUtils.DateTimeUtils;

import org.joda.time.DateTime;

import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SubjectPresent
public class ShiftApplication extends Controller {
	
	/**
	 * Converts dates to Gregorian for save in db
	 * @param his
	 */
	public static void parseDatesToGregorian(Shifts his) {
		his.start = DateTimeUtils
				.getGregorianDateTime(his.start);
		his.end = DateTimeUtils
				.getGregorianDateTime(his.end);
	}
	
	/**
	 * converts dates to Shamsi to show in edit fill forms
	 * @param his
	 */
	public static void parseDateTimesAsIranianDateTimes(Shifts his) {
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
	 * shows ONE history item
	 * @param historyId
	 * @return
	 */
	public static Result shiftIndex(Long shiftId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		
		return ok(views.html.shift.shiftIndex.render(
				shiftId,
				Shifts.find.byId(shiftId)
				));
				
	}
	
	/**
     * Returns list of history events for calendar view
     * @param start Long Timestamp of current view start
     * @param end Long Timestamp of current view end
     * @return Result
     */
    public static Result json(Long userId) {
    	
    	List<Shifts> resultList = Shifts.find.where().eq("user.id", userId).findList();
        ArrayList<ObjectNode> allShiftEvents = new ArrayList<ObjectNode>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        for (Shifts shiftEvent : resultList) {
        	ObjectNode node = Json.newObject();
        	node.put("id", shiftEvent.id);
        	node.put("title", shiftEvent.workType.workType);
        	node.put("start", df.format(shiftEvent.start));
        	node.put("end", df.format(shiftEvent.end));
        	node.put("allDay", true);
        	node.put(
            		"url", controllers.routes.ShiftApplication.shiftIndex(shiftEvent.id).toString());
            allShiftEvents.add(node);
        }
        return ok(play.libs.Json.toJson(allShiftEvents));
    }
	
    /**
     * Displays full calendar
     * @return Result
     */
    public static Result shiftCalendar(Long userId) {
    	response().setHeader("Cache-Control",
    			"no-store, no-cache, must-revalidate");
    	response().setHeader("Pragma", "no-cache");
        return ok(views.html.shift.shiftCalendar.render(userId, "Shift Calendar"));
    }
	
	/**
	 * Display create history form
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	public static Result createShift(Long userId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Shifts> newShiftForm = form(Shifts.class);
		return ok(views.html.shift.shiftCreateForm.render(
					userId, newShiftForm
					));

	}
	
	/**
	 * save new history
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	public static Result save(Long userId) {
		
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (Shifts.class.getField(key).getType()
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
						if(Shifts.class.getField(key).getType()
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
			Form<Shifts> newShiftForm = form(Shifts.class).bind(newData);
			if (newShiftForm.hasErrors()) {
				return badRequest(views.html.shift.shiftCreateForm.render(
						userId, newShiftForm
						));
			}
			Shifts shif = newShiftForm.get();
			shif.user=Users.find.byId(userId);
			//Begin saving
				shif.save();
				flash("success","Log added");
			return redirect(
					routes.ShiftApplication.shiftCalendar(userId));
	}

	
    /**
     * Saves in DB date changed by event drag
     * @return Result
     */
    public static Result move() {

        Long id = Long.valueOf(form().bindFromRequest().get("id"));
        int dayDelta = Integer.parseInt(form().bindFromRequest().get("dayDelta"));
        int minuteDelta = Integer.parseInt(form().bindFromRequest().get("minuteDelta"));

        Shifts event = Shifts.find.byId(id);
	        event.start = new DateTime(event.start).plusDays(dayDelta).plusMinutes(minuteDelta).toDate();
	        event.end = new DateTime(event.end).plusDays(dayDelta).plusMinutes(minuteDelta).toDate();
	        event.allDay = Boolean.valueOf(form().bindFromRequest().get("allDay"));
	        event.endsSameDay = endsSameDay(event.start, event.end);
	        event.update();
	        return ok("changed");
    }
    
    /**
     * Saves in DB end date changed by event resize
     * @return Result
     */
    public static Result resize() {

        Long id = Long.valueOf(form().bindFromRequest().get("id"));
        int dayDelta = Integer.parseInt(form().bindFromRequest().get("dayDelta"));
        int minuteDelta = Integer.parseInt(form().bindFromRequest().get("minuteDelta"));

        Shifts event = Shifts.find.byId(id);
        event.end = new DateTime(event.end).plusDays(dayDelta).plusMinutes(minuteDelta).toDate();
        event.endsSameDay = endsSameDay(event.start, event.end);
        event.update();
        return ok("changed");
        
        
    }
    
	/**
	 * Display edit history form
	 * @param historyId
	 * @return
	 */
	public static Result editShift(Long shiftId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
			Shifts shif = Shifts.find.byId(shiftId);
			parseDateTimesAsIranianDateTimes(shif);
			Form<Shifts> editShiftForm = form(Shifts.class).fill(shif);
			Users curu = Users.findByUserName(session().get("userName"));
	        if(curu.roles.contains(SecurityRole.findByName("admin"))) {
			return ok(views.html.shift.shiftEdit.render(
					shiftId, editShiftForm
					));
			} else {
				flash("error","Only authorized personnel can edit shifts");
				return redirect(routes.ShiftApplication.shiftIndex(shiftId));
			}

	}
	
	/**
	 * update history
	 * @param historyId
	 * @return
	 */
	public static Result update(Long shiftId) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (Shifts.class.getField(key).getType()
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
						if(Shifts.class.getField(key).getType()
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
			Form<Shifts> editShift = form(Shifts.class).bind(newData);
			
			if (editShift.hasErrors()) {
				return badRequest(views.html.shift.shiftEdit.render(
						shiftId, editShift
						));
			}
						
			Shifts shif = editShift.get();
			shif.update(shiftId);
			flash("success","History updated");
			return redirect(routes.HistoryApplication.historyIndex(shiftId));
	}
	
	/**
	 * 
	 * @param id
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	public static Result deleteShift(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Shifts.find.ref(id).delete();
		flash("success","Delete successful.");
		Ebean.commitTransaction();
		return ok("delete done");
	}
}
