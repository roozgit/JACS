package controllers;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import models.CustomBean;
import models.Disciplines;
import models.History;
import models.Plants;
import models.Sections;
import models.equipment.Criticality;
import models.equipment.Equipments;
import models.maintenance.HoldReasons;
import models.maintenance.MaintenanceCategories;
import models.maintenance.MaintenanceGroups;
import models.maintenance.MaintenanceStatus;
import models.maintenance.Maintenances;
import models.maintenance.MaintenancesParts;
import models.maintenance.PreventiveMaintenances;
import myUtils.DateTimeUtils;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapArrayDataSource;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleGraphics2DExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.avaje.ebean.Expr;

@SubjectPresent
public class ReportApplication extends Controller {

	static String REPORT_DEFINITION_PATH = "./reports/MyReports/";

	
	public static Result reportSelector() {
		response().setHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response().setHeader("Pragma", "no-cache");
		return ok(views.html.reports.reportSelect.render());
	}
	
	public static Result displayReport() {
			
			Map<String, String> newData = new HashMap<String, String>();
			Map<String, String[]> urlFormEncoded = play.mvc.Controller.request()
					.body().asFormUrlEncoded();
			
			if (urlFormEncoded != null) {
				for (String key : urlFormEncoded.keySet()) {
					try {
						if(key.equals("start") || key.equals("end")) {
							newData.put(key,DateTimeUtils.getGregorianDateTimeAsString(urlFormEncoded.get(key)[0].toString()));
						} else {
							newData.put(key, urlFormEncoded.get(key)[0]);
						}
					} catch (SecurityException e) {
						newData.put(key, "");
						}
				}
			}
		    String reportFormat = newData.get("reportFormat");
		    String reportName = newData.get("reportName");
		    Long disciplineId = Long.valueOf(newData.get("disciplineSelect"));
		    String start = newData.get("start");
		    String end = newData.get("end");
		    
		    DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY/MM/dd");
		    DateTime s1;
		    DateTime e1;
		    
		    try {
				s1  = formatter.parseDateTime(start);
				e1 = formatter.parseDateTime(end).plusDays(1); //To include the whole end date
			} catch (IllegalArgumentException e) {
				s1 = new DateTime();
				e1 = new DateTime();
			}
		    String switcher = reportName+reportFormat;
		    
		switch (switcher.toLowerCase()) {
		//Failure report
		case "failurereportpdf":
			try {
				return failureReport(1L,s1.toDate(),e1.toDate(), "PDF");
			} catch (SQLException e2) {
				Logger.error("Failure report error-pdf",e2.fillInStackTrace());
			}
		case "failurereportexcel":
			try {
				return failureReport(1L,s1.toDate(),e1.toDate(), "Excel");
			} catch (SQLException e2) {
				Logger.error("Failure report error-pdf",e2.fillInStackTrace());
			}
		case "failurereporthtml":
			try {
				return failureReport(1L,s1.toDate(),e1.toDate(), "Html");
			} catch (SQLException e2) {
				Logger.error("Failure report error-pdf",e2.fillInStackTrace());
			}
			
			
		case "nglequipmentpdf":
			return equipmentReport(1L, "PDF");
		case "nglequipmentexcel":
			return equipmentReport(1L, "Excel");
		case "nglequipmenthtml":
			return equipmentReport(1L, "Html");
		
			//Man-hour per discipline per unit report selector
		case "manhourdisciplineunitpdf":
			try {
				return manHourPerDisciplinePerSection(1L,s1.toDate(),e1.toDate(), disciplineId, "PDF");
			} catch (SQLException e) {
				Logger.error("Man hour per discipline per unit error-pdf",e.fillInStackTrace());
			}
			break;
		case "manhourdisciplineunitexcel":
			try {
				return manHourPerDisciplinePerSection(1L,s1.toDate(),e1.toDate(), disciplineId, "Excel");
			} catch (SQLException e) {
				Logger.error("Man hour per discipline per unit error-excel",e.fillInStackTrace());
			}
			break;
		
		//Section work order count
		case "sectionworkordercountpdf":
			try {
				return sectionWorkOrderCount(1L,s1.toDate(),e1.toDate(),"PDF");
			} catch (SQLException e) {
				Logger.error("Man hour per discipline per unit error-pdf",e.fillInStackTrace());
			}
			break;
		case "sectionworkordercountexcel":
			try {
				return sectionWorkOrderCount(1L,s1.toDate(),e1.toDate(),"Excel");
			} catch (SQLException e) {
				Logger.error("Man hour per discipline per unit error-excel",e.fillInStackTrace());
			}
			break;
		case "sectionworkordercounthtml":
			try {
				return sectionWorkOrderCount(1L,s1.toDate(),e1.toDate(),"Html");
			} catch (SQLException e) {
				Logger.error("Man hour per discipline per unit error-excel",e.fillInStackTrace());
			}
			break;
		
		case "workorderstatusreportpdf":
			return workorderStatus(1L,s1.toDate(),e1.toDate(),"PDF");
		case "workorderstatusreportexcel":
			return workorderStatus(1L,s1.toDate(),e1.toDate(),"Excel");
		case "workorderstatusreporthtml":
			return workorderStatus(1L,s1.toDate(),e1.toDate(),"Html");
		
		case "criticalequipmentworkreportpdf":
			return criticalEquipmentWorkOrders(1L,s1.toDate(),e1.toDate(),"PDF");
		case "criticalequipmentworkreportexcel":
			return criticalEquipmentWorkOrders(1L,s1.toDate(),e1.toDate(),"Excel");
		case "criticalequipmentworkreporthtml":
			return criticalEquipmentWorkOrders(1L,s1.toDate(),e1.toDate(),"Html");
			
		case "maintenancebacklogreportpdf":
			return backLogReport(1L,s1.toDate(),e1.toDate(),disciplineId,"PDF");
		case "maintenancebacklogreportexcel":
			return backLogReport(1L,s1.toDate(),e1.toDate(),disciplineId,"Excel");
		case "maintenancebacklogreporthtml":
			return backLogReport(1L,s1.toDate(),e1.toDate(),disciplineId,"Html");
			
		case "consumedmaterialreportpdf":
			return maintMaterial(1L,s1.toDate(),e1.toDate(),true,"PDF");
		case "consumedmaterialreportexcel":
			return maintMaterial(1L,s1.toDate(),e1.toDate(),true,"Excel");
		case "consumedmaterialreporthtml":
			return maintMaterial(1L,s1.toDate(),e1.toDate(),true,"Html");
		
		case "requestedmaterialreportpdf":
			return maintMaterial(1L,s1.toDate(),e1.toDate(),false,"PDF");
		case "requestedmaterialreportexcel":
			return maintMaterial(1L,s1.toDate(),e1.toDate(),false,"Excel");
		case "requestedmaterialreporthtml":
			return maintMaterial(1L,s1.toDate(),e1.toDate(),false,"Html");
			
		case "statussummaryhtml":
			return woSummary(1L,"corrective",s1.toDate(),e1.toDate());
		case "pmstatussummaryhtml":
			return woSummary(1L,"preventive",s1.toDate(),e1.toDate());
			
		default:
			break;
		}
		flash("error","Requested report can't be generated. Please change parameters and try again");
		return redirect(routes.ReportApplication.reportSelector());
	}
	
	/**
	 * 
	 * @param reportDefFile
	 * @param reportParams
	 * @param jrds
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Result downloadHtml(String reportDefFile,
			Map reportParams, JRDataSource jrds) {

		String compiledFile = REPORT_DEFINITION_PATH + reportDefFile
				+ ".jasper";

		try {
			JasperCompileManager.compileReportToFile(REPORT_DEFINITION_PATH
					+ reportDefFile + ".jrxml", compiledFile);
			String pfi = JasperFillManager.fillReportToFile(compiledFile,
					reportParams, jrds);
			String outputFile = JasperExportManager.exportReportToHtmlFile(pfi);
			
			return ok(new java.io.File(outputFile));

		} catch (Exception e) {
			Logger.error("Html report Error:",e.fillInStackTrace());
			return ok("Error! Report was not generated.");
		}

	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Result downloadPDF(String reportDefFile,
			Map reportParams, JRDataSource jrds) {

		String compiledFile = REPORT_DEFINITION_PATH + reportDefFile
				+ ".jasper";

		try {
			JasperCompileManager.compileReportToFile(REPORT_DEFINITION_PATH
					+ reportDefFile + ".jrxml", compiledFile);
			String pfi = JasperFillManager.fillReportToFile(compiledFile,
					reportParams, jrds);
			String outputFile = JasperExportManager.exportReportToPdfFile(pfi);
			
			return ok(new java.io.File(outputFile));
			
		} catch (Exception e) {
			Logger.error("PDF report Error:",e.fillInStackTrace());
			return ok("Error! Report was not generated.");
		}

	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Result downloadExcel(String reportDefFile,
			Map reportParams, JRDataSource jrds) {

		String compiledFile = REPORT_DEFINITION_PATH + reportDefFile
				+ ".jasper";

		try {
			//OutputStream os = new ByteArrayOutputStream();
			File outputFile = new File(REPORT_DEFINITION_PATH+reportDefFile+".xls");
			JasperCompileManager.compileReportToFile(REPORT_DEFINITION_PATH
					+ reportDefFile + ".jrxml", compiledFile);
			JasperPrint jrPrint = JasperFillManager.fillReport(compiledFile,reportParams,jrds);
			
			Exporter exporter = new JRXlsExporter();
			exporter.setExporterInput(new SimpleExporterInput(jrPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputFile));
			SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
			configuration.setOnePagePerSheet(false);
			configuration.setDetectCellType(true);
			configuration.setCollapseRowSpan(false);
			exporter.setConfiguration(configuration);
			exporter.exportReport();

			return ok(outputFile);
		} catch (Exception e) {
			Logger.error("Excel report Error:",e.fillInStackTrace());
			return ok("Error! Report was not generated.");
		}
		
	}
	
	/**
	 * Uses ReportApplication to generate PM form
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Result downloadPdfPmForm(Long id,String mimeType) {
		Map reportParams = new HashMap();
		PreventiveMaintenances pm = PreventiveMaintenances.find.byId(id);
		
		if(pm.parentEquipment!=null) {
			reportParams.put("equipmentCode", pm.parentEquipment.name);
			reportParams.put("equipmentName", pm.parentEquipment.description);
			reportParams.put("location", pm.parentEquipment.section.name);
		} else if(pm.parentSection!=null) {
			reportParams.put("location", pm.parentSection.name);
		}
		
		reportParams.put("discipline", pm.actingDiscipline.name);
		if(pm.onShutDown==true) {
			reportParams.put("interval", "SHUTDOWN LIST");
			} else if(pm.intervalOperationHours!=null && pm.intervalOperationHours!=0) {
				reportParams.put("interval", pm.intervalOperationHours.toString()+" operation hours");
				} else if(pm.intervalDays!=null && pm.intervalDays!=0) {
					reportParams.put("interval", pm.intervalDays.toString()+" days");
				}
		reportParams.put("today", myUtils.DateTimeUtils.getIranianDate(new Date()));
		reportParams.put("routineName", pm.name);
		ArrayList<CustomBean> bl = pm.pmItems();
		JRBeanCollectionDataSource beanColDS =
				new JRBeanCollectionDataSource(bl);
		
		//Generate report and serve
		if(mimeType.toLowerCase()=="excel")
			return downloadExcel("PMForm", reportParams, beanColDS);
		else return downloadPDF("PMForm", reportParams, beanColDS);
	}
	
	/**
	 * A model for later reports from bean classes!
	 * @return
	 */
	@SuppressWarnings({"rawtypes", "unchecked" })
	public static Result equipmentReport(Long plantId, String type) {
		Map reportParams = new HashMap();
		List<Equipments> aequip = Equipments.find.where()
				.eq("section.plant.id", plantId)
				.orderBy("equipmentClass.name")
				.findList();
		reportParams.put("jalaliDate", DateTimeUtils.getIranianDate(new Date()));
		reportParams.put("plantName",Plants.find.byId(plantId).name);
		
		JRBeanCollectionDataSource beanColDs = new JRBeanCollectionDataSource(aequip);
		switch(type) {
		case "PDF" :
			return downloadPDF("EquipmentReport", reportParams, beanColDs);
		case "Excel" :
			return downloadExcel("EquipmentReport", reportParams, beanColDs);
		case "Html" :
			return downloadHtml("EquipmentReport", reportParams, beanColDs);
		}
		return ok("An error happened!");
	}
	
	/**
	 * A model for later reports from bean classes!
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings({"rawtypes", "unchecked" })
	public static Result manHourPerDisciplinePerSection(Long plantId, Date start, Date end, Long disciplineId, String type) throws SQLException {
		Float disSectionSumManHour =0f;
		Float totalTTR = 0f;
		int counter=0;
		int sesize=0;
		List<Sections> loopSections = new ArrayList<Sections>();
		for (Sections se : Sections.find.all()) {
			if(Maintenances.find.where()
					.eq("maintainedSection.id", se.id)
					.eq("responsibleDiscipline.id",disciplineId)
					.between("requestDate", start, end)
					.findRowCount() > 0) {
				sesize++;
				loopSections.add(se);
			}
		}
		
		Map reportParams = new HashMap();
		HashMap[] reportMap = new HashMap[sesize];
		
		for (Sections s : loopSections) {
			disSectionSumManHour=0f;
			totalTTR =0f;
			HashMap rowMap = new HashMap();
			rowMap.put("sectionName",s.name);
			List<Maintenances> msList = Maintenances.find.where()
					.eq("maintainedSection.id", s.id)
					.eq("responsibleDiscipline.id",disciplineId)
					.between("requestDate", start, end)
					.findList();
				for(Maintenances maint : msList) {
				if(maint.calculatedTotalManHours!=null) disSectionSumManHour +=maint.calculatedTotalManHours;
				if(maint.calculatedTotalTimeToRepair!=null) totalTTR += maint.calculatedTotalTimeToRepair;
				}
				rowMap.put("manHour",disSectionSumManHour);
				rowMap.put("ttr",totalTTR);
			reportMap[counter] = rowMap;
			counter++;
		}
			
		JRMapArrayDataSource dataSource;
		dataSource = new JRMapArrayDataSource(reportMap);
		reportParams.put("diciplineName",Disciplines.find.byId(disciplineId).name);
				
			switch(type) {
			case "PDF" :
				return downloadPDF("ManHourPerDisciplineReport", reportParams, dataSource);
			case "Excel" :
				return downloadExcel("ManHourPerDisciplineReport", reportParams, dataSource);
			}
			
			
		
		return ok("An error happened!");
	}
	
	/**
	 * A model for later reports from bean classes!
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings({"rawtypes" })
	public static Result failureReport(Long plantId, Date start, Date end, String type) throws SQLException {
		ResultSet rs = null;
		try (
				Connection connection = play.db.DB.getConnection();
				PreparedStatement stmt = connection.prepareStatement(
						"select  failures.id,severity,function_impact, operation_impact,safety_impact,detection_method,failures.comments, " + 
						"failure_modes.failure_mode_code fmc,failure_mechanisms.notation," +
						"failure_mechanisms.subdivision_notation,history.start,history.end, " +
						"equipments.name ename, equipment_class.name eqcname, equipment_class.ec_type eqctype FROM failures " +
						"inner join failure_modes on failure_mode_id = failure_modes.id " +
						"inner join failure_mechanisms on failure_mechanism_id = failure_mechanisms.id " +
						"inner join history on history.fail_id = failures.id " +
						"left outer join equipments on history.parent_equipment_id = equipments.id " +
						"inner join equipment_class on equipments.equipment_class_id = equipment_class.id " +
						"WHERE (history.start BETWEEN ? AND ?) " +
						"ORDER BY fmc,eqcname");
				) {
			stmt.setDate(1, new java.sql.Date(start.getTime()));
			stmt.setDate(2,new java.sql.Date(end.getTime()));
			rs = stmt.executeQuery();
			Map reportParams = new HashMap();
			JRResultSetDataSource beanColDs = new JRResultSetDataSource(rs);
			
			switch(type) {
			case "Html" :
				return downloadHtml("FailureReport", reportParams, beanColDs);
			case "PDF" :
				return downloadPDF("FailureReport", reportParams, beanColDs);
			case "Excel" :
				return downloadExcel("FailureReport", reportParams, beanColDs);
			}
			
			
		} catch (SQLException e) {
			Logger.error("Failure report error",e.fillInStackTrace());
		} finally {
			if(rs != null)
				rs.close();
			
		}
		return ok("An error happened!");
	}
	
	
	/**
	 * Generate report of number of W.Os per section
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings({"rawtypes" })
	public static Result sectionWorkOrderCount(Long plantId, Date start, Date end, String type) throws SQLException {
		ResultSet rs = null;
		try (
				Connection connection = play.db.DB.getConnection();
				PreparedStatement stmt = connection.prepareStatement(
						"select count(*) as summh,sections.name as sectionName, sections.section_category categ from maintenances left outer join sections " +
						"on maintenances.maintained_section_id = sections.id " +
						"WHERE (request_date BETWEEN ? AND ?) " +
						"group by sections.name");
				) {
			
			stmt.setDate(1, new java.sql.Date(start.getTime()));
			stmt.setDate(2,new java.sql.Date(end.getTime()));
			rs = stmt.executeQuery();
			Map reportParams = new HashMap();
			JRResultSetDataSource beanColDs = new JRResultSetDataSource(rs);
			
			switch(type) {
			case "Html" :
				return downloadHtml("SectionWOReport", reportParams, beanColDs);
			case "PDF" :
				return downloadPDF("SectionWOReport", reportParams, beanColDs);
			case "Excel" :
				return downloadExcel("SectionWOReport", reportParams, beanColDs);
			}
			
			
		} catch (SQLException e) {
			Logger.error("Man hour per discipline per unit error",e.fillInStackTrace());
		} finally {
			if(rs != null)
				rs.close();
			
		}
		return ok("An error happened!");
	}
	
	/**
	 * 
	 * @param plantId
	 * @param start
	 * @param end
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Result workorderStatus(Long plantId, Date start, Date end, String type) {
		Map reportParams = new HashMap();
		List<Maintenances> maintList = Maintenances.find.where()
		.eq("maintenanceCategory", MaintenanceCategories.CORRECTIVE_MAINTENANCE)
		.or(Expr.eq("maintenanceStatus", MaintenanceStatus.HOLD),
				Expr.eq("maintenanceStatus",MaintenanceStatus.IN_PROGRESS))
		.between("requestDate",start,end)
		.select("workOrderSerial,requestDate,maintenancePriority, workOrderDescription,"
				+ "maintenanceStatus,holdReason,holdReasonComment")
		.orderBy("responsibleDiscipline.name")
		.findList();
		for(Maintenances m : maintList) {
			m.requestDate = myUtils.DateTimeUtils.getIranianDateAsDate(m.requestDate);
			if(m.workOrderDescription!=null)
			m.workOrderDescription=m.workOrderDescription.replace("ی","ي");
			if(m.workflowStage != null)
				m.workflowStage.description = m.workflowStage.description.replace("ی","ي");
		}
		JRBeanCollectionDataSource beanColDs = new JRBeanCollectionDataSource(maintList);
		
		switch(type) {
		case "PDF" :
			return downloadPDF("StatusReport", reportParams, beanColDs);
		case "Excel" :
			return downloadExcel("StatusReport", reportParams, beanColDs);
		case "Html" :
			return downloadHtml("StatusReport", reportParams, beanColDs);
		}
		
		return ok("An error happened!");
	}
	
	/**
	 * 
	 * @param plantId
	 * @param start
	 * @param end
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Result criticalEquipmentWorkOrders(Long plantId,Date start, Date end, String type) {
		Map reportParams = new HashMap();
		List<Maintenances> maintList = Maintenances.find.where()
		.or(Expr.eq("maintainedEquipment.criticality", Criticality.MOST_CRITICAL),
				Expr.eq("maintainedEquipment.criticality", Criticality.CRITICAL))
				.between("requestDate",start,end)
				.select("workOrderSerial,requestDate,maintenancePriority, workOrderDescription,"
				+ "maintenanceStatus,holdReason,holdReasonComment")
				.orderBy("responsibleDiscipline.name")
				.findList();
		for(Maintenances m : maintList) {
			m.requestDate = myUtils.DateTimeUtils.getIranianDateAsDate(m.requestDate);
			if(m.workOrderDescription!=null)
			m.workOrderDescription=m.workOrderDescription.replace("ی","ي");
		}
		JRBeanCollectionDataSource beanColDs = new JRBeanCollectionDataSource(maintList);
		
		switch(type) {
		case "PDF" :
			return downloadPDF("CriticalEquipmentReport", reportParams, beanColDs);
		case "Excel" :
			return downloadExcel("CriticalEquipmentReport", reportParams, beanColDs);
		case "Html" :
			return downloadHtml("CriticalEquipmentReport", reportParams, beanColDs);
		}
		
		return ok("An error happened!");
	}
	
	/**
	 * 
	 * @param plantId
	 * @param start
	 * @param end
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Result backLogReport(Long plantId, Date start, Date end, Long disciplineId, String type) {
		Map reportParams = new HashMap();
		reportParams.put("disciplineName", Disciplines.find.byId(disciplineId).name);
		
		Integer counter=0;
		Map<Integer,List<History>> hmap =new HashMap<Integer,List<History>>();
		for(Maintenances m : Maintenances.find.where()
				.eq("maintenanceStatus",MaintenanceStatus.FINISHED)
				.eq("responsibleDiscipline.id",disciplineId)
				.between("requestDate", start, end)
				.findList()) {
			List<History> lhpart = History.find.where().eq("maint.id",m.id).orderBy("isHappened asc").findList();
			if(lhpart.size()==2) {hmap.put(counter,lhpart); counter++;}
		}
		HashMap[] reportMap = new HashMap[hmap.size()];
		
		counter=0;
		for(Integer r : hmap.keySet()) {
			HashMap rowMap = new HashMap();
				List<History> mhlist = hmap.get(r);
				rowMap.put("workOderSerial",mhlist.get(0));
				History hplanned = mhlist.get(0);
				History hactual = mhlist.get(1);
				rowMap.put("workOrderSerial", mhlist.get(0).maint.workOrderSerial);
				rowMap.put("startDiff", (float) (hactual.start.getTime() - hplanned.start.getTime()) / 86400000);
				rowMap.put("lengthDiff", (float) ( (hactual.end.getTime()-hactual.start.getTime())/3600000 - (hplanned.end.getTime()-hplanned.start.getTime()) / 3600000 ));
				reportMap[counter] = rowMap;
				counter++;
			
		}
		JRMapArrayDataSource dataSource;
		dataSource = new JRMapArrayDataSource(reportMap);
		switch(type) {
		case "PDF" :
			return downloadPDF("backLogReport", reportParams, dataSource);
		case "Excel" :
			return downloadExcel("backLogReport", reportParams, dataSource);
		case "Html" :
			return downloadHtml("backLogReport", reportParams, dataSource);
		}
		
		return ok("An error happened!");
	}
	
	/**
	 * 
	 * @param id
	 * @param mimeType
	 * @return
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Result workOrderForm(Long id) {
		Map reportParams = new HashMap();
		Maintenances mwo = Maintenances.find.byId(id);
		
		if(mwo.maintainedEquipment!=null) {
			reportParams.put("equipmentCode", mwo.maintainedEquipment.name);
			reportParams.put("location", mwo.maintainedEquipment.section.name);
		} else if(mwo.maintainedSection!=null) {
			reportParams.put("location", mwo.maintainedSection.name);
		}
		reportParams.put("woDesc",mwo.workOrderDescription.replace("ی","ي"));
		reportParams.put("discipline", mwo.responsibleDiscipline.name.replace("ی","ي"));
		reportParams.put("reqDate", myUtils.DateTimeUtils.getIranianDate(mwo.requestDate));
		reportParams.put("woNumber",mwo.workOrderSerial);
		reportParams.put("priority",mwo.maintenancePriority);
		if(mwo.maintenanceReport!=null)
			reportParams.put("workDescription",mwo.maintenanceReport.replace("ی","ي"));
		reportParams.put("holdReason",mwo.holdReason);
		reportParams.put("holdReasonComment",mwo.holdReasonComment.replace("ی","ي"));
		int s = mwo.personnelInvolved.size();
		for(int i=0;i< (s<=7 ? s : 7); i ++) {
			reportParams.put("user"+i,mwo.personnelInvolved.get(i).user.lastName);
			reportParams.put("mh"+i,mwo.personnelInvolved.get(i).hours);
		}
		reportParams.put("totalMH",mwo.calculatedTotalManHours);
		
		History mah = History.find.where().eq("maint.id",id).eq("isHappened",true).findUnique();
		if(mah!=null) {
		reportParams.put("startDate", myUtils.DateTimeUtils.getIranianDateTime(mah.start));
		reportParams.put("endDate", myUtils.DateTimeUtils.getIranianDateTime(mah.end));
		}
		JREmptyDataSource beanColDS = new JREmptyDataSource();
		
		//Generate report and serve
		return downloadPDF("wo", reportParams, beanColDS);
	}
	
	/**
	 * 
	 * @param plantId
	 * @param start
	 * @param end
	 * @return
	 */
	public static Result woSummary(Long plantId, String cat, Date start, Date end) {
		
		MaintenanceCategories var;
		switch(cat) {
		case "corrective":
			var = MaintenanceCategories.CORRECTIVE_MAINTENANCE;
			break;
		case "preventive":
			var = MaintenanceCategories.PREVENTIVE_MAINTENANCE;
			break;
		case "predictive":
			var = MaintenanceCategories.PREDICTIVE_MAINTENANCE;
			break;
		default:
			var = MaintenanceCategories.CORRECTIVE_MAINTENANCE;
		}
		
		List<ArrayList<String>> tableList = new ArrayList<ArrayList<String>>();
		ArrayList<String> dnameList = new ArrayList<String>();
		ArrayList<String> dnameList2 = new ArrayList<String>();
		dnameList.add("Status\\Discipline");
		List<MaintenanceStatus> mstaList = new ArrayList<MaintenanceStatus>();
		List<HoldReasons> holdList = new ArrayList<HoldReasons>();
		
		//Create a list of maint statuses
		for (MaintenanceStatus ms : MaintenanceStatus.values())
			mstaList.add(ms);
		
		//Create a list of hold reasons
		for(HoldReasons hr : HoldReasons.values())
			holdList.add(hr);
		
		//Create a list of discipline names
		for(Disciplines d : Disciplines.find.all()) {
			dnameList.add(d.name);
			dnameList2.add(d.name);
		}
		dnameList.add("Total");
		tableList.add(dnameList);
		int[] columnSums = new int[dnameList2.size()+1];
		for(int k=0; k<columnSums.length;k++) columnSums[k]=0;
		
		for(MaintenanceStatus msta : mstaList) {
			int statusSum = 0;
			int columnLooper = 0;
			if(msta.equals(MaintenanceStatus.HOLD)) {
				for(HoldReasons hr : holdList) {
					columnLooper = 0;
					ArrayList<String> trow2 = new ArrayList<String>();
					trow2.add("HOLD "+hr.toString());
					for (String dna : dnameList2) {
						int stCount = Maintenances.find.where().eq("responsibleDiscipline.name", dna)
								.eq("maintenanceCategory",var)
								.between("requestDate",start,end)
								.eq("maintenanceStatus",msta)
								.eq("holdReason",hr)
								.findRowCount();
						trow2.add(String.valueOf(stCount));
						columnSums[columnLooper] += stCount;
						columnLooper++;
						statusSum += stCount;
					}
					trow2.add(String.valueOf(statusSum));
					columnSums[columnLooper]=statusSum;
					statusSum=0;
					tableList.add(trow2);
				}
			} else {
				ArrayList<String> trow = new ArrayList<String>();
				trow.add(msta.toString());
				for (String dna : dnameList2) {
					int stCount =  Maintenances.find.where().eq("responsibleDiscipline.name", dna)
							.eq("maintenanceCategory",var)
							.between("requestDate",start,end)
							.eq("maintenanceStatus",msta)
							.findRowCount();
					trow.add(String.valueOf(stCount));
					columnSums[columnLooper] += stCount;
					columnLooper++;
					statusSum += stCount;
				}
				trow.add(String.valueOf(statusSum));
				columnSums[columnLooper]=statusSum;
				statusSum=0;
				tableList.add(trow);
			}
		}
		ArrayList<String> finalRow = new ArrayList<String>();
		finalRow.add("Sums");
		int sumTotals=0;
		for(int k=0;k<columnSums.length-1;k++) {
			sumTotals+=columnSums[k];
			finalRow.add(String.valueOf(columnSums[k]));
		}
		finalRow.add(String.valueOf(sumTotals));
		tableList.add(finalRow);
		return ok(views.html.reports.woSummary.render(tableList));
	}
	
	/**
	 * 
	 * @param plantId
	 * @param Start
	 * @param end
	 * @param type
	 * @return
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Result maintMaterial(Long plantId, Date start, Date end, Boolean flag,String type) {
		Map reportParams = new HashMap();
		List<MaintenancesParts> mpList = MaintenancesParts.find.where()
				.between("maintenance.requestDate", start, end)
				.eq("stockFlag", flag)
				.orderBy("part.name")
				.findList();
		
		JRBeanCollectionDataSource beanColDs = new JRBeanCollectionDataSource(mpList);
		reportParams.put("plantName",Plants.find.byId(plantId).name);
		reportParams.put("jalaliDate", DateTimeUtils.getIranianDate(new Date()));
		switch(type) {
		case "PDF" :
			return downloadPDF("MaterialReport", reportParams, beanColDs);
		case "Excel" :
			return downloadExcel("MaterialReport", reportParams, beanColDs);
		case "Html" :
			return downloadHtml("MaterialReport", reportParams, beanColDs);
		}
		return ok("An error happened!");
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Result meterWorkOrder(Long disciplineId) {
		Map reportParams = new HashMap();
		if(Maintenances.find.where().eq("responsibleDiscipline.id",disciplineId).findRowCount()==0) return ok("");
		reportParams.put("finishedMaints",Maintenances.find.where().eq("responsibleDiscipline.id",disciplineId).eq("maintenanceStatus",MaintenanceStatus.FINISHED).findRowCount());
		reportParams.put("allMaints",Maintenances.find.where().eq("responsibleDiscipline.id",disciplineId).findRowCount());
		
		String compiledFile = REPORT_DEFINITION_PATH + "disciplineMeter"
				+ ".jasper";
		
		BufferedImage image = new BufferedImage(320, 200, BufferedImage.TYPE_3BYTE_BGR);
		try (OutputStream os = new ByteArrayOutputStream()) {
			
			JasperCompileManager.compileReportToFile(REPORT_DEFINITION_PATH
					+ "disciplineMeter" + ".jrxml", compiledFile);
			JasperPrint jrPrint = JasperFillManager.fillReport(compiledFile,reportParams,new JREmptyDataSource());
			JRGraphics2DExporter exporter = new JRGraphics2DExporter();
			exporter.setExporterInput(new SimpleExporterInput(jrPrint));
			SimpleGraphics2DExporterOutput output = new SimpleGraphics2DExporterOutput();
			output.setGraphics2D((Graphics2D)image.getGraphics());
			exporter.setExporterOutput(output);
			exporter.exportReport();
			ImageIO.write(image, "jpg", os);
		    return ok(new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray())).as("image/jpeg");
		    
		} catch (JRException | IOException e1) {
 
			e1.printStackTrace();
 
		}		
		return ok("no report");
	}
	
	/**
	 * 
	 * @param gid
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Result projectGantt(Long gid) {
		Map reportParams = new HashMap();
		MaintenanceGroups g = MaintenanceGroups.find.byId(gid);
		int size = 0;
		for(Maintenances ma : g.memberMaints) {
			size += History.find.where().eq("maint.id",ma.id).findRowCount();
			for(Maintenances depma : ma.dependentMaintenances)
			  size += History.find.where().eq("maint.id",depma.id).findRowCount();
		}
		int counter=0;
		HashMap[] reportMap = new HashMap[size];
		
		for(Maintenances m : g.memberMaints) {
			if(History.find.where().eq("maint.id", m.id).eq("isHappened", true).findRowCount()==1) {
				HashMap rowMap = new HashMap();
				rowMap.put("Task", "TASK:"+m.workOrderSerial);
				rowMap.put("Subtask", m.workOrderSerial);
				rowMap.put("StartTimestamp",History.find.where().eq("maint.id", m.id).eq("isHappened", true).findUnique().start);
				rowMap.put("EndTimestamp",History.find.where().eq("maint.id", m.id).eq("isHappened", true).findUnique().end);
				rowMap.put("Series","Actual");
				reportMap[counter] = rowMap;
				counter++;
			}
			if(History.find.where().eq("maint.id", m.id).eq("isHappened", false).findRowCount()==1) {
				HashMap rowMap = new HashMap();
				rowMap.put("Task", "TASK:"+m.workOrderSerial);
				rowMap.put("Subtask", m.workOrderSerial);
				rowMap.put("StartTimestamp",History.find.where().eq("maint.id", m.id).eq("isHappened", false).findUnique().start);
				rowMap.put("EndTimestamp",History.find.where().eq("maint.id", m.id).eq("isHappened", false).findUnique().end);
				rowMap.put("Series","Scheduled");
				reportMap[counter] = rowMap;
				counter++;
			}
			if(m.dependentMaintenances.size()!=0) {
				for(Maintenances subm : m.dependentMaintenances) {
					if(History.find.where().eq("maint.id", subm.id).eq("isHappened", true).findRowCount()==1) {
						HashMap rowMap = new HashMap();
						rowMap.put("Task","TASK:"+m.workOrderSerial);
						rowMap.put("Subtask", subm.workOrderSerial);
						rowMap.put("StartTimestamp",History.find.where().eq("maint.id", subm.id).eq("isHappened", true).findUnique().start);
						rowMap.put("EndTimestamp",History.find.where().eq("maint.id", subm.id).eq("isHappened", true).findUnique().end);
						rowMap.put("Series","Actual");
						reportMap[counter] = rowMap;
						counter++;
					}
					if(History.find.where().eq("maint.id", subm.id).eq("isHappened", false).findRowCount()==1) {
						HashMap rowMap = new HashMap();
						rowMap.put("Task", "TASK:"+m.workOrderSerial);
						rowMap.put("Subtask", subm.workOrderSerial);
						rowMap.put("StartTimestamp",History.find.where().eq("maint.id", subm.id).eq("isHappened", false).findUnique().start);
						rowMap.put("EndTimestamp",History.find.where().eq("maint.id", subm.id).eq("isHappened", false).findUnique().end);
						rowMap.put("Series","Scheduled");
						reportMap[counter] = rowMap;
						counter++;
					}
				}
			}
		}
		
		//Actually generate gantt image
		JRMapArrayDataSource dataSource;
		dataSource = new JRMapArrayDataSource(reportMap);
		return downloadPDF("GanttChartReport",reportParams,dataSource);
	}
}
