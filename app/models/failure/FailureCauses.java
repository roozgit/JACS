package models.failure;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.*;

import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;


@Entity
@Table(name="failure_causes")

public class FailureCauses extends Model {

	private static final long serialVersionUID = -6717126409503475091L;

	@Id
	@GeneratedValue
	public Long id;
	
	public Integer codeNumber;
	
	public String notation;
	
	public Integer subdivisionCodeNumber;
	
	public String subdivisionNotation;
	
	public String description;

	//Query creation helper
	public static Model.Finder<Long,FailureCauses> find = 
		new Model.Finder<Long,FailureCauses>(Long.class, FailureCauses.class);
	
		public static Map<String,String> options() {
        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        	for(FailureCauses ec: FailureCauses.find.all()) {
            	options.put(ec.id.toString(), ec.notation+"---"+ec.subdivisionNotation);
        	}
        return options;
	}

		/**
		 * Returns a page of Failure causes for  display
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
		public static Page<FailureCauses> page(int page, int pageSize, String sortBy,
				String order, String filter) {
			String whoql = 
					"lower(notation) like :filter OR " +
					"lower(subdivisionNotation) like :filter OR " +
					"lower(description) like :filter";

			
			com.avaje.ebean.Query<FailureCauses> query = Ebean.createQuery(FailureCauses.class);
			query.where(whoql).
			setParameter("filter","%"+filter+"%");
			
			return query.orderBy(sortBy + " " + order).
					findPagingList(pageSize)
					.setFetchAhead(false).getPage(page);
		}

}
