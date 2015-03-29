package controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import models.Tags2;
import models.UserBlobs;
import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

@SubjectPresent
public class UserBlobApplication extends Controller {
	


	public static Result uploadForm() {
		List<UserBlobs> blobList = new ArrayList<UserBlobs>();
		blobList = UserBlobs.find.all();
		return ok(
			views.html.users.uploadFile.render(blobList)
			);
}
	
	//********************FILE METHODS**************
	public static Result upload(Long userId) {
		MultipartFormData body = request().body().asMultipartFormData();
		FilePart uploadedFile = body.getFile("file");
		try {
			Logger.info("Upload return code:"+
					myUtils.FileUtils.uploadPersonnel(uploadedFile, userId).toString());
		} catch (IOException e) {
			Logger.error("personnel upload error", e.fillInStackTrace());
			flash("error","Can't upload file for personnel");
		}
		return redirect(routes.UserApplication.userIndex(userId));
	}
	
	/**
	 * download file
	 * @param id file id to download
	 * @return file to download
	 */
	public static Result download(Long id) {
		UserBlobs fileToServe = UserBlobs.find.byId(id);
		try {
		File fin = new File("/tmp");
		if (fin.listFiles() != null) {
			for (File file : fin.listFiles()) {
    				org.apache.commons.io.FileDeleteStrategy.FORCE.deleteQuietly(file);
				}
			org.apache.commons.io.FileUtils.writeByteArrayToFile(
				new File("/tmp/"+fileToServe.name), fileToServe.blobFile);
			
		}
			} catch (IOException e) {
				Logger.error("UserBlob download error",e.fillInStackTrace());
					flash("error", "Can't download file");
    				return redirect(routes.UserBlobApplication.uploadForm());
				}
		return ok(new java.io.File("/tmp/"+fileToServe.name));
	}
	
	public static Result renderImage(Long userId) {
		long bid=-1L;
		for(UserBlobs ub : UserBlobs.find.where().eq("owner.id",userId).findList()) {
			if(ub.tag==Tags2.PERSONNEL_PHOTO) bid=ub.id;
		}
		if(bid==-1L)
			return ok("Nothing");
		else {
			byte[] image = UserBlobs.find.byId(bid).blobFile;
			return ok(image).as("image/jpeg");
		}
			
	}
	/**
	 * Show edit file form in order to edit file tag
	 * @param id
	 * @return
	 */
	@Restrict({ @Group("admin")})
	public static Result editFile(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<UserBlobs> updateForm = new Form<UserBlobs>(UserBlobs.class).fill(
				UserBlobs.find.byId(id));
		return ok(views.html.users.editFile.render(id,updateForm));
	}

	/**
	 * updates blob
	 * @param id
	 * @return
	 */
	@Restrict({ @Group("admin")})
	public static Result update(Long id) {
		Form<UserBlobs> updateForm = new Form<UserBlobs>(UserBlobs.class).bindFromRequest();
		if (updateForm.hasErrors()) {
			return badRequest(views.html.users.editFile.render(id,updateForm));
		}
		updateForm.get().update(id);
		return redirect(routes.UserBlobApplication.uploadForm());
		
	}
	
	/**
	 * delete file
	 * @param id File id to delete
	 * @return
	 */
	@Restrict({ @Group("admin")})
	public static Result delete(Long id) {
			try {
				UserBlobs.find.ref(id).delete();
			} catch (Exception e) {
				Logger.error("UserBlob delete error",e.fillInStackTrace());
			}
			
			return redirect(routes.UserBlobApplication.uploadForm());
	}

}
