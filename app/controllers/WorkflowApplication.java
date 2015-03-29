package controllers;

import static play.data.Form.form;

import java.util.ArrayList;
import java.util.List;

import models.maintenance.MaintenanceWorkflow;
import models.maintenance.Maintenances;
import models.maintenance.WorkflowTree;
import play.Logger;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.avaje.ebean.Ebean;

@SubjectPresent
public class WorkflowApplication extends Controller {
	
	public static Result workflowIndex(String maintCat) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		
		Form<WorkflowTree> wftForm = form(WorkflowTree.class);
		return ok(views.html.workflow.workflowIndex.render(
					maintCat,
					WorkflowTree.find.where()
					.eq("treeCategory", maintCat)
					.findList(),
					wftForm
				));
	}
	
	
	@Restrict({@Group("admin")})
	public static Result deleteWorkflow(String maintCat) {
		
		Ebean.beginTransaction();
		try {
			//Remove all references from maintenances to current workflow
			Ebean
	         .createUpdate(Maintenances.class, "UPDATE maintenances SET workflow_stage_id=null").execute();
			
			//DELETE all workflow history
			Ebean.delete(MaintenanceWorkflow.find.all());
			
			//DELETE workflow
			Ebean.delete(WorkflowTree.find.where().eq("treeCategory", maintCat).findList());
			Ebean.commitTransaction();
		} catch (Exception ex) {
			Logger.error("Error in workflow deletion",ex.fillInStackTrace());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
			return redirect(routes.Application.index2());
	}
	
	@Restrict({@Group("admin")})
	public static Result save(String maintCat) {
		Form<WorkflowTree> wftForm = form(WorkflowTree.class).bindFromRequest();
		wftForm.get().save();
		return redirect(routes.WorkflowApplication.workflowIndex(maintCat));
	}
	
	@Restrict({@Group("admin")})
	public static Result editWorkflow(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<WorkflowTree> wftForm = form(WorkflowTree.class).fill(WorkflowTree.find.byId(id));;
		return ok(views.html.workflow.editWorkflow.render(
					id, wftForm
				));
	}
	
	@Restrict({@Group("admin")})
	public static Result update(Long id) {
		Form<WorkflowTree> wftForm = form(WorkflowTree.class).bindFromRequest();
		if(wftForm.hasErrors())
			return badRequest(views.html.workflow.editWorkflow.render(
					id,wftForm));
		
		wftForm.get().update();
		return redirect(routes.Application.index2());
	}
	
	
		/**
		 * 
		 * @return
		 */
	    public static Result flowTreeJson(String wfType) {
			ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
			ObjectNode node = Json.newObject();
			WorkflowTree head = WorkflowTree.find.where().isNull("referringRole").eq("treeCategory", wfType).findUnique();
			node.put("title", head.decidingRole.getName());
			node.put("key",head.receivingRole.id+"_"+head.decidingRole.id);
			node.put("folder",true);
    		node.put("lazy", true);
			allnodes.add(node);
			return ok(play.libs.Json.toJson(allnodes));
	 }
		
	/**
	 * 
	 * @return
	 */
    public static Result fillFlowTreeJson(Long recId,Long decId, String wfType) {
    	ArrayList<ObjectNode> allnodes = new ArrayList<ObjectNode>();
    	
    	List<WorkflowTree> chosenList;
    	
    	chosenList= WorkflowTree.find
        			.where()
        			.eq("treeCategory",wfType)
        			.eq("referringRole.id",decId)
        			.eq("decidingRole.id",recId)
        			.orderBy("id asc")
        			.findList();
    			
    			
    	
    	for(WorkflowTree wft : chosenList) {
    		ObjectNode node = Json.newObject();
    		node.put("title", wft.decidingRole.getName());
    		if(wft.receivingRole==null) {
    			node.put("key", "0"+"_"+wft.decidingRole.id);
    			node.put("folder",false);
        		node.put("lazy",false);
    		}
    			
    		else {
    			node.put("key", wft.receivingRole.id+"_"+wft.decidingRole.id);
    			node.put("folder",true);
        		node.put("lazy",true);
    		}
    		//node.put("href",controllers.routes.InstallationApplication.installationIndex().toString());
    		allnodes.add(node);
    		
    	}
    	return ok(play.libs.Json.toJson(allnodes));
        }
}