package models.maintenance;

import javax.persistence.*;

import models.Components;
import play.db.ebean.Model;
import play.data.format.Formats;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;

@Entity
@Table(name="maintenances_components", uniqueConstraints=@UniqueConstraint(columnNames={"maintenance_id","component_id"}))
public class MaintenancesComponents extends Model {

	private static final long serialVersionUID = 78625798624056923L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Long id;
	
	@ManyToOne
	public Maintenances maintenance;
	
	@ManyToOne
	public Components component;
	
	@Enumerated(EnumType.STRING)
	public MaintenanceActivities maintenanceActivity;
	
	@Required
	@Formats.NonEmpty
	@Min(0)
	public Float timeToRepair;
	
	
	// Query creation helper
		public static Model.Finder<Long, MaintenancesComponents> find = 
				new Model.Finder<Long, MaintenancesComponents>(Long.class, MaintenancesComponents.class);
	
}
