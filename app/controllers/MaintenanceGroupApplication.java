package controllers;

import static play.data.Form.form;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.History;
import models.Parts;
import models.RepairTools;
//import views.html.*;
import models.maintenance.MaintenanceGroups;
import models.maintenance.MaintenanceStatus;
import models.maintenance.Maintenances;
import models.maintenance.MaintenancesParts;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;

@SubjectPresent
public class MaintenanceGroupApplication extends Controller {

	public static Result maintenanceGroupList() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.maintenances.maintenanceGroupList.render(MaintenanceGroups.find.all()));
	}
	
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result maintenanceGroupIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		
		String rcheck =	checkMembersPlannedDates(id);
		if(rcheck.length()!=0)
			flash ("error","Jobs with no planned dates removed:"+rcheck);
		
		MaintenanceGroups mgroup = MaintenanceGroups.find.byId(id);
			
			Ebean.createUpdate(MaintenanceGroups.class, "UPDATE maintenance_groups SET percent_complete=:pc WHERE id=:id")
	         .setParameter("pc", calCompletionPercent(id))
	         .setParameter("id", id)
		     .execute();
		return ok(views.html.maintenances.maintenanceGroupIndex.render(
				mgroup
				));
	}
	
	
	/**
	 * 
	 * @param gid
	 * @return
	 */
	private static float calCompletionPercent(Long gid) {
		Integer nofin=0;
		MaintenanceGroups mg = MaintenanceGroups.find.byId(gid);
		if(mg.memberMaints==null) return 0f;
		if(mg.memberMaints.size()==0) return 0f;
		for(Maintenances mm : mg.memberMaints) {
			if(mm.maintenanceStatus==MaintenanceStatus.FINISHED) 
				nofin++;
		}
		return Float.valueOf((float) 100*( (float) nofin/(mg.memberMaints.size())));
	}
	
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result createMaintenanceGroup() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenanceGroups> newMGForm = form(MaintenanceGroups.class);
		return ok(views.html.maintenances.createMaintenanceGroupForm.render(newMGForm,
				"New maintenance project"));

	}
	
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result editMaintenanceGroup(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<MaintenanceGroups> MGForm = form(MaintenanceGroups.class)
				.fill(MaintenanceGroups.find.byId(id));
		return ok(views.html.maintenances.editMaintenanceGroup.render(id, MGForm));
	}
	
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result update(Long id) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (MaintenanceGroups.class.getField(key).getType()
								.toString()
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
		
		Form<MaintenanceGroups> updateForm = new Form<MaintenanceGroups>(
				MaintenanceGroups.class).bind(newData);
		if (updateForm.hasErrors()) {
			return badRequest(views.html.maintenances.editMaintenanceGroup.render(
					id, updateForm));
		}
		MaintenanceGroups mg = updateForm.get();
		mg.id = id;
		Ebean.beginTransaction();
		try {
			List<Maintenances> membersOfGroup = mg.memberMaints;
			for(int i=0; i <membersOfGroup.size();i++) {
				Maintenances tMaint = Maintenances.find.byId(membersOfGroup.get(i).id);
				tMaint.maintenanceGroup = MaintenanceGroups.find.byId(mg.id);
				tMaint.update();
			}
			
			mg.update(id);
			Ebean.commitTransaction();
			flash("success","Selected maintenances added to group");
		} catch (Exception ex) {
			Logger.error("Group update failed",ex.fillInStackTrace());
			flash("error","Update failed");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.MaintenanceGroupApplication.maintenanceGroupIndex(id));
	}
	
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result save() {
		Form<MaintenanceGroups> newMGForm = form(MaintenanceGroups.class).bindFromRequest();
		if (newMGForm.hasErrors()) {
			return badRequest(views.html.maintenances.createMaintenanceGroupForm.render(
					newMGForm, "Error!"));
		}
		try {
			newMGForm.get().save();
		} catch (Exception ex) {
			return badRequest(views.html.maintenances.createMaintenanceGroupForm.render(
					newMGForm, ex.getMessage() + " Error!"));
		}
		return redirect(routes.MaintenanceGroupApplication.maintenanceGroupList());
	}
	
	/**
	 * 
	 * @param projectId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result addPartToGroupMaints(Long projectId) {
		DynamicForm requestData = Form.form().bindFromRequest();
		
		Long partId=-1L;
		Float quantity =0.0f;
		
		try {
		  partId = Long.valueOf(requestData.get("partId"));		
		  quantity = Float.valueOf(requestData.get("partQuantity"));
		} catch (Exception x){
		  Logger.error("Form values not valid",x.fillInStackTrace());
		  quantity=0.0f;
		  partId=-1L;
		}
		if(quantity.compareTo(0f)==0 || partId==-1L) {
			flash("error","Form values not valid");
			return redirect(routes.MaintenanceGroupApplication.maintenanceGroupIndex(projectId));
		}
		Ebean.beginTransaction();
		try {
			for(Maintenances ma : MaintenanceGroups.find.byId(projectId).memberMaints) {
				MaintenancesParts mapa = new MaintenancesParts();
				mapa.maintenance = ma;
				mapa.part = Parts.find.byId(partId);
				mapa.quantity = quantity;
				mapa.stockFlag = false;
				mapa.save();
			}
			flash("success","Part requested for all peojects.");
			Ebean.commitTransaction();	
		} catch (Exception exc) {
			flash("error","Couldn't request part for all maintenances in project");
			Logger.error("Error in maintenace-part creation for maintenance group",exc.fillInStackTrace());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.MaintenanceGroupApplication.maintenanceGroupIndex(projectId));
	}
	
	/**
	 * 
	 * @param projectId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result addToolToGroupMaints(Long projectId) {
		DynamicForm requestData = Form.form().bindFromRequest();
		
		Long toolId=-1L;
		System.out.println(requestData);
		try {
			toolId = Long.valueOf(requestData.get("toolId"));
		} catch (Exception x){
		  Logger.error("Form values not valid",x.fillInStackTrace());
		  toolId=-1L;
		}
		if(toolId==-1L) {
			flash("error","Form values not valid");
			return redirect(routes.MaintenanceGroupApplication.maintenanceGroupIndex(projectId));
		}
		Ebean.beginTransaction();
		try {
			for(Maintenances ma : MaintenanceGroups.find.byId(projectId).memberMaints) {
				ma.tools.add(RepairTools.find.byId(toolId));
				ma.saveManyToManyAssociations("tools");
			}
			flash("success","Part requested for all peojects.");
			Ebean.commitTransaction();	
		} catch (Exception exc) {
			flash("error","Couldn't request tool for all maintenances in project");
			Logger.error("Error in maintenace-tool creation for maintenance group",exc.fillInStackTrace());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.MaintenanceGroupApplication.maintenanceGroupIndex(projectId));
		
	}
	
	/**
	 * 
	 * @param projectId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result shiftDates(Long projectId) {
		DynamicForm requestData = Form.form().bindFromRequest();
		Integer numDays=0;
		
		try {
		  numDays = Integer.valueOf(requestData.get("numDays"));		
		} catch (Exception x){
		  Logger.error("Form values not valide",x.fillInStackTrace());
		  numDays=0;
		}
		for(Maintenances m : MaintenanceGroups.find.byId(projectId).memberMaints) {
			History h = History.find.where().eq("maint.id",m.id).eq("isHappened",false).findUnique();
			h.start = new Date(h.start.getTime()+numDays * 24*3600*1000);
			h.end = new Date(h.end.getTime()+numDays*24*3600*1000);
			h.update();
		}
		return redirect(routes.MaintenanceGroupApplication.maintenanceGroupIndex(projectId));
	}
	
	@Restrict({ @Group("admin"), @Group("planner")})
	public static Result deleteGroup(Long projectId) {
		try {
		MaintenanceGroups.find.ref(projectId).delete();
		} catch (Exception excep) {
			Logger.error("Groupd deletion error",excep.fillInStackTrace());
			flash("error","Couldn't delete project");
			return redirect(routes.MaintenanceGroupApplication.maintenanceGroupList());
		}
		flash("success","Project deleted");
		return redirect(routes.MaintenanceGroupApplication.maintenanceGroupList());
	}
	
	private static String checkMembersPlannedDates(Long gid) {
		String removedMs = new String();
		MaintenanceGroups mg = MaintenanceGroups.find.byId(gid);
		for (Maintenances m :mg.memberMaints) {
			if(History.find.where().eq("maint.id",m.id).eq("isHappened",false).findRowCount()==0) {
				MaintenanceApplication.removeFromGroup(m.id, gid);
				removedMs += m.workOrderSerial+"--";
			}
				
		}
		return removedMs;
	}
	
}
