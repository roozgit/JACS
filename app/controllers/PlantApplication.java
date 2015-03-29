package controllers;

import static play.data.Form.form;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.Blobs;
import models.Installations;
import models.Plants;
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
public class PlantApplication extends Controller {
	
	/**
     * This result directly redirect to company list home.
     */
    public static final Result GO_HOME = redirect(
        routes.PlantApplication.list(0, "name", "asc", "")
    );

    /**
     * Information about one plant
     * @param id Id of the plant
     * @return Result of render(Plants.find.byId(id))
     */
	public static Result plantIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");

		return ok(views.html.plants.plantIndex.render(Plants.find.byId(id)));
	}
	
	/**
	 * Returns a list of 10 plants for use in plantList view
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
			int ppid = 5;
			if(session().containsKey("pagesize")) {
				try {
				ppid = Integer.valueOf(session().get("pagesize"));
				} catch (Exception eex) {
					ppid = 5;
				}
			}
			return ok(
					views.html.plants.plantList.render(
							Plants.page(page, ppid, sortBy , order, filter),
							sortBy, order, filter
							));
		}
		
	/**
	 * Lists a page of plants ordered by name and ascending
	 * @return
	 */
		public static Result plantList() {
			return GO_HOME;
		}

	/**
	 * Displays plant create form
	 * @return form display
	 */
	@Restrict({@Group("admin")})
	public static Result createPlant() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Plants> newPlantForm = form(Plants.class);
		return ok(views.html.plants.createPlantForm.render(newPlantForm));
	}
	
	/**
	 * Saves a plant
	 * @return GO_HOME
	 */
	@Restrict({@Group("admin")})
	public static Result save() {
		Form<Plants> newPlantForm = form(Plants.class).bindFromRequest();
		if (newPlantForm.hasErrors()) {
			return badRequest(views.html.plants.createPlantForm.render(newPlantForm));
		}
		Ebean.beginTransaction();
		try {
			Plants p = newPlantForm.get();
			p.installation = Installations.find.byId(1L);
			p.save();
			flash("success", "Plant " + newPlantForm.get().name + " has been created");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Plant save error",e.fillInStackTrace());
			flash("error", "Plant save error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return GO_HOME;
	}

	/**
	 * display edit plant form
	 * @param id Id of plant
	 * @return form display
	 */
	@Restrict({@Group("admin"),
		@Group("editor")})
	public static Result editPlant(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Plants> plantForm = form(Plants.class).fill(Plants.find.byId(id));
		return ok(views.html.plants.editPlant.render(id, plantForm));
	}

	/**
	 * Updates plant
	 * @param id Id of plant
	 * @return GO_HOME
	 */
	@Restrict({@Group("admin"),
		@Group("editor")})
	public static Result update(Long id) {
		
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request().body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for(String key : urlFormEncoded.keySet()) {
				
				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if(Plants.class.getField(key).getType().toString().compareTo
								("interface java.util.List")==0) {
							newData.put(key + "[0]"+".id", value[0]);
						} else {
							newData.put(key,value[0]);
							}
					} catch (NoSuchFieldException |SecurityException e) {
						newData.put(key,value[0]);
					}
				} else if(value.length > 1) {
					for (int i = 0; i < value.length; i++) {
						newData.put(key + "[" + i + "]"+".id" , value[i]);
					}
				}
			}
		}
		Form<Plants> updateForm = new Form<Plants>(Plants.class).bind(newData);
		if (updateForm.hasErrors()) {
			return badRequest(views.html.plants.editPlant.render(id,updateForm));
		}
		Ebean.beginTransaction();
		try {
			Plants p = updateForm.get();
			p.id=id;
			p.saveManyToManyAssociations("files");
			p.update(id);
			flash("success", "Plant " + updateForm.get().name + " has been updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Plant update error",e.fillInStackTrace());
			flash("error", "Plant update error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect((routes.PlantApplication.plantIndex(id)));
	}
	
	/**
	 * Uploads file directly for current plant
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result upload(Long plantId) {
		MultipartFormData body = request().body().asMultipartFormData();
			FilePart uploadedFile = body.getFile("file");
			String un = session().get("userName");
			Plants eq = Plants.find.byId(plantId);
			try {
				eq.files.add(
						Blobs.find.byId(myUtils.FileUtils.upload(uploadedFile, un)));
				eq.saveManyToManyAssociations("files");
				eq.update();
				
			} catch (IOException e1) {
				Logger.error("Plant blob upload error",e1.fillInStackTrace());
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
		 	return redirect(routes.PlantApplication.plantIndex(plantId));
	}
	
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result deleteAssociation(Long plantId, Long blobId) {
		Plants eq = Plants.find.byId(plantId);
		Ebean.beginTransaction();
		try {
			eq.files.remove(
					Blobs.find.byId(blobId)
					);
			eq.saveManyToManyAssociations("files");
			eq.update(plantId);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Plant blob association error",e.fillInStackTrace());
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
			Logger.error("Physical file removal error(Plants)",e.fillInStackTrace());
		}
		return redirect(routes.PlantApplication.plantIndex(plantId));
	}

}