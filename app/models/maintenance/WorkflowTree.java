package models.maintenance;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.*;

import models.SecurityRole;
import play.db.ebean.Model;

@Entity
@Table(name="workflow_tree")
public class WorkflowTree extends Model {

	private static final long serialVersionUID = 7930200679020387832L;

	@Id
	public Long id;
	
	@Enumerated(EnumType.STRING)
	public MaintenanceCategories treeCategory;
	
	@ManyToOne
	public SecurityRole decidingRole;
	
	@ManyToOne
	public SecurityRole referringRole;
	
	@ManyToOne
	public SecurityRole receivingRole;
	
	public String description;
	
	//Query creation helper
		public static Model.Finder<Long,WorkflowTree> find = 
				new Model.Finder<Long,WorkflowTree>(Long.class,WorkflowTree.class);
	
	public static Map<String, String> options(Long stageId) {
		Long refId,recId,decId;
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		
		if(stageId==0) {
			for(WorkflowTree wt : WorkflowTree.find.all()) {
				if( wt.referringRole==null)
					options.put(wt.id.toString(), wt.description);
			}
			return options;
		}
		WorkflowTree currentStage = WorkflowTree.find.byId(stageId);
		//options.put(stageId.toString(),currentStage.description);
		try {
			refId = currentStage.referringRole.id;			
		} catch (NullPointerException e) {
			refId=-1L;
		}
		
		try {
			recId = currentStage.receivingRole.id;			
		} catch (NullPointerException e) {
			recId=-1L;
		}
		decId = currentStage.decidingRole.id;

		for (WorkflowTree wt : WorkflowTree.find.all()) {
			if( ( (wt.decidingRole.id==refId && wt.receivingRole.id==decId) || (wt.decidingRole.id==recId && wt.referringRole.id==decId))  &&
					wt.treeCategory == currentStage.treeCategory)
				options.put(wt.id.toString(), wt.description);
		}
		return options;
	}

}