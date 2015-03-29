package models.maintenance;

import javax.persistence.*;

import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.*;

import java.util.*;

@Entity
@Table(name="maintenance_groups")
public class MaintenanceGroups extends Model {
	
	private static final long serialVersionUID = -2817380891528464364L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;
	
	@Column(unique=true)
	@Formats.NonEmpty
	@Required
	public String name;	
	
	@OneToMany(mappedBy="maintenanceGroup")
	public List<Maintenances> memberMaints = new ArrayList<Maintenances>();
	
	public Float percentComplete;
	
	@Column(columnDefinition = "TEXT")
	public String comments;
	
	//***************************************************************
	
	//Query creation helper
	public static Model.Finder<Long,MaintenanceGroups> find = 
			new Model.Finder<Long,MaintenanceGroups>(Long.class,MaintenanceGroups.class);
	
	
	/**
	 * 
	 * @return
	 */
	public static Map<String, String> options() {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (MaintenanceGroups m : MaintenanceGroups.find.all()) {
			options.put(m.id.toString(), m.name);
		}
		return options;
	}
	
}