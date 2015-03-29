package controllers;

import static play.data.Form.form;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.Blobs;
import models.Components;
import models.Subunits;
import myUtils.DateTimeUtils;
import play.Logger;
import play.data.DynamicForm;
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
public class ComponentApplication extends Controller {

	public static void parseDatesAsIranianDates(Components eq) {
		eq.manufactureDate = DateTimeUtils
				.getIranianDateAsDate(eq.manufactureDate);
		eq.purchaseDate = DateTimeUtils
				.getIranianDateAsDate(eq.purchaseDate);
		eq.guaranteeEndDate = DateTimeUtils
				.getIranianDateAsDate(eq.guaranteeEndDate);
	}
	/**
	 * Information about one component
	 * 
	 * @param id
	 *            Id of the component
	 * @return Result of render(Components.find.byId(id))
	 */
	public static Result componentIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Components co = Components.find.byId(id);
		if(co==null) return badRequest("No such component"); else
		return ok(views.html.components.componentIndex.render(co));
	}

	/**
	 * Returns a list of 25 components for use in subunitList view
	 * 
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
	public static Result list(Long parentSubunitId, int page, String sortBy,
			String order, String filter) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		int ppid = 20;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 20;
			}
		}
		if(Subunits.find.byId(parentSubunitId)==null) return badRequest("No such parent subunit");
		else
		return ok(views.html.components.componentList.render(parentSubunitId,
				Components.page(parentSubunitId, page, ppid, sortBy, order,
						filter), sortBy, order, filter));
	}

	/**
	 * Displays subunit create form
	 * 
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result createComponent(Long parentSubunitId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Components> newComponentForm = form(Components.class);
		return ok(views.html.components.createComponentForm.render(parentSubunitId,newComponentForm));
	}

	/**
	 * Saves a component
	 * 
	 * @return table of all components
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result save(Long parentSubunitId) {
		Form<Components> newComponentForm = form(Components.class)
				.bindFromRequest();
		if (newComponentForm.hasErrors()) {
			return badRequest(views.html.components.createComponentForm.render(parentSubunitId,newComponentForm));
		}
		Ebean.beginTransaction();
		try {
			Components c = newComponentForm.get();
			c.subunit = Subunits.find.byId(parentSubunitId);
			c.save();
			flash("success", "Component " + newComponentForm.get().name
					+ " has been created");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Component save error",e.fillInStackTrace());
			flash("error", "Component save error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.ComponentApplication.list(parentSubunitId, 0, "name", "asc", ""));
	}

	/**
	 * display edit component form
	 * 
	 * @param id
	 *            Id of component
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result editComponent(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Components co = Components.find.byId(id);
		if(co==null)
			return badRequest("No such component"); else {
		parseDatesAsIranianDates(co);
		Form<Components> componentForm = form(Components.class).fill(
				co);
		return ok(views.html.components.editComponent.render(id, componentForm));
			}
	}

	/**
	 * Updates component
	 * 
	 * @param id
	 *            Id of component
	 * @return table of all components
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
						if (Components.class.getField(key).getType()
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
						if(Components.class.getField(key).getType()
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
		Form<Components> updateForm = form(Components.class).bind(newData);
		if (updateForm.hasErrors()) {
			return badRequest(views.html.components.editComponent.render(id,updateForm));
		}
		Ebean.beginTransaction();
		try {
			Components c = updateForm.get();
			c.id = id;
			c.saveManyToManyAssociations("files");
			c.update(id);
			
			//Ensure that dates are set to null if necessary
			if (c.manufactureDate == null){
			       Ebean
			         .createUpdate(Components.class, "UPDATE components SET manufacture_date=null where id=:id")
			         .setParameter("id", id).execute();
			    }
			if (c.purchaseDate == null){
			       Ebean
			         .createUpdate(Components.class, "UPDATE components SET purchase_date=null where id=:id")
			         .setParameter("id", id).execute();
			    }
			if (c.guaranteeEndDate == null){
			       Ebean
			         .createUpdate(Components.class, "UPDATE components SET guarantee_end_date=null where id=:id")
			         .setParameter("id", id).execute();
			    }
			//End of null date updating block
			flash("success", "Component " + updateForm.get().name
					+ " has been updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Component update error",e.fillInStackTrace());
			flash("error", "Error in company update");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.ComponentApplication.componentIndex(id));
	}
	
	/**
	 * Uploads file directly for current component
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result upload(Long componentId) {
		MultipartFormData body = request().body().asMultipartFormData();
			FilePart uploadedFile = body.getFile("file");
			String un = session().get("userName");
			Components eq = Components.find.byId(componentId);
			try {
				eq.files.add(
						Blobs.find.byId(myUtils.FileUtils.upload(uploadedFile, un)));
				eq.saveManyToManyAssociations("files");
				eq.update();
				
			} catch (IOException e1) {
				Logger.error("Component blob upload error",e1.fillInStackTrace());
				flash("error","Error in uploading file");
				
			} catch(IllegalStateException e2) {
				Blobs fblob = Blobs.find.where().eq("name",uploadedFile.getFilename()).findUnique();
				if(!eq.files.contains(fblob)) {
					eq.files.add(fblob);
					eq.saveManyToManyAssociations("files");
					eq.update();
					flash("success",e2.getMessage()+" File added to data source anyway.");
					
				} else flash("error", e2.getMessage()+ " File already is a data source");
				
			}
		 	//return redirect("/component?id="+componentId.toString() + "#tabs-sources");
			return redirect(routes.ComponentApplication.componentIndex(componentId)+"#tabs-sources");
	}
	
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result deleteAssociation(Long componentId, Long blobId) {
		Components eq = Components.find.byId(componentId);
		Ebean.beginTransaction();
		try {
			eq.files.remove(
					Blobs.find.byId(blobId)
					);
			eq.saveManyToManyAssociations("files");
			eq.update(componentId);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Component blob association removal error",e.fillInStackTrace());
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
			Logger.error("Physical file removal error(Components)",e.fillInStackTrace());
		}
		return redirect(routes.ComponentApplication.componentIndex(componentId) + "#tabs-sources");
	}
	
	/**
	 * delete
	 * @param id to delete
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result delete(Long parentSubunitId, Long id) {
		try {
			Components.find.ref(id).delete();
			flash("success,Component deleted successfully");
		} catch (Exception e) {
			Logger.error("Component delete error",e.fillInStackTrace());
			flash("error","An error occured. Remove possible references to the component and try again");
		}
		
		return redirect(routes.ComponentApplication.list(parentSubunitId, 0, "name", "asc", ""));
	}
	
	/**
	 * 
	 * @param parentComponentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("editor") ,@Group("creator")})
	public static Result copyComponent(Long componentId) {
		DynamicForm requestData = form().bindFromRequest();
		String newParentSubunitId = requestData.get("subunit.id");
		if(Components.copy(componentId,Long.valueOf(newParentSubunitId))==true) {
			flash("success","Component was copied to new subunit.");
		} else {
			flash("error","Error! Component copy failed!");
		}
		return redirect(
				routes.ComponentApplication.list(
						Long.valueOf(newParentSubunitId), 0, "name", "asc", ""));
	}
	
	/**
	 * 
	 * @param parentComponentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("editor"), @Group("creator") })
	public static Result copyComponentAsEquipment(Long componentId) {
		DynamicForm requestData = form().bindFromRequest();
		String newParentSectionId = requestData.get("section.id");
		if(Components.copyAsEquipment(componentId,Long.valueOf(newParentSectionId))==true) {
			flash("success","Component was moved to section as equipment");
		} else {
			flash("error","Error! Component move failed!");
		}
		return redirect(
				routes.EquipmentApplication.list(
						Long.valueOf(newParentSectionId), 0, "name", "asc", ""));
	}

}