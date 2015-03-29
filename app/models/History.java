package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import models.equipment.Equipments;
import models.failure.Failures;
import models.maintenance.Maintenances;
import models.maintenance.PreventiveMaintenances;
import myUtils.DateTimeUtils;
import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Page;
import com.avaje.ebean.Query;

@Entity
@Table(name="history",uniqueConstraints= {
		   @UniqueConstraint(columnNames={"maint_id","is_happened"}),
		  @UniqueConstraint(columnNames={"fail_id"})
		   })
public class History extends Model {

	private static final long serialVersionUID = 8068160444139469068L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Long id;
	
	@ManyToOne
	public Installations parentInstallation;
	
	@ManyToOne
	public Plants parentPlant;
	
	@ManyToOne
	public Sections parentSection;
	
	@ManyToOne
	public Equipments parentEquipment;
	
	@ManyToOne
	public Subunits parentSubunit;
	
	@ManyToOne
	public Components parentComponent;
	
	@Required
	@Formats.DateTime(pattern="yyyy/MM/dd HH:mm")
	public Date start = new Date();
	
	@Required
	@Formats.DateTime(pattern="yyyy/MM/dd HH:mm")
	public Date end = new Date();
	
	public Boolean endsSameDay;
	public Boolean allDay=true;
	
	@Enumerated(EnumType.STRING)
	public EventTypes eventType;
	
	@Enumerated(EnumType.STRING)
	public OperationStates state;
	
	@OneToMany(mappedBy="parentHistory")
	public List<Datasheet> consumption = new ArrayList<Datasheet>();
	
	@ManyToOne
	public Maintenances maint;
	
	@OneToOne
	public Failures fail;
	//@JoinColumn(name="failHistory",referencedColumnName="fail" )
	
	
	@Column(columnDefinition="TEXT")
	public String comments;
	
	public Boolean systemEvent;
	
	public Boolean isHappened;
	
	@ManyToOne
	public Users registrar;
	
	//Query creation helper
		public static Model.Finder<Long,History> find =
				new Model.Finder<Long,History>(Long.class,History.class);
		
		
	public String validate() {
		if(allDay==null || isHappened==null)
			return "Boolean fields can't be null";
		else if(start.getTime()>=end.getTime())
			return "Event can't finish before it begins";
			else
				return null;
	}
	
		/**
		 * Returns a page of history for display
		 * 
		 * @param page
		 *            Page number
		 * @param pageSize
		 *            Size of each page
		 * @param sortBy
		 *            parameter to sort the plants by, typically name
		 * @param order
		 *            order of sort
		 * @param filter
		 *            Search filter, for plants, both name and description are used for
		 *            search
		 * @return Page<Plants>: A page of History
		 */
		public static Page<History> page(String fieldName, Long parentId, int page, int pageSize,
				String sortBy, String order, String filter) {
			
			Date start=new Date(),end = new Date();
			boolean cf = false;
			if(filter.matches("(13|14)\\d\\d(/)(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])-"
					+ "(13|14)\\d\\d\\2(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])")) {
						String[] dates = filter.split("-");
						start = DateTimeUtils.getGregorianDateTime(dates[0] + " 00:00");
						end = DateTimeUtils.getGregorianDateTime(dates[1] + " 00:00");
						cf = true;
			}
			
			return findHistories(fieldName,parentId, filter,cf, start, end,"","")
						.orderBy(sortBy + " " + order)
	                    .findPagingList(pageSize)
	                    .setFetchAhead(false)
	                    .getPage(page);
		}
		
		/**
		 * 
		 * @param parentId
		 * @param page
		 * @param pageSize
		 * @param sortBy
		 * @param order
		 * @param filter
		 * @param cf
		 * @param start
		 * @param end
		 * @return
		 */
		public static Query<History> findHistories(String fieldName, Long parentId, String filter,
				boolean cf, Date start, Date end, String calType, String occurString) {
			com.avaje.ebean.Query<History> query = Ebean.createQuery(History.class);
			String whoql="";
			switch (fieldName) {
			case "parentInstallation.id":
				whoql =
					"(parentInstallation.id = :parentId OR " +
					"parentPlant.installation.id = :parentId OR " +
					"parentSection.plant.installation.id = :parentId OR " +
					"parentEquipment.section.plant.installation.id = :parentId OR " +
					"parentSubunit.equipment.section.plant.installation.id = :parentId OR " +
					"parentComponent.subunit.equipment.section.plant.installation.id = :parentId) AND ";
				break;
				
			case "parentPlant.id":
				whoql =
					"(parentPlant.id = :parentId OR " +
					"parentSection.plant.id = :parentId OR " +
					"parentEquipment.section.plant.id = :parentId OR " +
					"parentSubunit.equipment.section.plant.id = :parentId OR " +
					"parentComponent.subunit.equipment.section.plant.id = :parentId) AND ";
				break;
				
			case "parentSection.id":
				whoql =
					"(parentSection.id = :parentId OR " +
					"parentEquipment.section.id = :parentId OR " +
					"parentSubunit.equipment.section.id = :parentId OR " +
					"parentComponent.subunit.equipment.section.id = :parentId) AND ";
				break;
			
			case "parentEquipment.id":
				whoql =
					"(parentEquipment.id = :parentId OR " +
					"parentSubunit.equipment.id = :parentId OR " +
					"parentComponent.subunit.equipment.id = :parentId) AND ";
				break;
			
			case "parentSubunit.id":
				whoql =
					"(parentSubunit.id = :parentId OR " +
					"parentComponent.subunit.id = :parentId) AND ";
				break;
				
			case "parentComponent.id":
				whoql =
					"(parentComponent.id = :parentId) AND ";
				break;
				
			default:
				break;
			}
			if(occurString!="") {
				if(occurString.equals("actual")) whoql = whoql.concat("(isHappened = true) AND ");
				else if(occurString.equals("planned")) whoql = whoql.concat("(isHappened = false) AND ");
			}
			
			if(!calType.equals("")) {
				whoql = whoql.concat("(maint.maintenanceCategory = :pm) AND ");
			} 
			
			if(cf) {
				whoql =	whoql.concat("((start < :startDate AND end > :endDate) OR " +
						"(start >= :startDate AND start <= :endDate) OR " +
						"(end >= :startDate AND end <= :endDate))");
			}  else {
				whoql = whoql.concat("(lower(eventType) like :filter OR " +
						"lower(maint.responsibleDiscipline.name) like :filter)");
			}
				
				query.where(whoql);
				
				query
				.setParameter("parentId",parentId);

				if(!calType.equals("")) query.setParameter("pm",calType);
				
				if(cf) {
					query
					.setParameter("startDate",start)
					.setParameter("endDate",end);
				} else query.setParameter("filter","%"+filter+"%");
			return query;
		}
		
		/**
		 * 
		 * @param start
		 * @param end
		 * @return
		 */
		public static List<History> findInDateRange(Date start, Date end) {

	        return find.where().or(
	                Expr.and(
	                        Expr.lt("start", start),
	                        Expr.gt("end", end)
	                ),
	                Expr.or(
	                        Expr.and(
	                                Expr.gt("start", start),
	                                Expr.lt("start", end)
	                        ),
	                        Expr.and(
	                                Expr.gt("end", start),
	                                Expr.lt("end", end)
	                        )
	                )
	        ).findList();
	    }
		
		/**
		 * Gives a list of perfomred PM routines on item
		 * @param parentLevel
		 * @param parentId
		 * @return
		 */
		public static List<History> findLastRoutines(Integer parentLevel, Long parentId) {
			List<History> resultList = new ArrayList<History>();
			List<PreventiveMaintenances> levelPmRoutines = new ArrayList<PreventiveMaintenances>();
			String fieldName = new String();
			switch(parentLevel) {
			case 5:
				levelPmRoutines = PreventiveMaintenances.find.where().eq("parentSection.id",parentId).findList();
				fieldName = "parentSection.id";
				break;
			case 6:
				levelPmRoutines = PreventiveMaintenances.find.where().eq("parentEquipment.id",parentId).findList();
				levelPmRoutines.addAll(PreventiveMaintenances.find.where().eq("pmClass.id",Equipments.find.byId(parentId).equipmentClass.id).findList());
				fieldName = "parentEquipment.id";
				break;
			default:
				return null;
			}
			if(levelPmRoutines.size()==0) return null;
			for(PreventiveMaintenances routine : levelPmRoutines) {
				History lastRoutine = new History();
				List<History> routineMaints = History.find.where()
						.or(Expr.eq("maint.classLevelPMRoutine.id", routine.id),
								Expr.eq("maint.pmRoutine.id",routine.id))
						.eq(fieldName,parentId)
						.eq("isHappened", true)
						.orderBy("end desc")
						.findList();
				if(routineMaints.size()!=0) {
					lastRoutine = routineMaints.get(0);
					resultList.add(lastRoutine);
				}
					
			}
			return resultList;
		}
		
		/**
		 * Computes up time from given date
		 * @param parentLevel
		 * @param parentId
		 * @return
		 */
		public static Long upTime(Integer parentLevel, Long parentId, Date sDate, Date eDate) {
			
			String fieldName = new String();
			switch(parentLevel) {
			case 3:
				fieldName = "parentInstallation.id";
				break;
			case 4:
				fieldName = "parentPlant.id";
				break;
			case 5:
				fieldName = "parentSection.id";
				break;
			case 6:
				fieldName = "parentEquipment.id";
				break;
			case 7:
				fieldName = "parentSubunit.id";
				break;
			case 8:
				fieldName = "parentComponent.id";
				break;
			default:
				return -1L;
			}
			
			String whoql =
					fieldName + " = :pid AND " +
					"eventType = 'NORMAL_OPERATION' AND " +
					"isHappened = TRUE AND " +
					"end > :beginning AND start < :ending AND " +
					"(state='RUNNING' OR state='HOT_STANDBY' OR state='START_UP' OR state='IDLE' OR state='TESTING')";
			
			com.avaje.ebean.Query<History> query = Ebean.createQuery(History.class);
			query.where(whoql).
			setParameter("pid",parentId)
			.setParameter("beginning",sDate)
			.setParameter("ending", eDate);
			
			Long totalOH = 0L;
			List<History> allRunningHistory = query.findList();
			if(allRunningHistory.size()!=0) {
				for(History h : query.findList()) {
					totalOH += (h.end.getTime()-h.start.getTime()) / 3600000;
				}
			}
			return totalOH;
		}
}