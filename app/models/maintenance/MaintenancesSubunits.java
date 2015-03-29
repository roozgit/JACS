package models.maintenance;


import javax.persistence.*;

import play.data.format.Formats;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;
import models.*;

@Entity
@Table(name="maintenances_subunits", uniqueConstraints=@UniqueConstraint(columnNames={"maintenance_id","subunit_id"}))
public class MaintenancesSubunits extends Model {

	private static final long serialVersionUID = 2931665700608850057L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Long id;
	
	@ManyToOne
	public Maintenances maintenance;
	
	@ManyToOne
	public Subunits subunit;
	
	@Enumerated(EnumType.STRING)
	public MaintenanceActivities maintenanceActivity;
		
	@Required
	@Formats.NonEmpty
	@Min(0)
	public Float timeToRepair;
	
	
	// Query creation helper
		public static Model.Finder<Long, MaintenancesSubunits> find = 
				new Model.Finder<Long, MaintenancesSubunits>(Long.class, MaintenancesSubunits.class);
	
}
