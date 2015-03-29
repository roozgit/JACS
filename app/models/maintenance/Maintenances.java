package models.maintenance;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import models.Blobs;
import models.Disciplines;
import models.History;
import models.RepairTools;
import models.Sections;
import models.Users;
import models.equipment.Equipments;
import models.failure.Failures;
import myUtils.DateTimeUtils;
import play.Logger;
import play.data.format.Formats;
import play.data.validation.Constraints.Max;
import play.data.validation.Constraints.Min;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;
import com.avaje.ebean.Query;

@Entity
@Table(name="maintenances")
public class Maintenances extends Model {
	
	private static final long serialVersionUID = 3337373059056380744L;

	//************************************************************
	//Common fields
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;
	
	@Column(unique=true)
	@Formats.NonEmpty
	public String workRequestSerial;
	
	@Column(unique=true)
	@Formats.NonEmpty
	public String workOrderSerial;
	
	@Formats.DateTime(pattern="yyyy/MM/dd")
	public Date requestDate;
	
	//***************************************************************
	//Maintenance request fields
	@ManyToOne
	public Equipments maintainedEquipment;
	
	@ManyToOne
	public Sections maintainedSection;
	
	@ManyToOne
	public PreventiveMaintenances pmRoutine;
	
	@ManyToOne
	public PreventiveMaintenances classLevelPMRoutine;
	
	@ManyToMany
	@JoinTable(name = "maintenances_maintenances", joinColumns = @JoinColumn(name = "prereq_maintenances_id"), inverseJoinColumns = @JoinColumn(name = "dependent_maintenances_id"))
	public List<Maintenances> dependentMaintenances = new ArrayList<Maintenances>();
	
	@ManyToOne
	public MaintenanceGroups maintenanceGroup;
	
	@Min(0)
	@Max(7)
	public Integer maintenancePriority;
	
	@Column(columnDefinition = "TEXT")
	public String workOrderDescription;
	
	//***************************************************************
	//Maintenance planning and work verification fields
	
	@OneToOne
	public Disciplines responsibleDiscipline;
	
	@OneToOne
	public WorkflowTree workflowStage;
	
	@Enumerated(EnumType.STRING)
	public MaintenanceStatus maintenanceStatus;
	
	@Enumerated(EnumType.STRING)
	public HoldReasons holdReason;
	
	public String holdReasonComment;
	
	@Enumerated(EnumType.STRING)
	public MaintenanceCategories maintenanceCategory;
	
	@Column(columnDefinition = "TEXT")
	public String planningComment;
	
	//***************************************************************
	//work executer and final verification fields
	
	@OneToOne
	public Users responsiblePerson;
	
	@ManyToMany
	public List<Failures> correspondingFailure = new ArrayList<Failures>();

	@Enumerated(EnumType.STRING)
	public MaintenanceActivities maintenanceActivity;
	
	@ManyToMany
	public List<Equipments> equipmentResources = new ArrayList<Equipments>();
	
	@ManyToMany
	public List<RepairTools> tools = new ArrayList<RepairTools>();
	
	@OneToMany(mappedBy="maintenance")
	public List<MaintenancesSubunits> subunitsMaintained = new ArrayList<MaintenancesSubunits>();

	@OneToMany(mappedBy="maintenance")
	public List<MaintenancesComponents> componentsMaintained = new ArrayList<MaintenancesComponents>();
	
	@OneToMany(mappedBy="maintenance")
	public List<MaintenancesParts> partsUsed = new ArrayList<MaintenancesParts>();
	
	@OneToMany(mappedBy="maintenance")
	public List<MaintenancesUsers> personnelInvolved = new ArrayList<MaintenancesUsers>();
	
	public Float calculatedTotalManHours=0F;
	
	public Float timeToRepair=0F;
	
	public Float calculatedTotalTimeToRepair=0F;
	
	@Column(columnDefinition = "TEXT")
	public String maintenanceReport;
	
	//***************************************************************
	//Miscellaneous fields
	@ManyToMany
	public List<Blobs> files = new ArrayList<Blobs>();
	
	//Query creation helper
	public static Model.Finder<Long,Maintenances> find =
			new Model.Finder<Long,Maintenances>(Long.class,Maintenances.class);
	
	
	public String validate() {
		if (responsibleDiscipline==null) return "Maintenance must be assigned to a discipline!";
		else return null;
	}
	
	/**
	 * 
	 * @return
	 */
	public static Map<String, String> options() {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Maintenances m : Maintenances.find.where()
				.isNotNull("id")
				.orderBy("workOrderSerial")
				.findList()) {
			options.put(m.id.toString(), m.workOrderSerial);
		}
		return options;
	}
	
	/**
	 * Calculates total man hour spent on maintenance
	 * @param maintenanceId
	 * @return
	 */
	public static Float calculateManHour(Long maintenanceId) {
		
		float sum = 0;
		for(MaintenancesUsers mus : Maintenances.find.byId(maintenanceId).personnelInvolved) {
			Float m1 = Float.valueOf(mus.hours);
			sum += m1;
		}
		return sum;
	}
	
	/**
	 * Calculates total repair time spent in maintenance
	 * @param maintenanceId
	 * @return
	 */
	public static Float calculateTTR(Long maintenanceId) {
		Maintenances mm = Maintenances.find.byId(maintenanceId);
		Float sum = mm.timeToRepair;
		for(MaintenancesSubunits msu : mm.subunitsMaintained) {
			sum += msu.timeToRepair;
		}
		for(MaintenancesComponents mco : mm.componentsMaintained) {
			sum += mco.timeToRepair;
		}
		return sum;
	}
	
	/**
	 * Returns a page of maintenances for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort the maintenances by, typically name
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter, for maintenances
	 *            search
	 * @return Page<Maintenances>: A page of Maintenances
	 */
	public static Page<Maintenances> page(int page, int pageSize, String sortBy,
			String order, String filter1, String filter2, String filter3, String filter4,
			String filter5, String filter6, String filter7, String filter8,String filter9) {
		Date start = new Date();
		Date end = new Date();
		boolean cf=false;
		String adds2="", adds3="", adds9="";
		
		if(filter2.length()==0)
			adds2 = " OR maintainedSection.name is NULL";
		if(filter3.length()==0)
			adds3 = " OR maintainedEquipment.name is NULL";
		if(filter9.length()==0)
			adds9 = " OR holdReason is NULL";
		if(filter7.matches("(13|14)\\d\\d(/)(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])-"
				+ "(13|14)\\d\\d\\2(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])")) {
					String[] dates = filter7.split("-");
					start = DateTimeUtils.getGregorianDateTime(dates[0] + " 00:00");
					end = DateTimeUtils.getGregorianDateTime(dates[1] + " 00:00");
					cf = true;
		}
		Query<Maintenances> query = Ebean.createQuery(Maintenances.class);
		String whoql =
				"(lower(workRequestSerial) like :fil1 OR lower(workOrderSerial) like :fil1) AND " +
				"(lower(maintainedSection.name) like :fil2" + adds2 + ") AND " +
				"(lower(maintainedEquipment.name) like :fil3"+ adds3 + ") AND " +
				"lower(workOrderDescription) like :fil4 AND " +
				"lower(maintenanceCategory) like :fil5 AND " +
				"lower(responsibleDiscipline.name) like :fil6 AND "	+
				"lower(maintenanceStatus) like :fil8 AND " +
				"(lower(holdReason) like :fil9" + adds9 + ")";
		if(cf) {
			whoql =	whoql.concat(" AND (requestDate >= :startDate AND requestDate <= :endDate)");
		}
		
			query.where(whoql).orderBy(sortBy + " " + order)
			.setParameter("fil1","%"+filter1+"%")
			.setParameter("fil2", "%"+filter2+"%")
			.setParameter("fil3", "%"+filter3+"%")
			.setParameter("fil4", "%"+filter4+"%")
			.setParameter("fil5", "%"+filter5+"%")
			.setParameter("fil6", "%"+filter6+"%")
			.setParameter("fil8", "%"+filter8+"%")
			.setParameter("fil9", "%"+filter9+"%");
		
		if(cf) {
			query.setParameter("startDate", start)
			.setParameter("endDate", end);
		}
		return query
				.findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}
	
	/**
	 *  find maintenances in a specific workflow stage
	 * @param stageId
	 * @return
	 * NOT NEEDED XXXXXXXXXXXXXXXXXXXXXXXXXXXX NOT NEEDED
	 */
	public static List<Maintenances> findByWorkflowStage(Long stageId) {
		
		return Maintenances.find.where().
				eq("workflowStage.id", stageId).
				findList();
	}
	
	public static List<Maintenances> getDashboardMaintenances(Long userId) {
		Users u = Users.find.byId(userId);
		List<Maintenances> retList = new ArrayList<Maintenances>();
		for(Maintenances m : Maintenances.find.where().isNotNull("workflowStage")
				.orderBy("requestDate")
				.findList()) {
			if(u.roles.contains(m.workflowStage.receivingRole)) {
				if(m.workflowStage.receivingRole.getName().toLowerCase()
						.contains("incharge")) {
					if(m.responsiblePerson!=null)
						if(m.responsiblePerson.id == u.id)
							retList.add(m);
				} else retList.add(m);
			}
		}
		
		
		return retList;
	}
	
	public static Boolean delete(Long maintId) {
		Boolean result = true;
		Maintenances mtodel = Maintenances.find.byId(maintId);
		try {
			List<History> histsOfMaint = History.find.where()
					.eq("maint.id",maintId)
					.findList();
			if(histsOfMaint.size()!=0) {
				for(History h : histsOfMaint) h.delete();
			}
			if(mtodel.workflowStage!=null) {
				for(MaintenanceWorkflow mwfm : MaintenanceWorkflow.find.where().eq("maintenance.id",maintId)
					.findList()) {
					mwfm.delete();
				}
			}
			mtodel.delete();
		} catch (Exception e) {
			Logger.error("Maintenance deletion error",e.fillInStackTrace());
			result = false;			
		}
		return result;
	}
	
}