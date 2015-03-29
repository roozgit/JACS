package models;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="hiring_types")
public class HiringTypes extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = -410474651995964079L;
	
	@Id
	public Long id;
	
	public String hiringType;
	
	// Query creation helper
				public static Model.Finder<Long, HiringTypes> find = new Model.Finder<Long, HiringTypes>(
						Long.class, HiringTypes.class);
				
		/**
		 * A list of work types for use in html select element in views
		 * @return Map<id,name> of equipments
		 */
		public static Map<String,String> options() {
	        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
	        	for(HiringTypes wt: HiringTypes.find.all()) {
	            	options.put(wt.id.toString(), wt.hiringType);
	        	}
	        	return options;
		}

}
