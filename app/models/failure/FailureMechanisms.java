package models.failure;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

import play.db.ebean.*;
import java.util.*;

@Entity
@Table(name="failure_mechanisms")

public class FailureMechanisms extends Model {

	private static final long serialVersionUID = 4499444754279614511L;

	@Id
	@GeneratedValue
	public Long id;
	
	public Integer codeNumber;
	
	public String notation;
	
	public Integer subdivisionCodeNumber;
	
	public String subdivisionNotation;
	
	public String description;

	//Query creation helper
	public static Model.Finder<Long,FailureMechanisms> find = 
		new Model.Finder<Long,FailureMechanisms>(Long.class, FailureMechanisms.class);
	
		public static Map<String,String> options() {
        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        	for(FailureMechanisms ec: FailureMechanisms.find.all()) {
            	options.put(ec.id.toString(), ec.notation+"---"+ec.subdivisionNotation);
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
		public static Page<FailureMechanisms> page(int page, int pageSize, String sortBy,
				String order, String filter) {
			String whoql = 
					"lower(notation) like :filter OR " +
					"lower(subdivisionNotation) like :filter OR " +
					"lower(description) like :filter";

			
			com.avaje.ebean.Query<FailureMechanisms> query = Ebean.createQuery(FailureMechanisms.class);
			query.where(whoql).
			setParameter("filter","%"+filter+"%");
			
			return query.orderBy(sortBy + " " + order).
					findPagingList(pageSize)
					.setFetchAhead(false).getPage(page);
		}

}
