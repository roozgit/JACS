package controllers;

import static play.data.Form.form;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import models.Blobs;
import models.Companies;

import org.apache.commons.io.IOUtils;

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
public class CompanyApplication extends Controller {
	
	/**
     * This result directly redirect to company list home.
     */
    public static final Result GO_HOME = redirect(
        routes.CompanyApplication.list(0, "name", "asc", ""));
    
/**
 * Information about one company
 * @param id Id of the company
 * @return Result of render(Companies.find.byId(id))
 */
	public static Result companyIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Companies com = Companies.find.byId(id);
		if(com==null) return badRequest("No such company"); else
		return ok(views.html.companies.companyIndex.render(com,
				Companies.find.where().eq("headCompanies.id", id).findList()
				));
	}

	/**
	 * Returns a list of 10 companies for use in companyList or filtered queries
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
		int ppid = 15;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 15;
			}
		}
		return ok(
				views.html.companies.companyList.render(
						Companies.page(page, ppid, sortBy , order, filter),
						sortBy, order, filter
						));
	}
	
/**
 * Lists a page of companies ordered by name and ascending
 * @return
 */
	public static Result companyList() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");

		return GO_HOME;

	}

	/**
	 * Displays company creates form
	 * @return form display
	 */
	@Restrict({@Group("admin"),
		@Group("creator")})
	public static Result createCompany() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Companies> newCompanyForm = form(Companies.class);
		return ok(views.html.companies.createCompanyForm.render(newCompanyForm));
	}

	/**
	 * Saves a company
	 * @return GO_HOME
	 */
	@Restrict({@Group("admin"),
		@Group("creator")})
	public static Result save() {
		Form<Companies> newCompanyForm = form(Companies.class)
				.bindFromRequest();
		if (newCompanyForm.hasErrors()) {
			return badRequest(views.html.companies.createCompanyForm.render(newCompanyForm));
		}
		Ebean.beginTransaction();
		try {
			newCompanyForm.get().save();
			flash("success", "Company " + newCompanyForm.get().name + " has been created");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Company save error",e.fillInStackTrace());
			flash("error", "Can't save company");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return GO_HOME;
	}

	/**
	 * display edit company form
	 * @param id Id of company
	 * @return form display
	 */
	@Restrict({@Group("admin"),
		@Group("editor")})
	public static Result editCompany(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Companies> companyForm = form(Companies.class).fill(
				Companies.find.byId(id));
		return ok(views.html.companies.editCompany.render(id, companyForm));
	}

	/**
	 * Updates company
	 * @param id Id of comapny
	 * @return GO_HOME
	 */
	@Restrict({@Group("admin"),
		@Group("editor")})
	public static Result update(Long id) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {
				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (Companies.class.getField(key).getType().toString()
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
		Form<Companies> updateForm = new Form<Companies>(Companies.class)
				.bind(newData);

		if (updateForm.hasErrors()) {
			return badRequest(views.html.companies.editCompany.render(id,updateForm));
		}
		Ebean.beginTransaction();
				try {
					Companies compa = updateForm.get();
					compa.id = id;
					compa.saveManyToManyAssociations("headCompanies");
					compa.update(id);
					flash("success", "Company " + updateForm.get().name + " has been updated");
					Ebean.commitTransaction();
				} catch (Exception e) {
					Logger.error("Company update error",e.fillInStackTrace());
					flash("error", "Error in updating company data");
					Ebean.rollbackTransaction();
				} finally {
					Ebean.endTransaction();
				}
		
		return redirect(routes.CompanyApplication.companyIndex(id));
	}
	
	/**
	 * 
	 * @param userId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result upload(Long companyId) {
		Long cBlid = -1L;
		MultipartFormData body = request().body().asMultipartFormData();
		FilePart uploadedFile = body.getFile("file");
		try {
			cBlid = myUtils.FileUtils.upload(uploadedFile, session().get("userName"));
		} catch (Exception e) {
			Logger.error("Company blob upload error",e.fillInStackTrace());
			flash("error","Can't upload file");
		}
		if(cBlid !=-1L) {
			Companies co = Companies.find.byId(companyId);
			co.companyLogo=Blobs.find.byId(cBlid);
			co.update();
		}
		return redirect(routes.CompanyApplication.companyIndex(companyId));
	}
	
	/**
	 * 
	 * @param userId
	 * @return
	 */
	public static Result renderImage(Long companyId) {
		Companies co = Companies.find.byId(companyId);
		byte[] image=null;
		InputStream imageInputStream = null;
		if(co.companyLogo!=null) {
			try {
				imageInputStream = new java.io.FileInputStream(co.companyLogo.blobFile);
				image = org.apache.commons.io.IOUtils.toByteArray(
						imageInputStream );
			} catch (IOException e) {
				Logger.error("logo render error",e.fillInStackTrace());
			} finally {
				IOUtils.closeQuietly(imageInputStream);
			}
			return ok(image).as("image/jpeg");
			} else return ok("Nothing");
	}
	
	/**
	 * Removes association and probably the file itself from equipment-blob many-to-many
	 * @param equipmentId
	 * @param blobId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result deleteAssociation(Long companyId) {
		Long blobId = -1L;
		Companies co = Companies.find.byId(companyId);
		Ebean.beginTransaction();
		try {
			blobId = co.companyLogo.id;
			co.companyLogo=null;
			co.update(companyId);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Company blob association removal error",e.fillInStackTrace());
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
			Logger.error("Physical file delete error(Comapnies)",e.fillInStackTrace());
		}
		return redirect(routes.CompanyApplication.companyIndex(companyId));
	}
}
