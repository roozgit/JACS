package models.maintenance;

import javax.persistence.*;

import models.Users;
import play.data.format.Formats;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;


@Entity
@Table(name="maintenances_users", uniqueConstraints=@UniqueConstraint(columnNames={"maintenance_id","user_id"}))
public class MaintenancesUsers extends Model {

	private static final long serialVersionUID = 8151558266808238599L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Long id;
	
	@ManyToOne
	public Maintenances maintenance;
	
	@ManyToOne
	public Users user;
	
	@Required
	@Formats.NonEmpty
	@Min(0)
	public Float hours;
	
	// Query creation helper
	public static Model.Finder<Long, MaintenancesUsers> find = 
		new Model.Finder<Long, MaintenancesUsers>(Long.class, MaintenancesUsers.class);
}
