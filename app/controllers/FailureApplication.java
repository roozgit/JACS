package controllers;

import static play.data.Form.form;

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
import models.Subunits;
import models.Users;
import models.equipment.Equipments;
import models.failure.FailureCauses;
import models.failure.FailureMechanisms;
import models.failure.FailureModes;
import models.failure.Failures;
import models.maintenance.MaintenanceCategories;
import models.maintenance.Maintenances;
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
import com.avaje.ebean.Expr;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SubjectPresent
public class FailureApplication extends Controller {
	
	/**
     * This result directly redirect to equipmentClass list home.
     */
    public static final Result GO_HOME_FM = redirect(
        routes.FailureApplication.failureModeList(0, "typeNumber", "asc", "")
    );
    
    public static Result initCausalityTree (Integer parentLevel, Long parentId) {
    	ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	String field="parentInstallation";
    	switch(parentLevel) {
		case 3:
			field = "parentInstallation.id";
			break;
		case 4:
			field = "parentPlant.id";
			break;
		case 5:
			field = "parentSection.id";
			break;
		case 6:
			field = "parentEquipment.id";
			break;
		case 7:
			field = "parentSubunit.id";
			break;
		case 8:
			field = "parentComponent.id";
			break;
		}
    	for (Failures f : Failures.find.where().eq("failHistory."+field,parentId).findList()) {
    		ObjectNode node = Json.newObject();
    		node.put("title", f.failureMode.failureModeCode+ "-" +f.comments);
    		node.put("key", f.id.toString());
    		node.put("folder",true);
    		node.put("lazy", true);
    		node.put("href",controllers.routes.FailureApplication.failureIndex(parentLevel, parentId, f.id).toString());
    		allnodes.add(node);
    		}
    	return ok(play.libs.Json.toJson(allnodes));
        }
    
    /**
     * 
     * @param parentLevel
     * @param parentId
     * @param faId
     * @return
     */
    	public static Result fillCausalityTree(Long faId) {
    		ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    		ObjectNode node = Json.newObject();
    		Failures f= Failures.find.byId(faId).parentFailure;
    		if(f!=null) {
    			node.put("title", f.failureMode.failureModeCode+ "-" +f.comments);
        		node.put("key", f.id.toString());
        		node.put("folder",true);
        		node.put("lazy", true);
        		if(f.failHistory.parentInstallation!=null)
        			node.put("href",controllers.routes.FailureApplication.failureIndex(3, f.failHistory.parentInstallation.id, f.id).toString());
        		if(f.failHistory.parentPlant!=null)
        			node.put("href",controllers.routes.FailureApplication.failureIndex(4, f.failHistory.parentPlant.id, f.id).toString());
        		if(f.failHistory.parentSection!=null)
        			node.put("href",controllers.routes.FailureApplication.failureIndex(5, f.failHistory.parentSection.id, f.id).toString());
        		if(f.failHistory.parentEquipment!=null)
        			node.put("href",controllers.routes.FailureApplication.failureIndex(6, f.failHistory.parentEquipment.id, f.id).toString());
        		if(f.failHistory.parentSubunit!=null)
        			node.put("href",controllers.routes.FailureApplication.failureIndex(7, f.failHistory.parentSubunit.id, f.id).toString());
        		if(f.failHistory.parentComponent!=null)
        			node.put("href",controllers.routes.FailureApplication.failureIndex(8, f.failHistory.parentComponent.id, f.id).toString());
        		allnodes.add(node);
    		}
    		return ok(play.libs.Json.toJson(allnodes));    		
    	}
    
	/**
	 * shows ONE failure item
	 * @param
	 * @return
	 */
	public static Result failureIndex(Integer parentLevel, Long parentId, Long failureId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.failures.failureIndex.render(
				parentLevel,
				parentId,
				Failures.find.byId(failureId)
				));

	}
	
	public static Result failureCausesList() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.failures.failureCausesList.render(
				FailureCauses.find.all()
				));
		
	}
	
	public static Result failureMechanismsList() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.failures.failureMechanismsList.render(
				FailureMechanisms.find.all()
				));
	}
	
	/**
	 * Display create  form
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("requester"), @Group("planner")})
	public static Result createFailure(Integer parentLevel, Long parentId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Failures> newFailureForm = form(Failures.class);
		return ok(views.html.failures.failureCreateForm.render(
					parentLevel, parentId, newFailureForm
					));
	}
	
	/**
	 * Display create  form
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("requester"), @Group("planner")})
	public static Result createEquipmentFailure(Long equipmentId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Failures> newFailureForm = form(Failures.class);
		return ok(views.html.failures.failureCreateForm.render(
					6, equipmentId, newFailureForm
					));
	}
	
	/**
	 * save new
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("requester"), @Group("planner")})
	public static Result save(Integer parentLevel, Long parentId) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (Failures.class.getField(key).getType().toString()
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
		Form<Failures> newFailureForm = new Form<Failures>(Failures.class)
				.bind(newData);
			
			if (newFailureForm.hasErrors()) {
				return badRequest(views.html.failures.failureCreateForm.render(
						parentLevel, parentId, newFailureForm
						));
			}
			Failures fa = newFailureForm.get();
			History fh = new History();
			//Begin saving
			Ebean.beginTransaction();
			try
			{
			switch(parentLevel) {
			case 3:
				fh.parentInstallation = Installations.find.byId(parentId);
				if(fh.parentInstallation==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 4:
				fh.parentPlant = Plants.find.byId(parentId);
				if(fh.parentPlant==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 5:
				fh.parentSection = Sections.find.byId(parentId);
				if(fh.parentSection==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 6:
				fh.parentEquipment = Equipments.find.byId(parentId);
				if(fh.parentEquipment==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 7:
				fh.parentSubunit = Subunits.find.byId(parentId);
				if(fh.parentSubunit==null) throw new ValidationException("Null parent can't be saved!");
				break;
			case 8:
				fh.parentComponent = Components.find.byId(parentId);
				if(fh.parentComponent==null) throw new ValidationException("Null parent can't be saved!");
				break;
			}
			fa.save();
			fa.saveManyToManyAssociations("failureCauses");
			fa.update();
			fh.start=new Date();
			fh.end = new Date(fh.start.getTime()+60000);
			fh.eventType=EventTypes.FAILURE;
			fh.systemEvent=true;
			fh.allDay=false;
			fh.isHappened=true;
			fh.fail= fa;
			fh.registrar=Users.findByUserName(session().get("userName"));
			fh.save();
			flash("success","Log added");
			Ebean.commitTransaction();
			} catch (Exception e) {
				Logger.error("Failure save error",e.fillInStackTrace());
				flash("error","Can't save failure");
				Ebean.rollbackTransaction();
			} finally {
				Ebean.endTransaction();
			}
			return redirect(
					routes.HistoryApplication.editHistory(fh.id,"Failures"));
	}
   
	/**
	 * Display edit  form
	 * @param Id
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("requester"), @Group("planner")})
	public static Result editFailure(Integer parentLevel, Long parentId, Long failureId) {
			
			response().setHeader("Cache-Control",
					"no-store, no-cache, must-revalidate");
			response().setHeader("Pragma", "no-cache");
			Failures fa = Failures.find.byId(failureId);
			Form<Failures> editFailureForm = form(Failures.class).fill(fa);
			return ok(views.html.failures.failureEdit.render(
					parentLevel, parentId, failureId, editFailureForm
					));

	}
	
	/**
	 * update
	 * @param Id
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("requester"), @Group("planner")})
	public static Result update(Integer parentLevel, Long parentId, Long failureId) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (Failures.class.getField(key).getType().toString()
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
		Form<Failures> editFailure = new Form<Failures>(Failures.class)
				.bind(newData);
			
			if (editFailure.hasErrors()) {
				return badRequest(views.html.failures.failureEdit.render(
						parentLevel, parentId, failureId, editFailure
						));
			}
			
		Failures fa = editFailure.get();
		
		Ebean.beginTransaction();
		try {
			/*Ebean
	         .createUpdate(Event.class, "UPDATE event SET date=null where id=:id")
	         .setParameter("id", page.id).execute();  
	    }*/
			fa.id=failureId;
			if(fa.parentFailure.id!=null)
				if(isChained(failureId,fa.parentFailure.id) || fa.parentFailure.id==failureId) throw new ValidationException("Circular failure cause is unacceptable!");
			fa.saveManyToManyAssociations("failureCauses");
			fa.update(failureId);
			Ebean.commitTransaction();
			flash("success","Failure record updated");
		} catch (Exception ex) {
			Logger.error("Failure update error",ex.fillInStackTrace());
			flash("error",ex.getMessage());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.FailureApplication.failureIndex(parentLevel, parentId, failureId));
	}
	
	private static boolean isChained(Long faId,Long newParentId) {
		Failures head = Failures.find.byId(faId);
		Failures child = Failures.find.byId(newParentId);
		while (child!=null) {
			if(child.parentFailure==null) return false;
			else {
				if(child.parentFailure.id==head.id) return true;
				else {
					child = child.parentFailure;
				}
			}
				
		}
		return true;
	}
	
	/**
	 * Returns a list of 25 failuremodes for use in equipmentClassList or filtered queries
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
	public static Result failureModeList(int page, String sortBy, String order, String filter) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		int ppid = 30;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 30;
			}
		}
			return ok(
					views.html.failures.failureModeList.render(
							FailureModes.page(page, ppid, sortBy , order, filter),
							sortBy, order, filter
							));
		}
		
	
	/**
	 * Returns a list of 50 maints for use in maintenanceList or filtered queries
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
	public static Result list(Long equipmentId, int page, String sortBy, String order, String filter1, String filter2
			, String filter3, String filter4, String filter5,
			String filter6, String filter7, String filter8, String filter9) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		int ppid = 15;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 50;
			}
		}
		return ok(
				views.html.failures.failureList.render(equipmentId,
						Failures.page(equipmentId, page, ppid, sortBy , order, filter1, filter2, filter3
								, filter4, filter5, filter6, filter7,filter8,filter9),
						sortBy, order, filter1, filter2, filter3,
						filter4, filter5, filter6, filter7,filter8,filter9
						));
	}
		/**
		 * Displays equipmentClass creates form
		 * @return form display
		 */
		@Restrict({@Group("admin"),@Group("creator")})
		public static Result createFailureMode() {
			response().setHeader("Cache-Control",
					"no-store, no-cache, must-revalidate");
			response().setHeader("Pragma", "no-cache");
			Form<FailureModes> newFailureModeForm = form(FailureModes.class);
			return ok(views.html.failures.createFailureModeForm.render(newFailureModeForm));
		}

		/**
		 * Saves a equipmentClass
		 * @return GO_HOME
		 */
		@Restrict({@Group("admin"),@Group("creator")})
		public static Result failureModeSave() {
			Form<FailureModes> newFailureModeForm = form(FailureModes.class)
					.bindFromRequest();
			if (newFailureModeForm.hasErrors()) {
				return badRequest(views.html.failures.createFailureModeForm.render(newFailureModeForm));
			}
			Ebean.beginTransaction();
			try {
				newFailureModeForm.get().save();
				flash("success", "New failure mode has been created");
			Ebean.commitTransaction();
			} catch (Exception e) {
				Logger.error("FailureMode save error",e.fillInStackTrace());
				flash("error", "Can't save failure mode");
				Ebean.rollbackTransaction();
			} finally {
				Ebean.endTransaction();
			}
			return GO_HOME_FM;
		}
		
		/**
		 * display edit equipmentClass form
		 * @param id Id of equipmentClass
		 * @return form display
		 */
		@Restrict({@Group("admin"),@Group("editor")})
		public static Result editFailureMode(Long id) {
			response().setHeader("Cache-Control",
					"no-store, no-cache, must-revalidate");
			response().setHeader("Pragma", "no-cache");
			Form<FailureModes> failureModeForm = form(FailureModes.class).fill(
					FailureModes.find.byId(id));
			return ok(views.html.failures.editFailureMode.render(id, failureModeForm));
		}

		/**
		 * Updates equipmentClass
		 * @param id Id of equipmentClass
		 * @return GO_HOME
		 */
		@Restrict({@Group("admin"),@Group("editor")})
		public static Result failureModeUpdate(Long id) {
			Form<FailureModes> updateForm = new Form<FailureModes>(FailureModes.class)
					.bindFromRequest();

			if (updateForm.hasErrors()) {
				return badRequest(views.html.failures.editFailureMode.render(id,updateForm));
			}
			Ebean.beginTransaction();
					try {
						FailureModes fm = updateForm.get();
						fm.id = id;
						fm.update(id);
						flash("success", "Failure mode has been updated");
						Ebean.commitTransaction();
					} catch (Exception e) {
						Logger.error("FailureMode update error",e.fillInStackTrace());
						flash("error", "Can't update failure mode");
						Ebean.rollbackTransaction();
					} finally {
						Ebean.endTransaction();
					}
			
			return GO_HOME_FM;
		}
		
		/**
		 * FAILURE FILLER
		 * @param sectionId
		 * @param equipmentId
		 * @return
		 */
		public static Result failureFiller(Long sectionId, Long equipmentId) {
			ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
			Long realSection = sectionId;
			if(equipmentId>0) realSection=-1L;
			List<Failures> jsonFails = Failures.find.where().or(
					Expr.eq("failHistory.parentEquipment.id",equipmentId),
					Expr.eq("failHistory.parentSection.id",realSection))
					.findList();
			
			for (Failures f : jsonFails) {
				ObjectNode node = Json.newObject();
				node.put("id",f.id);
				node.put("value",f.id.toString() +"."+f.failureMode.failureModeCode+"---"+f.comments
						+myUtils.DateTimeUtils.getIranianDateTime(f.failHistory.start));
				allnodes.add(node);
					} 
			return ok(play.libs.Json.toJson(allnodes));
		}
		
		/**
	     * 
	     * @return
	     */
	    public static Result flotChartFailureJson() {
			List<String[]> dataPoints = new ArrayList<String[]>();
			ObjectNode node = Json.newObject();
	    	node.put("label","Failure mode chart");
	    	for(FailureModes fmo : FailureModes.find.all()) {
	    		String[] point = new String[2];
	    		point[0] = fmo.failureModeCode.toString();
	    		int pv = History.find.where()
			    		.eq("fail.failureMode.id",fmo.id).findRowCount();
		    	if(pv!=0) point[1] = String.valueOf(pv);
		    	dataPoints.add(point);		    	
	    	}
	    	node.put("data",play.libs.Json.toJson(dataPoints));
	        return ok(play.libs.Json.toJson(node));
	    }
	    
	    /**
	     * 
	     * @param equipmentId
	     * @return
	     */
	    @BodyParser.Of(BodyParser.Json.class)
	    public static Result mtbf(Long equipmentId) {
	    	ObjectNode node = Json.newObject();
	    	Float mtbf = 0.0f;
	    	Float mttf = 0.0f;
	    	Equipments target = Equipments.find.byId(equipmentId);
	    	List<Failures> efails = Failures.failureQueryBuilder(equipmentId, "", "", "", "", "", "", "", "", "").orderBy("failHistory.start asc").findList();
	    	Integer numOfFails = efails.size();
	    	Long totalUpTimeHoursBetweenFailures = 0L;
	    	if(numOfFails==0) return ok("undefined");
	    	Date startDate = efails.get(0).failHistory.end;
	    	
	    	for(int f=1;f<numOfFails;f++) {
	    		totalUpTimeHoursBetweenFailures += History.upTime(6, equipmentId, startDate,efails.get(f).failHistory.start);
	    		startDate = efails.get(f).failHistory.end;
	    	}
	    	
	    	Long totalOperationalTime = History.upTime(6,equipmentId,target.currentServiceStartDate,new Date());
	    	
	    	//mean calculation
	    	mtbf = Float.valueOf(totalUpTimeHoursBetweenFailures/numOfFails);
	    	mttf = Float.valueOf(totalOperationalTime/numOfFails);
	    	
	    	node.put("mtbf",mtbf.toString());
	    	node.put("mttf",mttf.toString());
	    	
	    	return ok(play.libs.Json.toJson(node));
	    }
}