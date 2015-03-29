package models.equipment;

import javax.persistence.*;

import models.failure.FailureModes;
import models.maintenance.PreventiveMaintenances;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.*;

import java.util.*;

@Entity
@Table(name="equipment_class")

public class EquipmentClass extends Model {
	
	private static final long serialVersionUID = 3534054611730902413L;

	@Id
	@GeneratedValue
	public Long id;
	
	@Required
	@ManyToOne
	public EquipmentCategory equipmentCategory;

	@Required
	@Formats.NonEmpty
	public String name;
	
	public String nameCode;
	
	public String ecType;
	
	public String ecTypeCode;
	
	@Column(columnDefinition = "TEXT")
	public String ecTypeDescription;
	
	@ManyToMany
	public List<FailureModes> possibleFailureModes = new ArrayList<FailureModes>();
	
	@Column(columnDefinition = "TEXT")
	public String generalMaintenaceMethod;
	
	@Column(columnDefinition = "TEXT")
	public String generalOperationMethod;
	
	@Column(columnDefinition = "TEXT")
	public String generalMaintenaceSafetyRequirements;
	
	@Column(columnDefinition = "TEXT")
	public String generalOperationSafetyRequirements;

	@Column(columnDefinition = "TEXT")
	public String generalOperatorRequiredCourses;

	@Column(columnDefinition = "TEXT")
	public String generalMaintenanceRequiredCourses;
	
	@OneToMany(mappedBy="pmClass")
	public List<PreventiveMaintenances> classPMList = new ArrayList<PreventiveMaintenances>();
	//Query creation helper
	public static Model.Finder<Long,EquipmentClass> find = 
		new Model.Finder<Long,EquipmentClass>(Long.class, EquipmentClass.class);
	
		public static Map<String,String> options() {
        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        	for(EquipmentClass ec: EquipmentClass.find.
        			where().
        			orderBy("equipmentCategory.name").
        			findList()
        			) {
            	options.put(ec.id.toString(), ec.equipmentCategory.name+"---"+ec.name+"---"+ec.ecType);
        	}
        return options;
	}

		/**
		 * Returns a page of equipmentclasses for display
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
		 * @return Page<Sections>: A page of EquipmentClass
		 */
		public static Page<EquipmentClass> page(int page, int pageSize, String sortBy,
				String order, String filter) {
			String whoql = 
					"lower(name) like :filter OR " +
					"lower(equipmentCategory.name) like :filter OR " +
					"lower(ecType) like :filter OR " +
					"lower(nameCode) like :filter OR " +
					"lower(ecTypeCode) like :filter";
			
			com.avaje.ebean.Query<EquipmentClass> query = Ebean.createQuery(EquipmentClass.class);
			query.where(whoql).
			setParameter("filter","%"+filter+"%");
			
			return query.orderBy(sortBy + " " + order).
					findPagingList(pageSize)
					.setFetchAhead(false).getPage(page);
		}

}
