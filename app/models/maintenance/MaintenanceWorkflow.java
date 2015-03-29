package models.maintenance;

import java.util.Date;
import java.util.List;

import javax.persistence.*;

import models.*;
import play.db.ebean.*;

@Entity
@Table(name="workflow")
public class MaintenanceWorkflow extends Model {

	private static final long serialVersionUID = 2025939083570750600L;


	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	public Long id;
	
	@ManyToOne
	public Maintenances maintenance;
	
	@ManyToOne
	public Users user;
	
	@OneToOne
	public WorkflowTree workflowStage;
	
	public Boolean acceptReject;
	
	public String rejectReason;
	
	public Date actionDate;	
	
	// Query creation helper
	public static Model.Finder<Long, MaintenanceWorkflow> find = 
		new Model.Finder<Long, MaintenanceWorkflow>(Long.class, MaintenanceWorkflow.class);
	
	public static List<MaintenanceWorkflow> workflowList(Long maintenanceId) {
		return MaintenanceWorkflow.find
				.orderBy("actionDate")
				.where()
				.eq("maintenance.id", maintenanceId)
				.findList();
		}
	
}
