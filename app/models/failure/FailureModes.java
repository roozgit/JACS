package models.failure;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

import play.data.validation.Constraints.Max;
import play.data.validation.Constraints.Min;
import play.db.ebean.Model;


@Entity
@Table(name="failure_modes")

public class FailureModes extends Model {

	private static final long serialVersionUID = -6717126409503475091L;

	@Id
	@GeneratedValue
	public Long id;
	
	@Min(1)
	@Max(3)
	public Integer typeNumber;
	
	@javax.persistence.Column(length=3)
	@play.data.validation.Constraints.MaxLength(3)
	public String failureModeCode;
	
	public String description;
	
	//Query creation helper
	public static Model.Finder<Long,FailureModes> find = 
		new Model.Finder<Long,FailureModes>(Long.class, FailureModes.class);
	
		public static Map<String,String> options() {
        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        	for(FailureModes ec: FailureModes.find.all()) {
            	options.put(ec.id.toString(), ec.failureModeCode.toString()+":"+ec.description);
        	}
        return options;
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
		 * @return Page<FailureMechanism>: A page of EquipmentClass
		 */
		public static Page<FailureModes> page(int page, int pageSize, String sortBy,
				String order, String filter) {
			String whoql = 
					"lower(failureModeCode) like :filter OR " +
					"lower(description) like :filter";

			
			com.avaje.ebean.Query<FailureModes> query = Ebean.createQuery(FailureModes.class);
			query.where(whoql).
			setParameter("filter","%"+filter+"%");
			
			return query.orderBy(sortBy + " " + order).
					findPagingList(pageSize)
					.setFetchAhead(false).getPage(page);
		}

}
