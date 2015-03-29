package controllers;

import static play.data.Form.form;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ValidationException;

import models.Blobs;
import models.InventoryEvents;
import models.PartHistory;
import models.Parts;
import models.SecurityRole;
import models.Users;
import models.maintenance.Maintenances;
import models.maintenance.MaintenancesParts;
import myUtils.DateTimeUtils;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.Dynamic;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;

@SubjectPresent
public class PartHistoryApplication extends Controller {
	
	/**
	 * shows ONE partHistory item
	 * @param partHistoryId
	 * @return
	 */
	public static Result partHistoryIndex(Long partHistoryId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		
		return ok(views.html.partHistory.partHistoryIndex.render(
				partHistoryId,
				PartHistory.find.byId(partHistoryId)
				));
				
	}
	
/**
 * Lists all partHistory of an item
 * @param parentLevel id of level for which we are creating history.
 * @param parentId
 * @return
 */
	public static Result list(Long parentId, int page, String sortBy,
			String order, String filter1, String filter2, String filter3, String filter4, String filter5
			, String filter6, String filter7, String filter8) {
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
		return ok(views.html.partHistory.partHistoryList.render(
				parentId, PartHistory.page(parentId, page, ppid, sortBy, order,
						filter1,filter2,filter3,filter4,filter5,filter6,filter7,filter8)
				, sortBy, order, filter1,filter2,filter3,filter4,filter5,filter6,filter7,filter8
				));
	}
	
	/**
	 * Display create partHistory form
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("warehouse")})
	public static Result createPartHistory(Long parentId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		List<PartHistory> fphs = PartHistory.find.where().eq("parentPart.id", parentId)
				.ne("eventType.id",0).ne("eventType.id",1)
				.orderBy("commenceDate").orderBy("id").findList();
		
		PartHistory filler;
		if(fphs.size()!=0)
			filler = fphs.get(fphs.size()-1);
		else filler = new PartHistory();
		filler.commenceDate = myUtils.DateTimeUtils.getIranianDateTimeAsDate(filler.commenceDate);
		Form<PartHistory> newPartHistoryForm = form(PartHistory.class).fill(filler);
		return ok(views.html.partHistory.partHistoryCreateForm.render(
					parentId, newPartHistoryForm
					));

	}
	
	/**
	 * Display create partHistory form
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("warehouse")})
	public static Result createRequestHistory() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		Form<PartHistory> newRequestHistoryForm = form(PartHistory.class);
		return ok(views.html.partHistory.partRequestCreateForm.render(
				newRequestHistoryForm
					));

	}
	@Restrict({ @Group("admin"), @Group("warehouse")})
	public static Result saveRequest() {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String,String> otherData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {
				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if(PartHistory.class.getField(key).getType()
								.toString()
								.compareTo("class java.util.Date") == 0) {
							otherData.put(key,DateTimeUtils.getGregorianDateTimeAsString(value[0]).toString());
						} else {
							if (key.contains("parentPart")) newData.put(key,value[0]);
							else otherData.put(key, value[0]);
						}
					} catch (NoSuchFieldException | SecurityException e) {
						otherData.put(key, value[0]);
						}
				} else if (value.length > 1) {
					for (int i = 0; i < value.length; i++) {
						newData.put(key + "[" + i + "]", value[i]);
					}
				}
			}
		}
		Ebean.beginTransaction();
		try {
			for(String key : newData.keySet()) {
				PartHistory phkey = new PartHistory();
				phkey.parentPart = Parts.find.byId(Long.valueOf(newData.get(key)));
				phkey.receiptNumber = otherData.get("receiptNumber");
				phkey.requestNumber = otherData.get("requestNumber");
				phkey.registrar = Users.findByUserName(session().get("userName"));
				DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY/MM/dd HH:mm");
				phkey.commenceDate = new DateTime(formatter.parseDateTime(
						otherData.get("commenceDate"))).toDate();
				phkey.comments = otherData.get("comments");
				phkey.stockBalance = Float.valueOf(otherData.get("stockBalance"));
				InventoryEvents phEvent = InventoryEvents.find.byId(Long.valueOf(otherData.get("eventType.id")));
				phkey.eventType = phEvent;
				if(phEvent.id==0L) {
					phkey.parentPart.remainingQuantity += phkey.stockBalance;
					phkey.remainingStock=phkey.parentPart.remainingQuantity;
				} else if(phEvent.id==1L){
					if(phkey.parentPart.remainingQuantity < phkey.stockBalance)
						throw new ValidationException("Not enough of this part in stock");
					phkey.parentPart.remainingQuantity -= phkey.stockBalance;
					phkey.remainingStock=phkey.parentPart.remainingQuantity;
				}
				phkey.parentPart.update();
				try {
					phkey.offeredUnitPrice = Float.valueOf(otherData.get("offeredUnitPrice"));
				} catch (Exception NumberFormatException) {
				   phkey.offeredUnitPrice = 0F;
				}
				phkey.requester = Users.find.byId(Long.valueOf(otherData.get("requester.id")));
				phkey.save();
				flash("success","Request placed");
			}
			Ebean.commitTransaction();
		} catch (Exception ex) {
			Logger.error("Request creation error",ex.fillInStackTrace());
			Ebean.rollbackTransaction();
			flash("error", "Request not created");
		} finally {
			Ebean.endTransaction();
		}
		
		return redirect(routes.PartHistoryApplication.list(-1L,0,"eventType","asc","","","","","","","",""));
	}
	
	/**
	 * 
	 * @param mainteanancePartId
	 * @param maintenanceId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("warehouse"), @Group("planner")})
    public static Result updateStockParts(Long mainteanancePartId, Long maintenanceId) {
    	MaintenancesParts mp = MaintenancesParts.find.byId(mainteanancePartId);
    	if(mp.stockFlag==true)
    	return redirect(routes.MaintenanceApplication.maintenanceIndex(maintenanceId)+ "#tabs-maintenanceReport");
    	
    	Parts par = Parts.find.byId(mp.part.id);
		if((par.remainingQuantity - mp.quantity) < 0) {
			flash("error","Not enough " + par.name + " in stock");
			return redirect(routes.MaintenanceApplication.maintenanceIndex(maintenanceId)+ "#tabs-maintenanceReport");
			}
    	PartHistory ph = new PartHistory();
		Ebean.beginTransaction();
		try {
			ph.parentPart=par;
			ph.parentMaintenance=Maintenances.find.byId(maintenanceId);
			ph.registrar=Users.findByUserName(session().get("userName"));
			ph.requester=Users.findByUserName(session().get("userName"));
			ph.eventType=InventoryEvents.find.byId(1L);
			float q, rq;
			q = mp.quantity;
			rq = par.remainingQuantity;
			ph.stockBalance= q;
			//par.remainingQuantity = rq -  q;
			ph.remainingStock= rq -  q;
			ph.commenceDate = new Date();
			ph.save();
			
			Ebean
	         .createUpdate(Parts.class, "UPDATE parts SET remaining_quantity=:requ WHERE id=:id")
	         .setParameter("requ", rq-q)
	         .setParameter("id", mp.part.id)
	         .execute();
			
			//par.update();
			Ebean
	         .createUpdate(MaintenancesParts.class, "UPDATE maintenances_parts SET stock_flag=true WHERE id=:id")
	         .setParameter("id", mp.id)
	         .execute();
			
			//mp.stockFlag=true;
			//mp.update();
			flash("success", mp.quantity + " of " + par.name + " received from warehouse");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("Error in stock update",e.fillInStackTrace());
			flash("error", "Part not deducted from warehouse stock");
			Ebean.rollbackTransaction();
		} finally {
			Ebean.endTransaction();
		}
		return redirect(routes.MaintenanceApplication.maintenanceIndex(maintenanceId) + "#tabs-maintenanceReport");
    }
	/**
	 * save new partHistory
	 * @param parentLevel
	 * @param parentId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("warehouse")})
	public static Result save(Long parentId) {
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (PartHistory.class.getField(key).getType()
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
						if(PartHistory.class.getField(key).getType()
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
		Form<PartHistory> newPartHistoryForm = new Form<PartHistory>(
				PartHistory.class).bind(newData);
		if (newPartHistoryForm.hasErrors()) {
			return badRequest(views.html.partHistory.partHistoryCreateForm
					.render(parentId,newPartHistoryForm));
		}
		
			PartHistory his = newPartHistoryForm.get();
			Ebean.beginTransaction();
			try {
				his.registrar=Users.findByUserName(session().get("userName"));
				his.parentPart = Parts.find.byId(parentId);
				if(his.eventType.equals(InventoryEvents.find.byId(0L))) {
					his.parentPart.remainingQuantity += his.stockBalance;
					his.remainingStock=his.parentPart.remainingQuantity;
				} else if(his.eventType.equals(InventoryEvents.find.byId(1L))){
					if(his.parentPart.remainingQuantity < his.stockBalance)
						throw new ValidationException("Not enough of this part in stock");
					his.parentPart.remainingQuantity -= his.stockBalance;
					his.remainingStock=his.parentPart.remainingQuantity;
				}
				his.parentPart.update();
				his.save();
				flash("success","Part received");
				Ebean.commitTransaction();
			} catch  (Exception e) {
				Logger.error("History save error",e.fillInStackTrace());
				flash("error", "History not created");
				Ebean.rollbackTransaction();
			} finally {
				Ebean.endTransaction();
			}
			
			return redirect(routes.PartHistoryApplication.list(parentId,0,"eventType","asc","","","","","","","",""));
	}

	
	/**
	 * Display edit partHistory form
	 * @param partHistoryId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("warehouse")})
	public static Result editPartHistory(Long partHistoryId) {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
			PartHistory his = PartHistory.find.byId(partHistoryId);
			his.commenceDate = myUtils.DateTimeUtils.getIranianDateTimeAsDate(his.commenceDate);
			Form<PartHistory> editpartHistoryForm = form(PartHistory.class).fill(his);
			return ok(views.html.partHistory.partHistoryEdit.render(
					partHistoryId, editpartHistoryForm
					));

	}
	
	/**
	 * update partHistory
	 * @param partHistoryId
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("warehouse")})
	public static Result update(Long partHistoryId) {
		if( (!PartHistory.find.byId(partHistoryId).registrar.id
				.equals(Users.findByUserName(session().get("userName")).id)) &&
				!(Users.findByUserName(session().get("userName"))
						.roles.contains(SecurityRole.findByName("admin")))
				) {
			flash("error","Only log creator or admin can edit it");
			return redirect(routes.PartHistoryApplication.partHistoryIndex(partHistoryId));
		}
		Map<String, String> newData = new HashMap<String, String>();
		Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
				.body().asFormUrlEncoded();
		if (urlFormEncoded != null) {
			for (String key : urlFormEncoded.keySet()) {

				String[] value = urlFormEncoded.get(key);
				if (value.length == 1) {
					try {
						if (PartHistory.class.getField(key).getType()
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
						if(PartHistory.class.getField(key).getType()
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
		
		Form<PartHistory> editPartHistory = new Form<PartHistory>(
				PartHistory.class).bind(newData);
		
			if (editPartHistory.hasErrors()) {
				return badRequest(views.html.partHistory.partHistoryEdit.render(
						partHistoryId, editPartHistory));
			}
			PartHistory his = editPartHistory.get();
			Ebean.beginTransaction();
			try {
			if(
				(InventoryEvents.find.byId(his.eventType.id).id == 1L
				|| InventoryEvents.find.byId(his.eventType.id).id==0L)
					&&
				(PartHistory.find.byId(partHistoryId).eventType.otherLanguageName !=
				InventoryEvents.find.byId(his.eventType.id).otherLanguageName)) {
				throw new ValidationException("Can't change to these two events. Please create" +
						" new events for delivered or received stock");
			}
					
			his.update(partHistoryId);
			Ebean.commitTransaction();
			flash("success","History updated");
			} catch (Exception e) {
				Logger.error("Part history update error",e.fillInStackTrace());
				flash("error","Can't change to these two events. Please create" +
						" new events for delivered or received stock");
				Ebean.rollbackTransaction();
			} finally {
				Ebean.endTransaction();
			}
		return redirect(routes.PartHistoryApplication.partHistoryIndex(partHistoryId));
	}
	
	/**
	 * 
	 * @param ppid
	 * @param phid
	 * @return
	 */
	@Restrict({ @Group("admin"),@Group("warehouse")})
	public static Result delete(Long ppid, Long phid) {
		try {
			PartHistory pahi = PartHistory.find.byId(phid);
			if (pahi.attachedDoc != null)
				throw new ValidationException("Please delete attached file first");
			PartHistory.find.ref(phid).delete();
			flash("success","History deleted");
		} catch (Exception exc) {
			Logger.error("PartHistory blob delete error",exc.fillInStackTrace());
			flash("error","Can't delete part history file");
		}
		
		return redirect(routes.PartHistoryApplication.list(ppid, 0,"commenceDate","asc","","","","","","","",""));
	}
	
	/**
	 * 
	 * @param parthid
	 * @return
	 */
	@Restrict({ @Group("admin"), @Group("warehouse"), @Group("planner") })
	public static Result upload(Long partHid) {
		Long cBlid = -1L;
		MultipartFormData body = request().body().asMultipartFormData();
		FilePart uploadedFile = body.getFile("file");
		try {
			cBlid = myUtils.FileUtils.upload(uploadedFile, session().get("userName"));
		} catch (Exception e) {
			Logger.error("History blob upload error",e.fillInStackTrace());
			flash("error","Can't upload file for log");
		}
		if(cBlid !=-1L) {
			PartHistory co = PartHistory.find.byId(partHid);
			co.attachedDoc=Blobs.find.byId(cBlid);
			co.update();
		}
		return redirect(routes.PartHistoryApplication.partHistoryIndex(partHid));
	}
	
	/**
	 * Removes association and probably the file itself from equipment-blob many-to-many
	 * @param equipmentId
	 * @param blobId
	 * @return
	 */
	@Dynamic(value="deleteFile")
	public static Result deleteAssociation(Long partHid, Long blobId) {
		PartHistory co = PartHistory.find.byId(partHid);
		Ebean.beginTransaction();
		try {
			//blobId = co.attachedDoc.id;
			co.attachedDoc=null;
			co.update(partHid);
			flash("success","Association removed. Will try to delete file also...");
			Ebean.commitTransaction();
		} catch (Exception e) {
			Logger.error("History blob association removal error",e.fillInStackTrace());
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
			Logger.error("History physical file error",e.fillInStackTrace());
		}
		return redirect(routes.PartHistoryApplication.partHistoryIndex(partHid));
	}
}
