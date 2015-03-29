package controllers;

import static play.data.Form.form;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ValidationException;

import models.Blobs;
import models.PartHistory;
import models.Parts;
import models.maintenance.MaintenancesParts;
import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;

@SubjectPresent
public class PartApplication extends Controller {
	
	/**
     * This result directly redirect to part list home.
     */
    public static final Result GO_HOME = redirect(
        routes.PartApplication.list(0, "name", "asc", "")
    );
    
    
    /**
     * Information about one part
     * @param id Id of the part
     * @return
     */
	public static Result partIndex(Long id) {
		response().setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response().setHeader("Pragma","no-cache");

		 return ok(views.html.parts.partIndex.render(
				 Parts.find.byId(id),
				 Parts.findComponentsForThisPart(id)
				 ));
	}

	 /**
	  * Returns a list of 50 parts for use in companyList or filtered queries
	  * @param page
	  * @param sortBy
	  * @param order
	  * @param filter
	  * @return
	  */
	   public static Result list(int page, String sortBy, String order, String filter) {
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
     				views.html.parts.partList.render(
     						Parts.page(page, ppid, sortBy , order, filter),
     						sortBy, order, filter
     						));
     	}
     	
 

    	/**
    	 * Displays company creates form
    	 * @return form display
    	 */
	   @Restrict({ @Group("admin"), @Group("creator"), @Group("warehouse") })
     	public static Result createPart() {
		   response().setHeader("Cache-Control",
					"no-store, no-cache, must-revalidate");
			response().setHeader("Pragma", "no-cache");
        		Form<Parts> newPartForm = form(Parts.class);
    		return ok(
    			views.html.parts.createPartForm.render(newPartForm));

        }
     	
    	/**
    	 * Saves a part
    	 * @return GO_HOME
    	 */
	   @Restrict({ @Group("admin"), @Group("creator"), @Group("warehouse") })
     	public static Result save() {
            Form<Parts> newPartForm = form(Parts.class).bindFromRequest();
            if(newPartForm.hasErrors()) {
                return badRequest(
    			    views.html.parts.createPartForm.render(newPartForm));
            }
            Ebean.beginTransaction();
    		try {
    			Parts p = newPartForm.get();
    			p.remainingQuantity=0F;
    			p.save();
    			flash("success", "Part " + newPartForm.get().name + " has been created");
    		Ebean.commitTransaction();
    		} catch (Exception e) {
    			Logger.error("Part save error",e.fillInStackTrace());
    			flash("error", "Part save error");
    			Ebean.rollbackTransaction();
    		} finally {
    			Ebean.endTransaction();
    		}
    		return GO_HOME;
     	}
     	
    	/**
    	 * display edit part form
    	 * @param id Id of part
    	 * @return form display
    	 */
	   @Restrict({@Group("admin"), @Group("editor"), @Group("warehouse")})
     	public static Result editPart(Long id) {
		   response().setHeader("Cache-Control",
					"no-store, no-cache, must-revalidate");
			response().setHeader("Pragma", "no-cache");
		Form<Parts> partForm = form(Parts.class).fill(Parts.find.byId(id));
		return ok(
				views.html.parts.editPart.render(id, partForm)
		);
       }
     	
    	/**
    	 * Updates company
    	 * @param id Id of part
    	 * @return GO_HOME
    	 */
	   @Restrict({@Group("admin"), @Group("editor"), @Group("warehouse")})
     	public static Result update(Long id) {
     		Map<String, String> newData = new HashMap<String, String>();
    		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
    				.body().asFormUrlEncoded();
    		if (urlFormEncoded != null) {
    			for (String key : urlFormEncoded.keySet()) {
    				String[] value = urlFormEncoded.get(key);
    				if (value.length == 1) {
    					try {
    						if (Parts.class.getField(key).getType().toString()
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
    	Form<Parts> updateForm = new Form<Parts>(Parts.class)
				.bind(newData);
		if (updateForm.hasErrors()) {
			return badRequest(views.html.parts.editPart.render(
					id, updateForm));
		}
		
		Ebean.beginTransaction();
		try {
			Parts p = updateForm.get();
			p.id=id;
			//If measurement unit is changed, all previously used units become invalid
			//so it must be prevented.
			if(!p.measurementUnit.equals(Parts.find.byId(id).measurementUnit)) {
				if(MaintenancesParts.find.where()
						.eq("part.id",id).findRowCount() > 0
						||
					PartHistory.find.where()
						.eq("parentPart.id",id).findRowCount() > 0)
					throw new ValidationException("This part has already been used with other" +
						"measurement units. Can't change unit now. Wait until unit conversion comes along!");
			}
			//If currency is changed, all previously used currencies become invalid
			//so it must be prevented.
			if(!p.currency.equals(Parts.find.byId(id).currency)) {
				if(PartHistory.find.where()
						.eq("parentPart.id",id).findRowCount() > 0)
					throw new ValidationException("This part has already been used with other" +
						"currency. Can't change currency now. Wait until currency conversion comes along!");
			}
			
			p.saveManyToManyAssociations("files");
			p.saveManyToManyAssociations("manufacturerCompanies");
			p.update(id);
		flash("success", "Part " + updateForm.get().name + " has been updated");
		Ebean.commitTransaction();
		} catch (Exception e) {
			flash("error", "Can't update part");
			Logger.error("Part update error",e.fillInStackTrace());
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.PartApplication.partIndex(id));
	}

	   /**
	    * 
	    * @return
	    */
    @Restrict({ @Group("admin"), @Group("warehouse"), @Group("planner") })
    public static Result viewPartRequests() {
    	response().setHeader("Cache-Control",
    			"no-store, no-cache, must-revalidate");
    	response().setHeader("Pragma", "no-cache");
    	List<MaintenancesParts> mptList =
    			MaintenancesParts.find.where().eq("stockFlag", false).findList();
    	return ok(views.html.parts.partRequestList.render(mptList));
    }
    
    @Restrict({ @Group("admin"), @Group("warehouse") })
    public static Result delete(Long id) {
    	try {
    		Parts.find.ref(id).delete();
    		flash("success: Part deleted");
    	} catch (Exception x){
    		Logger.error("Part removal error",x.fillInStackTrace());
    		flash("error! can't delete part: ");
    	  Logger.error("Error in part delete",x.fillInStackTrace());
    	}
    	return redirect(routes.PartApplication.list(0, "name", "asc", ""));
    }
    
    /**
	 * Uploads file directly for current part
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor"), @Group("warehouse"), @Group("planner") })
	public static Result upload(Long partId) {
		MultipartFormData body = request().body().asMultipartFormData();
			FilePart uploadedFile = body.getFile("file");
			String un = session().get("userName");
			Parts eq = Parts.find.byId(partId);
			try {
				eq.files.add(
						Blobs.find.byId(myUtils.FileUtils.upload(uploadedFile, un)));
				eq.saveManyToManyAssociations("files");
				eq.update();
				
			} catch (IOException e1) {
				Logger.error("Part blob association error",e1.fillInStackTrace());
				flash("error",e1.getMessage());
				
			} catch(IllegalStateException e2) {
				Blobs fblob = Blobs.find.where().eq("name",uploadedFile.getFilename()).findUnique();
				if(!eq.files.contains(fblob)) {
					eq.files.add(fblob);
					eq.saveManyToManyAssociations("files");
					eq.update();
					flash("success",e2.getMessage()+" File added to data source anyway.");
					
				} else flash("error", e2.getMessage()+ " File already is a data source");
				
			}
		 	return redirect(routes.PartApplication.partIndex(partId)+"#tabs-dataSources");
	}
	
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor"), @Group("warehouse"), @Group("planner") })
	public static Result deleteAssociation(Long partId, Long blobId) {
		Parts eq = Parts.find.byId(partId);
		Ebean.beginTransaction();
		try {
			eq.files.remove(
					Blobs.find.byId(blobId)
					);
			eq.saveManyToManyAssociations("files");
			eq.update(partId);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Part blob association removal error",e.fillInStackTrace());
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
			Logger.error("Physical file removal error(Parts)",e.fillInStackTrace());
		}
		return redirect(routes.PartApplication.partIndex(partId)+"#tabs-dataSources");
	}
}
