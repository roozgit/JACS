package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Blobs;
import models.Components;
import models.History;
import models.Installations;
import models.PartsComponents;
import models.Plants;
import models.Sections;
import models.Subunits;
import models.Users;
import models.equipment.Equipments;
import models.failure.Failures;
import models.maintenance.MaintenanceStatus;
import models.maintenance.Maintenances;

import org.joda.time.DateTime;

import play.Logger;
import play.Routes;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class Application extends Controller {
	
	/**
     * This result directly redirect to part list home.
     */
    public static final Result GO_HOME = redirect(
        routes.Application.list(0, "name", "asc", "")
    );
	
	public static Result index() {
		session().clear();
		return redirect(routes.AuthApplication.login());
    }
	
	@SubjectPresent
	public static Result index2() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Users inu = Users.findByUserName(session().get("userName"));
		return ok(views.html.index.render(
					Maintenances.getDashboardMaintenances(inu.id)
				));
	}
	
	@SubjectPresent
	public static Result fastSearch() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		DynamicForm searchData = Form.form().bindFromRequest();
		String sr = searchData.get("searchParam");
		return ok(views.html.fastSearchResults.render(Equipments.find.
				where().or(
						Expr.contains("description",sr),
						Expr.contains("name", sr))
				.orderBy("equipmentClass")
				.findPagingList(500)
				.getPage(0)
				));
	}
	
	/**
	  * Returns a list of 7 blobs
	  * @param page
	  * @param sortBy
	  * @param order
	  * @param filter
	  * @return
	  */
	@SubjectPresent
	 public static Result list(int page, String sortBy, String order, String filter) {
		 response().setHeader("Cache-Control",
					"no-store, no-cache, must-revalidate");
			response().setHeader("Pragma", "no-cache");
		 int ppid = 10;
			if(session().containsKey("pagesize")) {
				try {
				ppid = Integer.valueOf(session().get("pagesize"));
				} catch (Exception eex) {
					ppid = 10;
				}
			}
    		return ok(
    				views.html.uploadFile.render(
    						Blobs.page(page, ppid, sortBy , order, filter),
    						sortBy, order, filter
    						));
    	}
	
	//********************FILE METHODS**************
	@SubjectPresent
	public static Result upload() {
		MultipartFormData body = request().body().asMultipartFormData();
		FilePart uploadedFile = body.getFile("file");
		String un = session().get("userName");
		try {
			Logger.info(myUtils.FileUtils.upload(uploadedFile, un).toString());
		} catch (IOException e) {
			Logger.error("File upload error",e.fillInStackTrace());
			flash("error","File upload error");
		}
	 	return GO_HOME;
	}
	
	/**
	 * download file
	 * @param id file id to download
	 * @return file to download
	 */
	@SubjectPresent
	public static Result download(Long id) {
		Blobs fileToServe = Blobs.find.byId(id);
		try {
			if(fileToServe !=null)
				return ok(new java.io.File(Blobs.find.byId(id).blobFile));
			else return GO_HOME;
		} catch (Exception e) {
			Logger.error("Download error",e.fillInStackTrace());
			flash("error", "Can't download file");
			return GO_HOME;
			}
		}
	
	/**
	 * Show edit file form in order to edit file tag
	 * @param id
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result editFile(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Blobs> updateForm = new Form<Blobs>(Blobs.class).fill(
				Blobs.find.byId(id));
		return ok(views.html.editFile.render(id,updateForm));
	}

	/**
	 * updates blob
	 * @param id
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result update(Long id) {
		Form<Blobs> updateForm = new Form<Blobs>(Blobs.class).bindFromRequest();
		if (updateForm.hasErrors()) {
			return badRequest(views.html.editFile.render(id,updateForm));
		}
		updateForm.get().update(id);
		return GO_HOME;
	}
	
	/**
	 * delete file
	 * @param id File id to delete
	 * @return
	 */
	@Dynamic(value = "deleteFile")
	public static Result delete(Long id) {
		try {
			Blobs.find.ref(id).delete();
			flash("success", "File deleted");
		} catch (Exception e) {
			Logger.error("file delete error",e.fillInStackTrace());
			flash("error", e.getLocalizedMessage());
		}
		return GO_HOME;
		
	}
	
	/**
     * 
     * @param
     * @param
     * @return
     */
	@SubjectPresent
    public static Result treeJson() {
    	ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	
    	for(Installations in : Installations.find.all()) {
    		ObjectNode node = Json.newObject();
    		node.put("title", in.name);
    		node.put("key", "3_1");
    		node.put("folder",true);
    		node.put("lazy", true);
    		node.put("href",controllers.routes.InstallationApplication.installationIndex().toString());
    		allnodes.add(node);
    	}
    	return ok(play.libs.Json.toJson(allnodes));
        }
    
	@SubjectPresent
    public static Result fillJsonTree(Integer parentLevel, Long parentId) {
    	
    	ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	
		switch(parentLevel) {
		case 3:
			for(Plants item : Plants.find.where().eq("installation.id", parentId).findList()) {
	    		ObjectNode node = Json.newObject();
	    		node.put("title", item.name);
	    		node.put("key", "4" + "_" + item.id.toString());
	    		node.put("folder",true);
	    		node.put("lazy", true);
	    		node.put("href",controllers.routes.PlantApplication.plantIndex(item.id).toString());
	    		allnodes.add(node);
	    	}
			break;
		case 4:
			for(Sections item : Sections.find.orderBy("name")
					.where().eq("plant.id", parentId).findList()) {
	    		ObjectNode node = Json.newObject();
	    		node.put("title", item.name);
	    		node.put("key", "5" + "_" + item.id.toString());
	    		node.put("folder",true);
	    		node.put("lazy", true);
	    		node.put("href",controllers.routes.SectionApplication.sectionIndex(item.id).toString());
	    		allnodes.add(node);
	    	}
			break;
		case 5:
			Set<String> ecSet = new HashSet<String>();
			List<Equipments> itemList = Equipments.find.where().eq("section.id", parentId).findList();
			for(Equipments item : itemList) {
				ecSet.add(item.equipmentClass.name);
			}
			for(String citem : ecSet) {
				ObjectNode node = Json.newObject();
				ArrayNode allEquipmentNodes = node.arrayNode();
				node.put("title", citem);
				node.put("folder", true);
				for(Equipments item : itemList) {
					ObjectNode enode = Json.newObject();
					if(item.equipmentClass.name.equals(citem)) {
						enode.put("title", item.name);
			    		enode.put("key", "6" + "_" + item.id.toString());
			    		enode.put("folder",true);
			    		enode.put("lazy", true);
			    		enode.put("href",controllers.routes.EquipmentApplication.equipmentIndex(item.id).toString());
			    		allEquipmentNodes.add(enode);
					}
				}
				node.put("children",allEquipmentNodes);
				allnodes.add(node);
			}
			break;
		case 6:
			for(Subunits item : Subunits.find.where().eq("equipment.id", parentId).findList()) {
	    		ObjectNode node = Json.newObject();
	    		node.put("title", item.name);
	    		node.put("key", "7" + "_" + item.id.toString());
	    		node.put("folder",true);
	    		node.put("lazy", true);
	    		node.put("href",controllers.routes.SubunitApplication.subunitIndex(item.id).toString());
	    		allnodes.add(node);
	    	}
			break;
		case 7:
			for(Components item : Components.find.where().eq("subunit.id", parentId).findList()) {
	    		ObjectNode node = Json.newObject();
	    		node.put("title", item.name);
	    		node.put("key", "8" + "_" + item.id.toString());
	    		node.put("folder",true);
	    		node.put("lazy", true);
	    		node.put("href",controllers.routes.ComponentApplication.componentIndex(item.id).toString());
	    		allnodes.add(node);
	    	}
			break;
		case 8:
			for(PartsComponents item : PartsComponents.find.where().eq("component.id", parentId).findList()) {
	    		ObjectNode node = Json.newObject();
	    		node.put("title", item.part.name);
	    		node.put("key", "9" + "_" + item.part.id.toString());
	    		node.put("href",controllers.routes.PartApplication.partIndex(item.part.id).toString());
	    		allnodes.add(node);
	    	}
			break;
		}
		return ok(play.libs.Json.toJson(allnodes));
    }
    
    public static Result moveOrCopy(Long source, Long sourceId, Long dest, Long destId) {
    	if(source==6 && dest == 5)
			return ok(String.valueOf(Equipments.copy(sourceId, destId)));
		
    	if(source==6 && dest == 7)
    		return ok(String.valueOf(Equipments.copyAsComponent(sourceId, destId)));
    	
    	if(source==8 && dest == 5)
    		return ok(String.valueOf(Components.copyAsEquipment(sourceId, destId)));
    	
    	if(source==8 && dest == 7)
    		return ok(String.valueOf(Components.copy(sourceId, destId)));
    	
    	return ok("Nothing happened");
    }
    
    @SubjectPresent
    public static Result flotChartJson() {
    	//List<String[]> dataPoints = new ArrayList<String[]>();
    	List<ObjectNode> pieData = new ArrayList<ObjectNode>();
    	
    	//node.put("label","TM Maints");
    	
    	for(MaintenanceStatus ms  : MaintenanceStatus.values()) {
    		ObjectNode node = Json.newObject();
    		node.put("label",ms.toString());
    		node.put("data",Maintenances.find.where()
    				.eq("maintenanceStatus",ms).findRowCount());
    		pieData.add(node);
    	}
    	
    	return ok(play.libs.Json.toJson(pieData));
    }
    
    /**
     * 
     * @return
     */
    @SubjectPresent
    public static Result flotChartJson2() {
    	DateTime tod = new DateTime();
    	DateTime looper = new DateTime();
    	DateTime lastDay = tod.minusDays(30);
    	List<Integer[]> dataPoints = new ArrayList<Integer[]>();
    	ObjectNode node = Json.newObject();
    	node.put("label","Work request chart");
    	Integer counter = 30;
    	while(looper.isAfter(lastDay)) {
    		Integer[] points = new Integer[2];
        	points[0] = counter;
        	points[1] = Maintenances.find.where()
    				.between("requestDate", looper.minusDays(1).toDate(), looper.toDate())
    				.findRowCount();
        	dataPoints.add(points);
        	counter--;
        	looper = looper.minusDays(1);
        	}
        	node.put("data",play.libs.Json.toJson(dataPoints));
        	return ok(play.libs.Json.toJson(node));
    	}
    
    /**
     * 
     * @return
     */
	
	
    @Restrict({@Group("admin")})
	public static Result securityRoleList() {
    	response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.securityRoles.render());
	}
	
	/**
	 * 
	 * @return
	 */
    @Restrict({@Group("admin")})
    public static Result resolveDBInconsistencies() {
    	
    	try {
			//maintained section in maintenance must be equal to section of maintained equipment
    		//NO null calculated man-hour or ttr. set them to 0
    		Ebean
	         .createUpdate(Maintenances.class, "UPDATE maintenances SET calculated_total_man_hours=0 WHERE calculated_total_man_hours is NULL").execute();
    		Ebean
	         .createUpdate(Maintenances.class, "UPDATE maintenances SET time_to_repair=0 WHERE time_to_repair is NULL").execute();
    		Ebean
	         .createUpdate(Maintenances.class, "UPDATE maintenances SET calculated_total_time_to_repair=0 WHERE calculated_total_time_to_repair is NULL").execute();
    		
			for (Maintenances m : Maintenances.find.all()) {
				if(m.calculatedTotalManHours==null) m.calculatedTotalManHours=0.0f;
				if(m.calculatedTotalTimeToRepair==null) m.calculatedTotalTimeToRepair=0.0f;
				if(m.timeToRepair==null) m.timeToRepair=0.0f;
				if (m.maintainedEquipment != null) {
					m.maintainedSection = Sections.find
							.byId(m.maintainedEquipment.section.id);
					m.update();
				}
				
			}
			
			//Histories must belong to something
			List<History> hdList= History.find.where()
					.isNull("parentInstallation.id").isNull("parentPlant.id")
					.isNull("parentSection.id").isNull("parentEquipment.id")
					.isNull("parentSubunit.id").isNull("parentComponent.id")
					.findList();
			if(!hdList.isEmpty())
			for (History hd : hdList) {
				hd.delete();
			}
			
			//Failures with no histories must also be deleted(not a normal thing)
	    	for(Failures fd : Failures.find.all()) {
	    		if(History.find.where().eq("fail.id",fd.id).findUnique()==null) {
	    			for( Maintenances removeFromMaints : Maintenances.find.where().eq("correspondingFailure.id",fd.id).findList()) {
	    				removeFromMaints.correspondingFailure=null;
	    				removeFromMaints.update();
	    			}
	    			fd.delete();
	    		}
	    			
	    	}
	    	
	    	
	    	
	    	//NEXT : NO HISTORY LOG MUST HAVE BOTH NON_NULL main AND fail
	    	//EACH FAILURE MUST HAVE ONLY AND ONLY ONE HISTORY ASSIGNED TO IT
	    	//EACH MAINT CAN HAVE MAXIMUM OF TWO HISTORIES(PLANNED, ACTUAL)
	    	/**
	    	 * 
	    	 */
	    	
		} catch (Exception e) {
			Logger.error("DB inconsistency resolve error",e.fillInStackTrace());
		}
    	
    	return redirect(routes.Application.index2());
    }
	/**
	 * Javascript router
	 * @return
	 */
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
            Routes.javascriptRouter("jsRoutes",
                controllers.routes.javascript.HistoryApplication.json(),
                controllers.routes.javascript.ShiftApplication.json(),
                controllers.routes.javascript.HistoryApplication.deleteH(),
                controllers.routes.javascript.ShiftApplication.deleteShift(),
                controllers.routes.javascript.Application.fillJsonTree(),
                controllers.routes.javascript.FailureApplication.fillCausalityTree(),
                controllers.routes.javascript.Application.moveOrCopy(),
                controllers.routes.javascript.MaintenanceApplication.sectionEquipmentFiller(),
                controllers.routes.javascript.MaintenanceApplication.equipmentRoutineFiller(),
                controllers.routes.javascript.MaintenanceApplication.equipmentClassRoutineFiller(),
                controllers.routes.javascript.MaintenanceApplication.sectionRoutineFiller(),
                controllers.routes.javascript.MaintenanceApplication.calculateSerials(),
                controllers.routes.javascript.FailureApplication.failureFiller(),
                controllers.routes.javascript.FailureApplication.mtbf(),
                controllers.routes.javascript.MaintenanceApplication.mttr(),
                controllers.routes.javascript.UserApplication.disciplineUserFiller(),
                controllers.routes.javascript.MaintenanceApplication.createAjaxMaintenance(),
                controllers.routes.javascript.Application.flotChartJson(),
                controllers.routes.javascript.Application.flotChartJson2(),
                controllers.routes.javascript.WorkflowApplication.fillFlowTreeJson(),
                controllers.routes.javascript.WorkflowApplication.flowTreeJson(),
                controllers.routes.javascript.FailureApplication.flotChartFailureJson(),
                controllers.routes.javascript.PreventiveMaintenanceApplication.calculatePmPlanJson(),
                controllers.routes.javascript.PreventiveMaintenanceApplication.savePlannedMaints(),
                controllers.routes.javascript.EquipmentApplication.flotEquipmentUpTimeJson(),
                controllers.routes.javascript.DatasheetApplication.deleteItem(),
                controllers.routes.javascript.DatasheetApplication.update(),
                controllers.routes.javascript.MeasurementUnitApplication.measurementUnitOptions()
            )
        );
    }
	
}