package controllers;

import static play.data.Form.form;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Blobs;
import models.Plants;
import models.Sections;
import models.maintenance.Maintenances;
import play.Logger;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SubjectPresent
public class SectionApplication extends Controller {
    
    /**
     * Information about one section
     * @param id Id of the section
     * @return Result of render(Sections.find.byId(id))
     */
    public static Result sectionIndex(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		
		return ok(views.html.sections.sectionIndex.render(
				Sections.find.byId(id)
				));
	}
    
    /**
     * 
     * @param id
     * @return
     */
    public static Result allSectionMaints(Long id) {
    	ArrayList<ObjectNode> sectMaints = new ArrayList<ObjectNode>();
    	
    	String whoql =
				"maintainedSection.id = :id OR " +
				"maintainedEquipment.section.id = :id";
		com.avaje.ebean.Query<Maintenances> query = Ebean.createQuery(Maintenances.class);
		query.where(whoql).
		setParameter("id",id);
		List<Maintenances> smaints = query.orderBy("requestDate desc").findList();
		for(Maintenances m : smaints) {
			ObjectNode on = Json.newObject();
			on.put("date",myUtils.DateTimeUtils.getIranianDate(m.requestDate));
			on.put("orderNumber", m.workOrderSerial);
			on.put("maintLink","/maint?maintID="+m.id.toString());
			on.put("maintStatus",m.maintenanceStatus.toString());
			sectMaints.add(on);
		}
		ObjectNode last = Json.newObject();
		last.put("date", "Count");
		last.put("orderNumber","-");
		last.put("maintLink","#");
		last.put("maintStatus",smaints.size());
		sectMaints.add(last);
		return ok(play.libs.Json.toJson(sectMaints));
    }
	/**
	 * Returns a list of 10 sections for use in sectionList view
	 * @param page
	 * @param sortBy
	 * @param order
	 * @param filter
	 * @return
	 */
		public static Result list(Long parentPlantId,
				int page, String sortBy, String order, String filter) {
			int ppid = 10;
			if(session().containsKey("pagesize")) {
				try {
				ppid = Integer.valueOf(session().get("pagesize"));
				} catch (Exception eex) {
					ppid = 10;
				}
			}
			return ok(
					views.html.sections.sectionList.render(
							parentPlantId,
							Sections.page(parentPlantId, page, ppid, sortBy , order, filter),
							sortBy, order, filter
							));
		}

	/**
	 * Displays section create form
	 * @return form display
	 */
	@Restrict({@Group("admin"),
		@Group("creator")})
	public static Result createSection(Long parentPlantId) {
		Form<Sections> newSectionForm = form(Sections.class);
		return ok(views.html.sections.createSectionForm.render(
				parentPlantId,newSectionForm
				));
	}

	/**
	 * Saves a section
	 * @return table of all sections
	 */
	@Restrict({@Group("admin"),
		@Group("creator")})
	public static Result save(Long parentPlantId) {
		Form<Sections> newSectionForm = form(Sections.class).bindFromRequest();
		if (newSectionForm.hasErrors()) {
			return badRequest(views.html.sections.createSectionForm.render(
					parentPlantId,newSectionForm
					));
		}
		Ebean.beginTransaction();
		try {
			Sections s = newSectionForm.get();
			s.plant = Plants.find.byId(parentPlantId);
			s.save();
			flash("success", "Section " + newSectionForm.get().name + " has been created");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Section save error",e.fillInStackTrace());
			flash("error", "Section save error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.SectionApplication.list(parentPlantId,0,"name","asc",""));
	}
	
	/**
	 * display edit section form
	 * @param id Id of section
	 * @return form display
	 */
	@Restrict({@Group("admin"),
		@Group("editor")})
	public static Result editSection(Long id) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<Sections> sectionForm = form(Sections.class).fill(
				Sections.find.byId(id));

		return ok(views.html.sections.editSection.render(id, sectionForm));
	}

	/**
	 * Updates section
	 * @param id Id of section
	 * @return table of all sections
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
						if(Sections.class.getField(key).getType().toString().compareTo
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
		Form<Sections> updateForm = new Form<Sections>(Sections.class).bind(newData);
		if (updateForm.hasErrors()) {
			return badRequest(views.html.sections.editSection.render(id,updateForm));
		}
		
		Ebean.beginTransaction();
		try {
			Sections s = updateForm.get();
			s.id=id;
			s.saveManyToManyAssociations("files");
			s.update(id);
			flash("success", "Section " + updateForm.get().name + " has been updated");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Section update error",e.fillInStackTrace());
			flash("error", "Section update error");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect((routes.SectionApplication.sectionIndex(id)));
	}
	
	/**
	 * Uploads file directly for current section
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result upload(Long sectionId) {
		MultipartFormData body = request().body().asMultipartFormData();
			FilePart uploadedFile = body.getFile("file");
			String un = session().get("userName");
			Sections eq = Sections.find.byId(sectionId);
			try {
				eq.files.add(
						Blobs.find.byId(myUtils.FileUtils.upload(uploadedFile, un)));
				eq.saveManyToManyAssociations("files");
				eq.update();
				
			} catch (IOException e1) {
				Logger.error("Section blob association error",e1.fillInStackTrace());
				flash("error","Can't upload file for section");
				
			} catch(IllegalStateException e2) {
				Blobs fblob = Blobs.find.where().eq("name",uploadedFile.getFilename()).findUnique();
				if(!eq.files.contains(fblob)) {
					eq.files.add(fblob);
					eq.saveManyToManyAssociations("files");
					eq.update();
					flash("success",e2.getMessage()+" File added to data source anyway.");
					
				} else flash("error", e2.getMessage()+ " File already is a data source");
				
			}
		 	return redirect(routes.SectionApplication.sectionIndex(sectionId));
	}
	
	@Restrict({ @Group("admin"), @Group("creator"), @Group("editor") })
	public static Result deleteAssociation(Long sectionId, Long blobId) {
		Sections eq = Sections.find.byId(sectionId);
		Ebean.beginTransaction();
		try {
			eq.files.remove(
					Blobs.find.byId(blobId)
					);
			eq.saveManyToManyAssociations("files");
			eq.update(sectionId);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Section blob association removal",e.fillInStackTrace());
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
			Logger.error("Physical file removal error(Sections)",e.fillInStackTrace());
		}
		return redirect(routes.SectionApplication.sectionIndex(sectionId));
	}

}
