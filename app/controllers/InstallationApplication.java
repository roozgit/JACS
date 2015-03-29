package controllers;

import static play.data.Form.form;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.Blobs;
import models.Installations;
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
public class InstallationApplication extends Controller {

	public static Result installationIndex() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.installations.installationIndex
				.render(Installations.find.all()));
	}
	
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result editInstallation(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Installations> installationForm = form(Installations.class).fill(
				Installations.find.byId(id));

		return ok(views.html.installations.editInstallation.render(id,
				installationForm));
	}
	
	@Restrict({ @Group("admin"), @Group("editor") })
	public static Result update(Long id) {
		
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request().body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for(String key : urlFormEncoded.keySet()) {
				
				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if(Installations.class.getField(key).getType().toString().compareTo
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
		Form<Installations> updateForm = new Form<Installations>(Installations.class).bind(newData);
		if (updateForm.hasErrors()) {
			return badRequest(views.html.installations.editInstallation.render(id,updateForm));
		}

		Ebean.beginTransaction();
		try {
			Installations i = updateForm.get();
			i.id=id;
			i.saveManyToManyAssociations("files");
			i.update(id);
			flash("success", "Installation " + updateForm.get().name + " has been updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Installation update error",e.fillInStackTrace());
			flash("error", "Can't update installation");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.InstallationApplication.installationIndex());
	}
	
	
	
	
	
	/**
	 * Uploads file directly for current installation
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result upload(Long installationId) {
		MultipartFormData body = request().body().asMultipartFormData();
			FilePart uploadedFile = body.getFile("file");
			String un = session().get("userName");
			Installations eq = Installations.find.byId(installationId);
			try {
				eq.files.add(
						Blobs.find.byId(myUtils.FileUtils.upload(uploadedFile, un)));
				eq.saveManyToManyAssociations("files");
				eq.update();
				
			} catch (IOException e1) {
				Logger.error("Installation blob upload error",e1.fillInStackTrace());
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
		 	return redirect(routes.InstallationApplication.installationIndex());
	}
	
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result deleteAssociation(Long installationId, Long blobId) {
		Installations eq = Installations.find.byId(installationId);
		Ebean.beginTransaction();
		try {
			eq.files.remove(
					Blobs.find.byId(blobId)
					);
			eq.saveManyToManyAssociations("files");
			eq.update(installationId);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Installation blob association removal error",e.fillInStackTrace());
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
			Logger.error("Installation physical file removal error(Installations)",e.fillInStackTrace());
		}
		return redirect(routes.InstallationApplication.installationIndex());
	}
	
}
