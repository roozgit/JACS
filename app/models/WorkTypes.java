package models;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.data.validation.Constraints.Required;
import play.db.ebean.*;

@Entity
@Table(name="work_types")
public class WorkTypes extends Model {

	private static final long serialVersionUID = 5046385016510035032L;

	@Id
	public Long id;
	
	@Required
	public String workType;
	
	@Required
	public String workValue;
	
	// Query creation helper
			public static Model.Finder<Long, WorkTypes> find = new Model.Finder<Long, WorkTypes>(
					Long.class, WorkTypes.class);
			
	/**
	 * A list of work types for use in html select element in views
	 * @return Map<id,name> of equipments
	 */
	public static Map<String,String> options() {
        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        	for(WorkTypes wt: WorkTypes.find.all()) {
            	options.put(wt.id.toString(), wt.workType);
        	}
        	return options;
	}
	
}
