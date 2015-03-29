package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import models.Components;
import models.Datasheet;
import models.Parts;
import models.Sections;
import models.Subunits;
import models.equipment.Equipments;
import models.maintenance.Maintenances;
import myUtils.DateTimeUtils;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;

@SubjectPresent
public class SearchApplication extends Controller {
	
	public static Result searchForm() {
		List<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
		return ok(views.html.searchForm.render("tabs-sections",new DynamicForm(), results));
	}
	
	/**
	 * 
	 * @param stype
	 * @param page
	 * @return
	 */
	public static Result resultRenderer(String stype) {
		DynamicForm searchData = Form.form().bindFromRequest();
		List<ArrayList<String>> results;
		Integer page=0;
		String tabSelector =new String();
		
		try {
			page = Integer.valueOf(searchData.get("page"));
		} catch (Exception x) {
			page=0;
		}
		
		if(page<0) page=0;
		int ppid = 15;
		if(session().containsKey("pagesize")) {
			try {
			ppid = Integer.valueOf(session().get("pagesize"));
			} catch (Exception eex) {
				ppid = 10;
			}
		}
		boolean like =  searchData.get("searchType").equals("generalSearch") ? true : false;
		
		try {
			switch(stype) {
			case "sections":
				results = sectionSearch(searchData, like, page,ppid);
				tabSelector = "tabs-sections";
				break;
			case "equipments":
				results = equipmentSearch(searchData, like, page,ppid);
				tabSelector = "tabs-equipments";
				break;
			case "subunits":
				results = subunitSearch(searchData, like, page,ppid);
				tabSelector = "tabs-subunits";
				break;
			case "components":
				results = componentSearch(searchData, like, page,ppid);
				tabSelector = "tabs-components";
				break;
			case "parts":
				results = partSearch(searchData, like, page,ppid);
				tabSelector = "tabs-parts";
				break;
			case "datasheets":
				results = datasheetSearch(searchData, like, page,ppid);
				tabSelector = "tabs-datasheets";
				break;
			case "maintenances":
				results = maintenanceSearch(searchData, like, page,ppid);
				tabSelector = "tabs-maintenances";
				break;
			default:
				results = new ArrayList<ArrayList<String>>();
			} 
		} catch (Exception searchException) {
			Logger.error("Search failed", searchException.fillInStackTrace());
			return ok(views.html.searchForm.render("tabs-sections", searchData, new ArrayList<ArrayList<String>>()));
			}
		
		return ok(views.html.searchForm.render(tabSelector, searchData, results));
	}
	
	/**
	 * 
	 * @param searchData
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<ArrayList<String>> sectionSearch(DynamicForm searchData, boolean like, int page, int pageSize) throws Exception {
		try {
			String whosql = new String();
			Query<Sections> query = Ebean.createQuery(Sections.class);
			Map<String, String> searchMap = searchData.data();
			searchMap.remove("searchType");
			searchMap.remove("stype");
			searchMap.remove("page");
			
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					if(like==true) {
						whosql += "lower("+key+")" + " like :filter" +key + " AND ";
					} else {
						whosql += key + " = :filter" + key + " AND ";
					}
				}
			}
			if(whosql.length()>6) whosql = whosql.substring(0,whosql.length()-5);
			query.where(whosql);
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					if(like==true) {
						query.setParameter("filter"+key, "%"+searchMap.get(key)+"%");
					} else {
						query.setParameter("filter"+key, searchMap.get(key));
					}
					
				}
			}
			List<Sections> resultPage = query.select("id,name,description").findPagingList(pageSize).setFetchAhead(false).getPage(page).getList();
					
			List<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
			Integer total = query.findRowCount();
			results.add(new ArrayList<String>(Arrays.asList("#","Number of results:",total.toString())));
			for(Sections s: resultPage) {
				ArrayList<String> slist = new ArrayList<String>();
				slist.add(routes.SectionApplication.sectionIndex(s.id).toString());
				slist.add(s.name);
				slist.add(s.description);
				results.add(slist);
			}		
			
			return results;
		} finally {
			//Do nothing
		}
	}
	
	/**
	 * 
	 * @param searchData
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<ArrayList<String>> equipmentSearch(DynamicForm searchData, boolean like, int page, int pageSize) throws Exception {
		try {
			String whosql = new String();
			Query<Equipments> query = Ebean.createQuery(Equipments.class);
			Map<String, String> searchMap = searchData.data();
			searchMap.remove("searchType");
			searchMap.remove("stype");
			searchMap.remove("page");
			
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					if(key.equals("equipmentMaintenanceAspects")) {
							whosql += "(lower(maintenaceMethod)" + " like :filterequipmentMaintenanceAspects OR " +
								"lower(maintenaceSafetyRequirements)" + " like :filterequipmentMaintenanceAspects) AND ";
					} else if(key.equals("equipmentOperationAspects")) {
						whosql += "(lower(operationMethod)" + " like :filterequipmentOperationAspects OR " +
								"lower(operationSafetyRequirements)" + " like :filterequipmentOperationAspects) AND ";
					} else if(key.equals("equipmentHseAspects")) {
						whosql += "(lower(hseRisks)" + " like :filterequipmentHseAspects OR " +
								"lower(safetyRisks)" + " like :filterequipmentHseAspects) AND ";
					} else {
						String newKey = key;
						if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
						if(like==true) {
							whosql += "lower("+key+")" + " like :filter" +newKey + " AND ";
						} else {
							whosql += key + " = :filter" + newKey + " AND ";
						}
					}
					
				}
			}
			
			if(whosql.length()>6) whosql = whosql.substring(0,whosql.length()-5);
			query.where(whosql);
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					String newKey = key;
					if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
					if(like==true) {
						query.setParameter("filter"+newKey, "%"+searchMap.get(key)+"%");
					} else {
						query.setParameter("filter"+newKey, searchMap.get(key));
					}
					
				}
			}
			
			List<Equipments> resultPage = query.select("id,name,description").findPagingList(pageSize).setFetchAhead(false).getPage(page).getList();
			List<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
			Integer total = query.findRowCount();
			results.add(new ArrayList<String>(Arrays.asList("#","Number of results:",total.toString())));
			for(Equipments s: resultPage) {
				ArrayList<String> slist = new ArrayList<String>();
				slist.add(routes.EquipmentApplication.equipmentIndex(s.id).toString());
				slist.add(s.name);
				slist.add(s.description);
				results.add(slist);
			}
			return results;
		} finally {
			//Do nothing
		}
	}
	
	/**
	 * 
	 * @param searchData
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<ArrayList<String>> subunitSearch(DynamicForm searchData, boolean like, int page, int pageSize) throws Exception {
		try {
			String whosql = new String();
			Query<Subunits> query = Ebean.createQuery(Subunits.class);
			Map<String, String> searchMap = searchData.data();
			searchMap.remove("searchType");
			searchMap.remove("stype");
			searchMap.remove("page");
			
			for(String key : searchMap.keySet()) {
				String newKey = key;
				if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
				if(searchMap.get(key).length()>0) {
					if(like==true) {
						whosql += "lower("+key+")" + " like :filter" +newKey + " AND ";
					} else {
						whosql += key + " = :filter" + newKey + " AND ";
					}
				}
			}
			if(whosql.length()>6) whosql = whosql.substring(0,whosql.length()-5);
			query.where(whosql);
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					String newKey = key;
					if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
					if(like==true) {
						query.setParameter("filter"+newKey, "%"+searchMap.get(key)+"%");
					} else {
						query.setParameter("filter"+newKey, searchMap.get(key));
					}
					
				}
			}
			List<Subunits> resultPage = query.select("id,name,description").findPagingList(pageSize).setFetchAhead(false).getPage(page).getList();
					
			List<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
			Integer total = query.findRowCount();
			results.add(new ArrayList<String>(Arrays.asList("#","Number of results:",total.toString())));
			for(Subunits s: resultPage) {
				ArrayList<String> slist = new ArrayList<String>();
				slist.add(routes.SubunitApplication.subunitIndex(s.id).toString());
				slist.add(s.name + " in equipment: " + s.equipment.name + " in section: " + s.equipment.section.name);
				slist.add(s.description);
				results.add(slist);
			}
			return results;
			} finally {
				//Do nothing
			}
		
	}
	
	/**
	 * 
	 * @param searchData
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<ArrayList<String>> componentSearch(DynamicForm searchData, boolean like, int page, int pageSize) throws Exception {
		try {
			String whosql = new String();
			Query<Components> query = Ebean.createQuery(Components.class);
			Map<String, String> searchMap = searchData.data();
			searchMap.remove("searchType");
			searchMap.remove("stype");
			searchMap.remove("page");
			
			for(String key : searchMap.keySet()) {
				String newKey = key;
				if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
				if(searchMap.get(key).length()>0) {
					if(like==true) {
						whosql += "lower("+key+")" + " like :filter" +newKey + " AND ";
					} else {
						whosql += key + " = :filter" + newKey + " AND ";
					}
				}
			}
			if(whosql.length()>6) whosql = whosql.substring(0,whosql.length()-5);
			query.where(whosql);
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					String newKey = key;
					if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
					if(like==true) {
						query.setParameter("filter"+newKey, "%"+searchMap.get(key)+"%");
					} else {
						query.setParameter("filter"+newKey, searchMap.get(key));
					}
					
				}
			}
			List<Components> resultPage = query.select("id,name,description").findPagingList(pageSize).setFetchAhead(false).getPage(page).getList();
					
			List<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
			Integer total = query.findRowCount();
			results.add(new ArrayList<String>(Arrays.asList("#","Number of results:",total.toString())));
			for(Components s: resultPage) {
				ArrayList<String> slist = new ArrayList<String>();
				slist.add(routes.ComponentApplication.componentIndex(s.id).toString());
				slist.add(s.name + " in subunit: " + s.subunit.name+ " in equipment: " + s.subunit.equipment.name);
				slist.add(s.description);
				results.add(slist);
			}
			return results;
			} finally {
				//Do nothing
			}
	}
	
	/**
	 * 
	 * @param searchData
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<ArrayList<String>> partSearch(DynamicForm searchData, boolean like, int page, int pageSize) throws Exception {
		try {
			String whosql = new String();
			Query<Parts> query = Ebean.createQuery(Parts.class);
			Map<String, String> searchMap = searchData.data();
			searchMap.remove("searchType");
			searchMap.remove("stype");
			searchMap.remove("page");
			
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					if(key.equals("partMaterial")) {
							whosql += "(lower(material1)" + " like :filterpartMaterial OR " +
								"lower(material2)" + " like :filterpartMaterial OR lower(material3) like :filterpartMaterial) AND ";
					} else {
						String newKey = key;
						if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
						if(like==true) {
							whosql += "lower("+key+")" + " like :filter" +newKey + " AND ";
						} else {
							whosql += key + " = :filter" + newKey + " AND ";
						}
					}
					
				}
			}
			
			if(whosql.length()>6) whosql = whosql.substring(0,whosql.length()-5);
			query.where(whosql);
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					String newKey = key;
					if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
					if(like==true) {
						query.setParameter("filter"+newKey, "%"+searchMap.get(key)+"%");
					} else {
						query.setParameter("filter"+newKey, searchMap.get(key));
					}
					
				}
			}
			
			List<Parts> resultPage = query.select("id,name,description").findPagingList(pageSize).setFetchAhead(false).getPage(page).getList();
			List<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
			Integer total = query.findRowCount();
			results.add(new ArrayList<String>(Arrays.asList("#","Number of results:",total.toString())));
			for(Parts s: resultPage) {
				ArrayList<String> slist = new ArrayList<String>();
				slist.add(routes.PartApplication.partIndex(s.id).toString());
				slist.add(s.name);
				slist.add(s.description);
				results.add(slist);
			}
			return results;
		} finally {
			//Do nothing
		}
	}
	
	/**
	 * 
	 * @param searchData
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<ArrayList<String>> datasheetSearch(DynamicForm searchData, boolean like, int page, int pageSize) throws Exception {
		try {
			String whosql = new String();
			Query<Datasheet> query = Ebean.createQuery(Datasheet.class);
			Map<String, String> searchMap = searchData.data();
			searchMap.remove("searchType");
			searchMap.remove("stype");
			searchMap.remove("page");
			
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					if(like==true) {
						whosql += "lower("+key+")" + " like :filter" +key + " AND ";
					} else {
						whosql += key + " = :filter" + key + " AND ";
					}
				}
			}
			if(whosql.length()>6) whosql = whosql.substring(0,whosql.length()-5);
			query.where(whosql);
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					if(like==true) {
						query.setParameter("filter"+key, "%"+searchMap.get(key)+"%");
					} else {
						query.setParameter("filter"+key, searchMap.get(key));
					}
					
				}
			}
			List<Datasheet> resultPage = query.findPagingList(pageSize).setFetchAhead(false).getPage(page).getList();
			
			List<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
			Integer total = query.findRowCount();
			results.add(new ArrayList<String>(Arrays.asList("#","Number of results:",total.toString())));
			for(Datasheet s: resultPage) {
				ArrayList<String> slist = new ArrayList<String>();
				if(s.parentHistory==null) {
					if(s.parentInstallation!=null) {
						slist.add(routes.DatasheetApplication.list(3, s.parentInstallation.id, 0, "parameter", "asc", "").toString());
						slist.add("Reference datasheet: "+ s.parentInstallation.name);
					}
					if(s.parentPlant!=null) {
						slist.add(routes.DatasheetApplication.list(4, s.parentPlant.id, 0, "parameter", "asc", "").toString());
						slist.add("Reference datasheet: "+ s.parentPlant.name);
					}
					if(s.parentSection!=null) {
						slist.add(routes.DatasheetApplication.list(5 ,s.parentSection.id, 0, "parameter", "asc", "").toString());
						slist.add("Reference datasheet: "+ s.parentSection.name);
					}
					if(s.parentEquipment!=null) {
						slist.add(routes.DatasheetApplication.list(6, s.parentEquipment.id, 0, "parameter", "asc", "").toString());
						slist.add("Reference datasheet: "+ s.parentEquipment.name);
					}
					if(s.parentSubunit!=null) {
						slist.add(routes.DatasheetApplication.list(7, s.parentSubunit.id, 0, "parameter", "asc", "").toString());
						slist.add("Reference datasheet: "+ s.parentSubunit.name);
					}
					if(s.parentComponent!=null) {
						slist.add(routes.DatasheetApplication.list(8, s.parentComponent.id, 0, "parameter", "asc", "").toString());
						slist.add("Reference datasheet: "+ s.parentComponent.name);
					}
					if(s.parentPart!=null) {
						slist.add(routes.DatasheetApplication.list(3, s.parentPart.id, 0, "parameter", "asc", "").toString());
						slist.add("Reference datasheet: "+ s.parentPart.name);
					}
					if(slist.size()<2) throw new Exception();
					slist.add(s.parameter + ": " + s.value);
				} else {
					slist.add(routes.HistoryApplication.historyIndex(s.parentHistory.id).toString());
					slist.add("Recorded on:" + myUtils.DateTimeUtils.getIranianDate(s.parentHistory.start));
					slist.add(s.parameter + ": " + s.value);
				}
				
				results.add(slist);
			}		
			
			return results;
		} finally {
			//Do nothing
		}
	}
	
	/**
	 * 
	 * @param searchData
	 * @param page
	 * @param pageSize
	 * @return
	 */
	public static List<ArrayList<String>> maintenanceSearch(DynamicForm searchData, boolean like, int page, int pageSize) throws Exception {
		try {
			String whosql = new String();
			Query<Maintenances> query = Ebean.createQuery(Maintenances.class);
			Map<String, String> searchMap = searchData.data();
			searchMap.remove("searchType");
			searchMap.remove("stype");
			searchMap.remove("page");
			
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					if(key.equals("maintenancePmRoutines")) {
							whosql += "(lower(pmRoutine.name)" + " like :filtermaintenancePmRoutines OR " +
								"lower(pmRoutine.description)" + " like :filtermaintenancePmRoutines OR " +
								"lower(classLevelPMRoutine.name)" + " like :filtermaintenancePmRoutines OR " +
								"lower(classLevelPMRoutine.description)" + " like :filtermaintenancePmRoutines) AND ";
					} else if(key.equals("maintenanceRequestDate")) {
						whosql += "(requestDate between :filtermaintenanceRequestDate1 AND :filtermaintenanceRequestDate2) AND ";
					} else {
						String newKey = key;
						if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
						if(like==true) {
							whosql += "lower("+key+")" + " like :filter" +newKey + " AND ";
						} else {
							whosql += key + " = :filter" + newKey + " AND ";
						}
					}
					
				}
			}
			
			if(whosql.length()>6) whosql = whosql.substring(0,whosql.length()-5);
			query.where(whosql);
			for(String key : searchMap.keySet()) {
				if(searchMap.get(key).length()>0) {
					if(key.equals("maintenanceRequestDate")) {
						String regexDate = searchMap.get("maintenanceRequestDate");
						Date start = new Date(), end = new Date();
						if(regexDate.matches("(13|14)\\d\\d(/)(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])-"
								+ "(13|14)\\d\\d\\2(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])")) {
									String[] dates = regexDate.split("-");
									start = DateTimeUtils.getGregorianDateTime(dates[0] + " 00:00");
									end = DateTimeUtils.getGregorianDateTime(dates[1] + " 00:00");
									}
						query.setParameter("filtermaintenanceRequestDate1",start);
						query.setParameter("filtermaintenanceRequestDate2", end);
					} else {
						String newKey = key;
						if(key.contains(".")) newKey= key.split("\\.")[0]+key.split("\\.")[1];
						if(like==true) {
							query.setParameter("filter"+newKey, "%"+searchMap.get(key)+"%");
						} else {
							query.setParameter("filter"+newKey, searchMap.get(key));
						}
					}
				}
			}
		
			
			Integer total = query.findRowCount();
			List<Maintenances> resultPage = query.findPagingList(pageSize).setFetchAhead(false).getPage(page).getList();
			List<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
			results.add(new ArrayList<String>(Arrays.asList("#","Number of results:",total.toString())));
			for(Maintenances s: resultPage) {
				ArrayList<String> slist = new ArrayList<String>();
				slist.add(routes.MaintenanceApplication.maintenanceIndex(s.id).toString());
				slist.add(s.workOrderSerial);
				String ss = s.maintainedEquipment==null ? s.maintainedSection.name : s.maintainedEquipment.name;
				slist.add(s.workOrderDescription + "---"+ss);
				results.add(slist);
			}
			return results;
		} finally {
			//Do nothing
		}
	}
	
}