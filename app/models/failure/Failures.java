package models.failure;


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
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import models.History;
import models.Impacts;
import models.maintenance.Maintenances;
import myUtils.DateTimeUtils;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Page;
import com.avaje.ebean.Query;


@Entity
@Table(name="failures")
public class Failures extends Model {

	private static final long serialVersionUID = -4137986516546471679L;
	
	@Id
	@GeneratedValue
	public Long id;
	
	@OneToOne(mappedBy="fail")
	public History failHistory;
	
	@ManyToOne
	public Failures parentFailure;
		
	@Enumerated(EnumType.STRING)
	public Severity severity;
	
	@Enumerated(EnumType.STRING)
	public Impacts functionImpact;
	
	@Enumerated(EnumType.STRING)
	public Impacts operationImpact;
	
	@Enumerated(EnumType.STRING)
	public Impacts safetyImpact;
	
	@ManyToOne
	public FailureModes failureMode;
	
	@ManyToOne
	public FailureMechanisms failureMechanism;
	
	@ManyToMany
	public List<FailureCauses> failureCauses = new ArrayList<FailureCauses>();
	
	@Enumerated(EnumType.STRING)
	public DetectionMethods detectionMethod;

	@Column(columnDefinition = "TEXT")
	public String comments;
	
	@ManyToMany(mappedBy="correspondingFailure")
	public List<Maintenances> correctiveActions = new ArrayList<Maintenances>();

	public static Model.Finder<Long,Failures> find = new Model.Finder<Long,Failures>(Long.class,Failures.class);
	
	/**
	 * 
	 * @return
	 */
	public static Map<String, String> options() {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Failures f : Failures.find.all()) {
			options.put(f.id.toString(), f.id.toString()+"."+f.failureMode.failureModeCode.toString()+"---"+f.comments.toString() +
					"---"+ myUtils.DateTimeUtils.getIranianDateTime(f.failHistory.start));
		}
		return options;
	}
	
	public static Map<String, String> options(Long equipmentId) {
		
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Failures f : Failures.find.all()) {
			//History fhis = History.find.where().eq("fail.id", f.id).findUnique();
			
			//Put all equipments with this id that have failures
			if(f.failHistory.parentEquipment!=null) {
				if(equipmentId.equals(f.failHistory.parentEquipment.id))
					options.put(f.id.toString(), f.id.toString()+"."+f.failureMode.failureModeCode.toString()+"---"+f.comments.toString() +
						"---"+ myUtils.DateTimeUtils.getIranianDateTime(f.failHistory.start));
			}
			//Put all subunits which are children of equipmentId and have failures
			if(f.failHistory.parentSubunit!=null) {
				if(equipmentId.equals(f.failHistory.parentSubunit.equipment.id)) {
					options.put(f.id.toString(), f.id.toString()+"."+f.failureMode.failureModeCode.toString()+"---"+f.comments.toString() +
							"---"+ myUtils.DateTimeUtils.getIranianDateTime(f.failHistory.start));
				}
			}
			//Put all components which are children of equipmentId and have failures
			if(f.failHistory.parentComponent!=null) {
				if(equipmentId.equals(f.failHistory.parentComponent.subunit.equipment.id)) {
					options.put(f.id.toString(), f.id.toString()+"."+f.failureMode.failureModeCode.toString()+"---"+f.comments.toString() +
							"---"+ myUtils.DateTimeUtils.getIranianDateTime(f.failHistory.start));
				}
			}
			
			
			
		}
		return options;
	}
	
	/**
	 * 
	 * @param equipmentId
	 * @return
	 */
	public static Map<String, String> options(Long sectionId, Long equipmentId) {
		
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		Long realSection = sectionId;
		if(equipmentId>0) realSection=-1L;
		List<Failures> opFails = Failures.find.where().or(
				Expr.eq("failHistory.parentEquipment.id",equipmentId),
				Expr.eq("failHistory.parentSection.id",realSection))
				.findList();
		
		for (Failures f : opFails) {
			options.put(f.id.toString(), f.id.toString()+"."+f.failureMode.failureModeCode.toString()+"---"+f.comments.toString() +
					"---"+ myUtils.DateTimeUtils.getIranianDateTime(f.failHistory.start));
		}
		return options;
	}
	
	public static Query<Failures> failureQueryBuilder(Long equipmentId, 
			String filter1, String filter2, String filter3,String filter4,String filter5,String filter6,String filter7,String filter8, String filter9) {
		boolean cf=false;
		Date start = new Date();
		Date end = new Date();
		
		String whoql = "(failHistory.parentEquipment.id = :eqId OR failHistory.parentSubunit.equipment.id = :eqId OR failHistory.parentComponent.subunit.equipment.id = :eqId) ";
		//Filter 1 is for failure start date
		if(filter1.matches("(13|14)\\d\\d(/)(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])-"
				+ "(13|14)\\d\\d\\2(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])")) {
					String[] dates = filter1.split("-");
					start = DateTimeUtils.getGregorianDateTime(dates[0] + " 00:00");
					end = DateTimeUtils.getGregorianDateTime(dates[1] + " 00:00");
					cf = true;
		}
		if(cf) whoql=whoql.concat(" AND (failHistory.start >= :startDate AND failHistory.start <= :endDate) ");
		if(filter2.length()!=0) whoql=whoql.concat(" AND (severity like :sev) ");
		if(filter3.length()!=0) whoql=whoql.concat(" AND (functionImpact like :fimp) ");
		if(filter4.length()!=0) whoql=whoql.concat(" AND (operationImpact like :oimp) ");
		if(filter5.length()!=0) whoql=whoql.concat(" AND (safetyImpact like :simp) ");
		if(filter6.length()!=0) whoql=whoql.concat(" AND (failureMode.id =:fmc) ");
		if(filter7.length()!=0) whoql=whoql.concat(" AND (failureMechanism.id = :fme) ");
		if(filter8.length()!=0) whoql=whoql.concat(" AND (detectionMethod like :dtm)");
		if(filter9.length()!=0) whoql=whoql.concat(" AND (lower(comments) like :fil9)");
		Query<Failures> query = Ebean.createQuery(Failures.class);
		query.where(whoql);
		query.setParameter("eqId", equipmentId);
		if(cf) query.setParameter("startDate", start).setParameter("endDate",end);
		if(filter2.length()!=0) query.setParameter("sev", filter2);
		if(filter3.length()!=0) query.setParameter("fimp", filter3);
		if(filter4.length()!=0) query.setParameter("oimp", filter4);
		if(filter5.length()!=0) query.setParameter("simp", filter5);
		if(filter6.length()!=0) query.setParameter("fmc", filter6);
		if(filter7.length()!=0) query.setParameter("fme", filter7);
		if(filter8.length()!=0) query.setParameter("dtm", filter8);
		if(filter9.length()!=0) query.setParameter("fil9", "%"+filter9+"%");
		
		return query;
	}
	
	/**
	 * Returns a page of Failure mechanisms for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort the equipmentClass by, typically name
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter, for equipmentClass, name and description, and codes
	 *            search
	 * @return Page<Failures>: A page of Failures
	 */
	public static Page<Failures> page(Long equipmentId, int page, int pageSize, String sortBy,
			String order, String filter1, String filter2, String filter3,String filter4,String filter5,String filter6,String filter7,String filter8, String filter9) {

		return failureQueryBuilder(equipmentId,filter1,filter2,filter3,filter4,filter5,filter6,filter7,filter8,filter9).orderBy(sortBy + " " + order).
				findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}
}
