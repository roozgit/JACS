package models;

import java.util.LinkedHashMap;
import java.util.Map;

import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import javax.persistence.*;


@Entity
@Table(name = "dimensions")
public class Dimensions extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1794781565344052751L;

	@Id
	public Long id;
	
	@Required
	@Formats.NonEmpty
	public String name;
	
	public String comments;
	
	
	// Query creation helper
		public static Model.Finder<Long, Dimensions> find = new Model.Finder<Long, Dimensions>(
				Long.class, Dimensions.class);
	
		public static Map<String, String> options() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
			for (Dimensions co : Dimensions.find.all()) {
				options.put(co.id.toString(),co.name);
			}
			return options;
		}
}
