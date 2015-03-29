package models.maintenance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.CustomBean;
import models.Disciplines;
import models.Plants;
import models.Sections;
import models.equipment.EquipmentClass;
import models.equipment.Equipments;
import play.data.format.Formats;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
@Table(name="preventive_maitenances")
public class PreventiveMaintenances extends Model {

	private static final long serialVersionUID = 8505189596723388912L;

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	public Long id;
	
	@ManyToOne
	public Plants parentPlant;
	
	@ManyToOne
	public Sections parentSection;
	
	@ManyToOne
	public Equipments parentEquipment;
	
	@Required
	@Formats.NonEmpty
	@MaxLength(30)
	public String name;
	
	@Column(columnDefinition = "TEXT")
	public String description;
	
	@Column(columnDefinition = "TEXT")
	public String safetyRequirements;
	
	@ManyToOne
	public Disciplines actingDiscipline;
	
	public Float intervalDays=0f;
	
	public Float intervalOperationHours=0f;
	
	public Boolean onShutDown=false;
	
	@ManyToOne
	public EquipmentClass pmClass;
	
	//Query creation helper
	public static Model.Finder<Long,PreventiveMaintenances> find =
			new Model.Finder<Long,PreventiveMaintenances>(Long.class,PreventiveMaintenances.class);
	
	/**
	 * A map of ids and PMs for use in html select element
	 * @param id
	 * @return
	 */
	public static Map<String,String> options(Long sectionId, Long equipmentId) {
    	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
    	if(equipmentId!=-1L) {
    		for(PreventiveMaintenances pm: PreventiveMaintenances.find.all()) {
        		if(pm.parentEquipment!=null && pm.parentEquipment.id == equipmentId) options.put(pm.id.toString(), pm.name + "---" + pm.parentEquipment.name);
        	}
    	} else if(sectionId!=-1L) {
    		for(PreventiveMaintenances pm: PreventiveMaintenances.find.all()) {
        		if(pm.parentSection!=null && pm.parentSection.id == sectionId) options.put(pm.id.toString(), pm.name + "---" + pm.parentSection.name);
        	}
    	}
    	
    	return options;
	}
	
	public static Map<String,String> optionsClassLevel(Long equipmentId) {
    	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
    	Long ecid;
    	if(Equipments.find.byId(equipmentId) != null)
    		ecid = Equipments.find.byId(equipmentId).equipmentClass.id;
    	else ecid=-1L;

    	for(PreventiveMaintenances pm: PreventiveMaintenances.find.where()
    			.eq("pmClass.id",ecid).findList()) {
    		options.put(pm.id.toString(), pm.name + "---"+pm.pmClass.name +"-"+pm.pmClass.ecType);
    	}
    	return options;
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<CustomBean> pmItems() {
		String items[] = this.description.split("\\r?\\n");
		ArrayList<CustomBean> pmBeanList = new ArrayList<CustomBean>();
		for(int i=0;i<items.length;i++) {
			pmBeanList.add(produce(
					i+1,
					items[i].replace("ی","ي")
					));
		}
		return pmBeanList;
	}
	
	/**
	 *
	 */
	public CustomBean produce(
			 Integer crow,
			 String cpmActivity
		)
	{
		CustomBean cb = new CustomBean();
		cb.setRow(crow);
		cb.setItem(cpmActivity);
		return cb;
		
	}
	
	public static Long getParent(Long pmRoutineId) {
		PreventiveMaintenances pm = PreventiveMaintenances.find.byId(pmRoutineId);
		if(pm.pmClass!=null) return pm.pmClass.id;
		if(pm.parentEquipment!=null) return pm.parentEquipment.id;
		return -1L;
	}
	
}
