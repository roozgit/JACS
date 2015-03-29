package controllers;

import static play.data.Form.form;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.Blobs;
import models.Subunits;
import models.equipment.Equipments;
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
public class SubunitApplication extends Controller {

	/**
	 * Information about one subunit
	 * 
	 * @param id
	 *            Id of the subunit
	 * @return Result of render(Subunits.find.byId(id))
	 */
	public static Result subunitIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.subunits.subunitIndex.render(Subunits.find.byId(id)));

	}

	/**
	 * Returns a list of 15 subunits for use in subunitList view
	 * 
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
	public static Result list(Long parentEquipmentId, int page, String sortBy,
			String order, String filter) {
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
		return ok(views.html.subunits.subunitList.render(parentEquipmentId,
				Subunits.page(parentEquipmentId, page, ppid, sortBy, order,
						filter), sortBy, order, filter));
	}

	/**
	 * Displays subunit create form
	 * 
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result createSubunit(Long parentEquipmentId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Subunits> newSubunitForm = form(Subunits.class);
		return ok(views.html.subunits.createSubunitForm.render(
				parentEquipmentId, newSubunitForm));

	}

	/**
	 * Saves a subunit
	 * 
	 * @return table of all equipments
	 */
	@Restrict({ @Group("admin"), @Group("creator") })
	public static Result save(Long parentEquipmentId) {
		Form<Subunits> newSubunitForm = form(Subunits.class).bindFromRequest();
		if (newSubunitForm.hasErrors()) {
			return badRequest(views.html.subunits.createSubunitForm.render(
					parentEquipmentId, newSubunitForm));
		}
		Ebean.beginTransaction();
		try {
			Subunits s = newSubunitForm.get();
			s.equipment = Equipments.find.byId(parentEquipmentId);
			s.save();
			flash("success", "Subunit " + newSubunitForm.get().name
					+ " has been created");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Subunit save error",e.fillInStackTrace());
			flash("error", "Subunit save error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.SubunitApplication.list(parentEquipmentId, 0, "name", "asc", ""));
	}

	/**
	 * display edit subunit form
	 * 
	 * @param id
	 *            Id of subunit
	 * @return form display
	 */
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result editSubunit(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Subunits> subunitForm = form(Subunits.class).fill(
				Subunits.find.byId(id));

		return ok(views.html.subunits.editSubunit.render(id, subunitForm));
	}

	/**
	 * Updates subunit
	 * 
	 * @param id
	 *            Id of subunit
	 * @return table of all subunits
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
						if (Subunits.class.getField(key).getType().toString()
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
		Form<Subunits> updateForm = new Form<Subunits>(Subunits.class)
				.bind(newData);
		if (updateForm.hasErrors()) {
			return badRequest(views.html.subunits.editSubunit.render(id,updateForm));
		}

		Ebean.beginTransaction();
		try {
			Subunits s = updateForm.get();
			s.id = id;
			s.saveManyToManyAssociations("files");
			s.update(id);
			flash("success", "subunit " + updateForm.get().name
					+ " has been updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Subunit update error",e.fillInStackTrace());
			flash("error", "Subunit update error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.SubunitApplication.subunitIndex(id));
	}
	
	/**
	 * Uploads file directly for current subunit
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result upload(Long subunitId) {
		MultipartFormData body = request().body().asMultipartFormData();
			FilePart uploadedFile = body.getFile("file");
			String un = session().get("userName");
			Subunits eq = Subunits.find.byId(subunitId);
			try {
				eq.files.add(
						Blobs.find.byId(myUtils.FileUtils.upload(uploadedFile, un)));
				eq.saveManyToManyAssociations("files");
				eq.update();
				
			} catch (IOException e1) {
				Logger.error("Subunit blob update error",e1.fillInStackTrace());
				flash("error","Can't upload file for subunit");
				
			} catch(IllegalStateException e2) {
				Blobs fblob = Blobs.find.where().eq("name",uploadedFile.getFilename()).findUnique();
				if(!eq.files.contains(fblob)) {
					eq.files.add(fblob);
					eq.saveManyToManyAssociations("files");
					eq.update();
					flash("success",e2.getMessage()+" File added to data source anyway.");
					
				} else flash("error", e2.getMessage()+ " File already is a data source");
				
			}
		 	return redirect(routes.SubunitApplication.subunitIndex(subunitId));
	}
	
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result deleteAssociation(Long subunitId, Long blobId) {
		Subunits eq = Subunits.find.byId(subunitId);
		Ebean.beginTransaction();
		try {
			eq.files.remove(
					Blobs.find.byId(blobId)
					);
			eq.saveManyToManyAssociations("files");
			eq.update(subunitId);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Subunit blob association removal",e.fillInStackTrace());
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
			Logger.error("Physical file removal error(Subunits)",e.fillInStackTrace());
		}
		return redirect(routes.SubunitApplication.subunitIndex(subunitId));
	}
	
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result delete(Long parentEquipmentId, Long id) {
		try {
			Subunits.find.ref(id).delete();
		} catch (Exception e) {
			Logger.error("Subunit delete error",e.fillInStackTrace());;
		}
		
		return redirect(routes.SubunitApplication.list(parentEquipmentId, 0, "name", "asc", ""));
	}


}