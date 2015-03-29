package controllers;

import static play.data.Form.form;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Blobs;
import models.EventTypes;
import models.History;
import models.OperationStates;
import models.Sections;
import models.equipment.Equipments;
import myUtils.DateTimeUtils;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Query;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SubjectPresent
public class EquipmentApplication extends Controller {

	/**
	 * converts shamsi dates in Equipments class to Gregorian to save in DB
	 * 
	 * @param eq
	 *            the equipment to be saved to DB
	 */
	public static void parseDatesToGregorian(Equipments eq) {
		eq.initialCommissioningDate = DateTimeUtils
				.getGregorianDateTime(eq.initialCommissioningDate);
		eq.currentServiceEndDate = DateTimeUtils
				.getGregorianDateTime(eq.currentServiceEndDate);
		eq.currentServiceStartDate = DateTimeUtils
				.getGregorianDateTime(eq.currentServiceStartDate);
		eq.guaranteeEndDate = DateTimeUtils
				.getGregorianDateTime(eq.guaranteeEndDate);
		eq.manufactureDate = DateTimeUtils
				.getGregorianDateTime(eq.manufactureDate);
		eq.purchaseDate = DateTimeUtils.getGregorianDateTime(eq.purchaseDate);
	}

	/**
	 * converts Gregorian dates in db to shamsi dates to display in views
	 * 
	 * @param eq
	 *            Equipment to be displayed
	 */
	public static void parseDatesAsIranianDates(Equipments eq) {
		eq.initialCommissioningDate = DateTimeUtils
				.getIranianDateAsDate(eq.initialCommissioningDate);
		eq.currentServiceStartDate = DateTimeUtils
				.getIranianDateAsDate(eq.currentServiceStartDate);
		eq.currentServiceEndDate = DateTimeUtils
				.getIranianDateAsDate(eq.currentServiceEndDate);
		eq.guaranteeEndDate = DateTimeUtils
				.getIranianDateAsDate(eq.guaranteeEndDate);
		eq.manufactureDate = DateTimeUtils
				.getIranianDateAsDate(eq.manufactureDate);
		eq.purchaseDate = DateTimeUtils.getIranianDateAsDate(eq.purchaseDate);
	}
	
	/**
	 * 
	 * @param equipmentId
	 * @return
	 */
	public static Long calculateWorkHours(Long equipmentId) {
		Equipments ind = Equipments.find.byId(equipmentId);
		long workHours = -1L;
		Date then = ind.currentServiceStartDate;
		if(then!=null) {
			Date now = new Date();
			long eventDuration = 0;
			workHours = now.getTime()-then.getTime();
			String qusql ="(state = :s1 OR state = :s2 OR state = :s3 OR state = :s4) AND "
					+ "isHappened = TRUE";
			Query<History> hQuery = Ebean.createQuery(History.class);
			hQuery.where(qusql)
				.setParameter("s1", OperationStates.STOPPED)
				.setParameter("s2", OperationStates.FAILED)
				.setParameter("s3", OperationStates.COLD_STANDBY)
				.setParameter("s4", OperationStates.OUT_OF_SERVICE);
			
			List<History> intersecting = hQuery.where()
					.eq("parentEquipment.id",equipmentId)
					.or(
	                Expr.and(
	                        Expr.lt("start", then),
	                        Expr.gt("end", now)
	                ),
	                Expr.or(
	                        Expr.and(
	                                Expr.gt("start", then),
	                                Expr.lt("start", now)
	                        ),
	                        Expr.and(
	                                Expr.gt("end", then),
	                                Expr.lt("end", now)
	                        )
	                )
	        ).findList();
			
			for(History h : intersecting) {
				eventDuration = h.end.getTime()-h.start.getTime();
				if(h.start.getTime()<then.getTime())
					eventDuration = h.end.getTime()-then.getTime();
				if(h.end.getTime()>now.getTime())
					eventDuration = now.getTime()-h.start.getTime();
				workHours = workHours - eventDuration;
			}
		}
		return workHours/3600000L;
	}
	/**
	 * Information about one equipment
	 * 
	 * @param id
	 *            Id of the equipment
	 * @return Result of render(Equipments.find.byId(id))
	 */
	public static Result equipmentIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.equipments.equipmentIndex.render(
				Equipments.find.byId(id)
				));
	}

	/**
	 * Returns a list of 10 equipments for use in equipmentList view
	 * 
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
	public static Result list(Long parentSectionId, int page, String sortBy,
			String order, String filter) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		int ppid = 10;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 20;
			}
		}
		if(Sections.find.byId(parentSectionId)==null) return badRequest("No such parent section");
		else
		return ok(views.html.equipments.equipmentList.render(parentSectionId,
				Equipments.page(parentSectionId, page, ppid, sortBy, order,
						filter), sortBy, order, filter));
	}

	/**
	 * Lists a page of equipments ordered by name and ascending
	 * 
	 * @return first page of list
	 */
	public static Result equipmentList(Long parentSectionId) {
		return redirect(routes.EquipmentApplication.list(parentSectionId, 0, "name", "asc", ""));
	}

	/**
	 * Displays section create form
	 * 
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result createEquipment(Long parentSectionId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Equipments> newEquipmentForm = form(Equipments.class);
		return ok(views.html.equipments.createEquipmentForm.render(
				parentSectionId, newEquipmentForm));

	}

	/**
	 * Saves a equipment
	 * 
	 * @return table of all equipments
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result save(Long parentSectionId) {
		Form<Equipments> newEquipmentForm = form(Equipments.class)
				.bindFromRequest();
		if (newEquipmentForm.hasErrors()) {
			return badRequest(views.html.equipments.createEquipmentForm.render(
					parentSectionId, newEquipmentForm));
		}
		Ebean.beginTransaction();
		try {
			Equipments e = newEquipmentForm.get();
			e.section = Sections.find.byId(parentSectionId);
			e.save();
			flash("success", "Equipment " + newEquipmentForm.get().name
					+ " has been created");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Equipment save error",e.fillInStackTrace());
			flash("error", "Can't save equipment");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return equipmentList(parentSectionId);
	}

	/**
	 * display edit equipment form
	 * 
	 * @param id
	 *            Id of equipment
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result editEquipment(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Equipments fillerEquipment = Equipments.find.byId(id);
		
		if(fillerEquipment==null)
			return badRequest("No such equipment"); else {
		// This method causes the dates in form to be displayed shamsi
		parseDatesAsIranianDates(fillerEquipment);
		
		Form<Equipments> updateForm = form(Equipments.class).fill(
				fillerEquipment);
		return ok(views.html.equipments.editEquipment.render(id, updateForm));
		}
	}

	/**
	 * Updates equipment
	 * 
	 * @param id
	 *            Id of equipment
	 * @return table of all equipments
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
						if (Equipments.class.getField(key).getType().toString()
								.compareTo("interface java.util.List") == 0) {
							newData.put(key + "[0]" + ".id", value[0]);
						} else {
							newData.put(key, value[0]);
						}
					} catch (NoSuchFieldException | SecurityException e) {
						newData.put(key, value[0]);
					}
					try {
						if(Equipments.class.getField(key).getType()
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
		Form<Equipments> updateForm = new Form<Equipments>(Equipments.class)
				.bind(newData);
		if (updateForm.hasErrors()) {
			return badRequest(views.html.equipments.editEquipment.render(id,updateForm));
		}
		Ebean.beginTransaction();
		try {
			Equipments e = updateForm.get();
			e.id = id;
			e.saveManyToManyAssociations("files");
			e.saveManyToManyAssociations("specialTools");
			e.update(id);
			
			if (e.purchaseDate == null){
			       Ebean
			         .createUpdate(Equipments.class, "UPDATE equipments SET purchase_date=null where id=:id")
			         .setParameter("id", id).execute();
			    }
			
			if (e.manufactureDate == null){
			       Ebean
			         .createUpdate(Equipments.class, "UPDATE equipments SET manufacture_date=null where id=:id")
			         .setParameter("id", id).execute();
			    }
			
			if (e.guaranteeEndDate == null){
			       Ebean
			         .createUpdate(Equipments.class, "UPDATE equipments SET guarantee_end_date=null where id=:id")
			         .setParameter("id", id).execute();
			    }
			
			if (e.currentServiceStartDate == null){
			       Ebean
			         .createUpdate(Equipments.class, "UPDATE equipments SET current_service_start_date=null where id=:id")
			         .setParameter("id", id).execute();
			    }
			
			if (e.currentServiceEndDate == null){
			       Ebean
			         .createUpdate(Equipments.class, "UPDATE equipments SET current_service_end_date=null where id=:id")
			         .setParameter("id", id).execute();
			    }
			
			if (e.initialCommissioningDate == null){
			       Ebean
			         .createUpdate(Equipments.class, "UPDATE equipments SET initial_commissioning_date=null where id=:id")
			         .setParameter("id", id).execute();
			    }
			//End of date nulling block
			
			flash("success", "Equipment " + updateForm.get().name
					+ " has been updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Equipment update error",e.fillInStackTrace());
			flash("error", "Can't update equipment");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.EquipmentApplication.equipmentIndex(id));
	}
	
	/**
	 * Uploads file directly for current equipment
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result upload(Long equipmentId) {
			MultipartFormData body = request().body().asMultipartFormData();
			FilePart uploadedFile = body.getFile("file");
			String un = session().get("userName");
			Equipments eq = Equipments.find.byId(equipmentId);
			try {
				eq.files.add(
						Blobs.find.byId(myUtils.FileUtils.upload(uploadedFile, un)));
				eq.saveManyToManyAssociations("files");
				eq.update();
				
			} catch (IOException e1) {
				Logger.error("Equipment file upload error",e1.fillInStackTrace());
				flash("error","Can't upload file");
				
			} catch(IllegalStateException e2) {
				Blobs fblob = Blobs.find.where().eq("name",uploadedFile.getFilename()).findUnique();
				if(!eq.files.contains(fblob)) {
					eq.files.add(fblob);
					eq.saveManyToManyAssociations("files");
					eq.update();
					flash("success",e2.getMessage()+" File added to data source anyway.");
					
				} else flash("error", e2.getMessage()+ " File already is a data source");
				
			}
		 	return redirect(routes.EquipmentApplication.equipmentIndex(equipmentId) + "#tabs-sources");
	}
	
	/**
	 * Removes association and probably the file itself from equipment-blob many-to-many
	 * @param equipmentId
	 * @param blobId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result deleteAssociation(Long equipmentId, Long blobId) {
		Equipments eq = Equipments.find.byId(equipmentId);
		Ebean.beginTransaction();
		try {
			eq.files.remove(
					Blobs.find.byId(blobId)
					);
			eq.saveManyToManyAssociations("files");
			eq.update(equipmentId);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Equipment blob association removal error",e.fillInStackTrace());
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
			Logger.error("Physical file removal error(Equipments)",e.fillInStackTrace());
		}
		return redirect(routes.EquipmentApplication.equipmentIndex(equipmentId) + "#tabs-sources");
	}
	
	/**
	 * 
	 * @param equipmentId
	 * @param targetSubunitId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor")})
	public static Result copyEquipmentAsComponent(Long equipmentId) {
		DynamicForm requestData = form().bindFromRequest();
		Long targetSubunitId = Long.parseLong(requestData.get("subunit.id"));
		if(Equipments.copyAsComponent(equipmentId, targetSubunitId)==true) {
			flash("success","Equipment successfully moved to subunit as component");
		} else {
			flash("error","Error! Equipment was not moved to subunit as a component!");
		}
		return redirect(routes.SubunitApplication.subunitIndex(targetSubunitId));
	}
	
	/**
	 * Returns copy equipment result
	 * @param equipmentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor")})
	public static Result copyEquipment(Long equipmentId) {
		DynamicForm requestData = form().bindFromRequest();
		Long targetSectionId = Long.parseLong(requestData.get("section.id"));
		if(Equipments.copy(equipmentId, targetSectionId)==true) {
			flash("success","Equipment successfully copied");
		} else {
			flash("error","Error! Equipment was not copied!");
		}
		return redirect(routes.EquipmentApplication.equipmentIndex(equipmentId));
	}
	
	@Restrict({ @Group("admin")})
	public static Result delete(Long equipmentId) {
		Long sid = Equipments.find.byId(equipmentId).section.id;
		Equipments.find.ref(equipmentId).delete();
		return equipmentList(sid);
	}
	
	/**
     * 
     * @return
     */
    public static Result flotEquipmentUpTimeJson(Long id) {
		List<Long[]> dataPoints = new ArrayList<Long[]>();
		ObjectNode node = Json.newObject();
    	node.put("label","Up time chart");
    	
    	
    	GregorianCalendar cal =   new GregorianCalendar();
    	cal.setGregorianChange(new Date(Long.MIN_VALUE));
    	
    	for(History h : History.find.where().eq("parentEquipment.id",id).eq("isHappened", true).orderBy("start asc").findList()) {
    		Long[] point = new Long[2];
        	
        	Date sdt = myUtils.DateTimeUtils.getIranianDateTimeAsDate(h.start);
        	Integer[] exdp = extractDateParts(sdt);
        	cal.set(exdp[0],exdp[1],exdp[2],exdp[3],exdp[4],exdp[5]);
    		point[0] = cal.getTimeInMillis();
    		if(h.state.getState()==true) {
    			if(h.eventType.equals(EventTypes.MAINTENANCE)) point[1] = 1L; else point[1] = 2L;
    			} else {
    				if(h.eventType.equals(EventTypes.MAINTENANCE)) point[1] = -1L; else point[1] = -2L;
    				}
	    	dataPoints.add(point);
	    	point = new Long[2];
	    	sdt = myUtils.DateTimeUtils.getIranianDateTimeAsDate(h.end);
	    	exdp = extractDateParts(sdt);
        	cal.set(exdp[0],exdp[1],exdp[2],exdp[3],exdp[4],exdp[5]);
    		point[0] = cal.getTimeInMillis();
    		if(h.state.getState()==true) {
    			if(h.eventType.equals(EventTypes.MAINTENANCE)) point[1] = 1L; else point[1] = 2L;
    			} else {
    				if(h.eventType.equals(EventTypes.MAINTENANCE)) point[1] = -1L; else point[1] = -2L;
    				}
	    	dataPoints.add(point);
	    	
    		Long[] pointx = new Long[2];
    		sdt = myUtils.DateTimeUtils.getIranianDateTimeAsDate(h.end);
    		exdp = extractDateParts(sdt);
        	cal.set(exdp[0],exdp[1],exdp[2],exdp[3],exdp[4],exdp[5]);
    		pointx[0] = cal.getTimeInMillis();
    		pointx[1]=0L;
    		dataPoints.add(pointx);
	    	
    	}
    	node.put("data",play.libs.Json.toJson(dataPoints));
        return ok(play.libs.Json.toJson(node));
    }
 
    /**
     * a function to extract parts of a java date object as integer
     * @param inDate
     * @return
     */
    private static Integer[] extractDateParts(Date inDate) {
    	Integer[] result = new Integer[6];
    	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	String strDateTime = dateFormat.format(inDate);
		String[] dt = strDateTime.split(" ");
		String[] strDate = dt[0].split("/");
		String[] strTime = dt[1].split(":");
		result[0]=Integer.valueOf(strDate[0]);
		result[1]=Integer.valueOf(strDate[1])-1;
		result[2]=Integer.valueOf(strDate[2]);
		result[3]=Integer.valueOf(strTime[0]);
		result[4]=Integer.valueOf(strTime[1]);
		result[5]=Integer.valueOf(strTime[2]);
		return result;
    }
    
}
