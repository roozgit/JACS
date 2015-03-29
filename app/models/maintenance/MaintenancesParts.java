package models.maintenance;


import javax.persistence.*;

import models.Parts;
import play.data.format.Formats;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
@Table(name="maintenances_parts", uniqueConstraints=@UniqueConstraint(columnNames={"maintenance_id","part_id"}))
public class MaintenancesParts extends Model {

	private static final long serialVersionUID = 3226626986368597949L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Long id;
	
	@ManyToOne
	public Maintenances maintenance;
	
	@ManyToOne
	public Parts part;
	
	@Required
	@Formats.NonEmpty
	@Min(0)
	public Float quantity;
	
	public Boolean stockFlag;
	
	// Query creation helper
		public static Model.Finder<Long, MaintenancesParts> find = 
				new Model.Finder<Long, MaintenancesParts>(Long.class, MaintenancesParts.class);
	
}
