package models;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
@Table(name="inventory_events")
public class InventoryEvents extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1617833899823700589L;
	
	@Id
	public Long id;
	
	@Required
	@Formats.NonEmpty
	public String name;
	
	public String otherLanguageName;
	
	public String comments;
	
	// Query creation helper
				public static Model.Finder<Long, InventoryEvents> find = new Model.Finder<Long, InventoryEvents>(
						Long.class, InventoryEvents.class);
				
		/**
		 * A list of work types for use in html select element in views
		 * @return Map<id,name> of equipments
		 */
		public static Map<String,String> options() {
	        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
	        	for(InventoryEvents wt: InventoryEvents.find.all()) {
	            	options.put(wt.id.toString(), wt.name);
	        	}
	        	return options;
		}
	
}