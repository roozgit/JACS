package models.equipment;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.*;

import play.data.validation.Constraints.Required;
import play.db.ebean.*;


@Entity
@Table(name="equipment_category")

public class EquipmentCategory extends Model {

	private static final long serialVersionUID = 1321189071875278595L;
	
	@Id
	public Long id;
	
	@Required
	public String name;
	
	//Query creation helper
		public static Model.Finder<Long,EquipmentCategory> find = 
			new Model.Finder<Long,EquipmentCategory>(Long.class, EquipmentCategory.class);
		
			public static Map<String,String> options() {
	        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
	        	for(EquipmentCategory ec: EquipmentCategory.find.all()) {
	            	options.put(ec.id.toString(), ec.name);
	        	}
	        return options;
		}
	}
