package models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.ValidationException;

import models.equipment.EquipmentClass;
import models.equipment.Equipments;
import play.Logger;
import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

@Entity
@Table(name = "components")
public class Components extends Model {

	private static final long serialVersionUID = 3277071428800732633L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;
	
	@ManyToOne
	public Subunits subunit;

	@ManyToOne
	public EquipmentClass componentClass;
	
	@Required
	@Formats.NonEmpty
	public String name;
	
	public String description;
	
	public String componentSerialNo;
	
	//Maker info
	@ManyToOne
	public Companies manufacturerCompany;
	
	public String manufacturerModelDesignation;
	
	@Formats.DateTime(pattern="yyyy/MM")
	public Date manufactureDate;
	
	@Formats.DateTime(pattern="yyyy/MM/dd")
	public Date purchaseDate;
	
	@Column(columnDefinition="TEXT")
	public String guarantee;
	@Formats.DateTime(pattern="yyyy/MM/dd")
	public Date guaranteeEndDate;

	@Column(columnDefinition = "TEXT")
	public String comments;
	
	@ManyToMany
	public List<Blobs> files = new ArrayList<Blobs>();
	


	// Query creation helper
	public static Model.Finder<Long, Components> find = new Model.Finder<Long, Components>(
			Long.class, Components.class);

	/**
	 * A list of Components for use in html select element in views
	 * @return Map<id,name> of equipments
	 */
	public static Map<String, String> options() {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Components co : Components.find.where().isNotNull("id").orderBy("name").findList()) {
			options.put(co.id.toString(),co.name+" in "+co.subunit.name+" in "+co.subunit.equipment.name);
		}
		return options;
	}
	
	/**
	 * Overrides options() and returns list of components for a specific subunit
	 * @param parentSubunitId Id of subunit
	 * @return Map<id,name>
	 */

	public static Map<String, String> options(Long parentSubunitId) {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Components c : Subunits.find.byId(parentSubunitId).components) {
			options.put(c.id.toString(),c.name);
		}
		return options;
	}
	
	/**
	 * Returns components belonging to an equipment. It searches through two levels
	 * TO BE REMOVED LATER
	 * @param equipmentId
	 * @return
	 */
	public static Map<String, String> optionsEquipment(Long equipmentId) {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		
		
		for(Subunits su : Equipments.find.byId(equipmentId).subunits ) {
			for (Components co : su.components) {
				options.put(co.id.toString(),co.name);
			}
		}
		
		return options;
	}
	
	/**
	 * Returns a page of components for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort the equipments by, typically name
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter, for components, name and description, class, maker
	 *            search
	 * @return Page<Sections>: A page of Components
	 */
	public static Page<Components> page(Long parentSubunitId, int page, int pageSize, String sortBy,
			String order, String filter) {
		String whoql =
				"(subunit.id) = :parentSubunitId AND (" +
				"lower(name) like :filter OR " +
				"lower(description) like :filter OR " +
				"lower(componentClass.name) like :filter OR " +
				"lower(manufacturerCompany) like :filter)";
		
		com.avaje.ebean.Query<Components> query = Ebean.createQuery(Components.class);
		query.where(whoql).
		setParameter("filter","%"+filter+"%").
		setParameter("parentSubunitId",parentSubunitId);
		
		return query.orderBy(sortBy + " " + order).
				findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}
	
	/**
	 * 
	 * @param equipmentId
	 * @return
	 */
	public static boolean copy(Long componentId, Long newParentSubunitId) {
		boolean result;
		Ebean.beginTransaction();
		try {
			Components pc = Components.find.byId(componentId);
			Subunits newParentSubunit = Subunits.find.byId(newParentSubunitId);
				Components pastedC = (Components) pc._ebean_createCopy();
				pastedC.id = null;
				pastedC.subunit = newParentSubunit;
				pastedC.save();
				
				//copy component blobs to new equipment
				int fs = pc.files.size();
				pastedC.files = new ArrayList<Blobs>();
				if(fs!=0) {
					for(Blobs bb : pc.files) {
						pastedC.files.add(bb);
					}
				}
				pastedC.saveManyToManyAssociations("files");
				
				//save component datasheet
				List<Datasheet> copiedDsList = Datasheet.find.where().
						eq("parentComponent.id", componentId).
						findList();
				if(copiedDsList.size()!=0) {
					for(Datasheet ds : copiedDsList) {
						Datasheet newDs = new Datasheet();
						newDs = (Datasheet) ds._ebean_createCopy();
						newDs.id=null;
						newDs.parentComponent=pastedC;
						newDs.save();
					}
				}
			Ebean.commitTransaction();
			result = true;
		} catch(Exception ex) {
			Ebean.rollbackTransaction();
			result=false;
		} finally {
			Ebean.endTransaction();
		}
		return result;
	}
	
	
	
	/**
	 * 
	 * @param equipmentId
	 * @return
	 */
	public static boolean copyAsEquipment(Long componentId, Long newSectionId) {
		boolean result;
		Ebean.beginTransaction();
		try {
			Components pc = Components.find.byId(componentId);
			Equipments pe = new Equipments();
			
			//Throw exception if there are parts in this component
			List<PartsComponents> copiedPCList = PartsComponents.find.where().
					eq("component.id", componentId).
					findList();
			if(copiedPCList.size()!=0) {
					throw new ValidationException("Please remove parts in this component or first copy them"
							+ "using copy command in part-component page");
				}
			
			pe.section = Sections.find.byId(newSectionId);
			pc.subunit=null;
			pe.comments= pc.comments;
			pe.equipmentSerialNo= pc.componentSerialNo;
			pe.description= pc.description;
			pe.guarantee= pc.guarantee;
			pe.manufacturerModelDesignation= pc.manufacturerModelDesignation;
			pe.name= pc.name;
			if(pc.componentClass!=null)
				pe.equipmentClass= EquipmentClass.find.byId(pc.componentClass.id);
			else throw new ValidationException("Component has no class! Please specify class before upgrading"
					+ "it to equipment level");
			pe.manufactureDate = pc.manufactureDate;
			pe.manufacturerCompany =
					pc.manufacturerCompany==null ? null : Companies.find.byId(pc.manufacturerCompany.id);
			pe.manufacturerModelDesignation = pc.manufacturerModelDesignation;
			pe.purchaseDate = pc.purchaseDate;
			pe.save();
				//move component blobs to new equipment
				int fs = pc.files.size();
				pe.files = new ArrayList<Blobs>();
				if(fs!=0) {
					for(Blobs bb : pc.files) {
						pe.files.add(bb);
					}
				}
				pe.saveManyToManyAssociations("files");
				pc.deleteManyToManyAssociations("files");
				
				//Move component datasheet to new equipment
				List<Datasheet> copiedDsList = Datasheet.find.where().
						eq("parentComponent.id", componentId).
						findList();
				if(copiedDsList.size()!=0) {
					for(Datasheet ds : copiedDsList) {
						ds.parentComponent=null;
						ds.parentEquipment=pe;
						Ebean.update(ds,
								new HashSet<String>(Arrays.asList("parentComponent","parentEquipment")));
					}
				}
				
					for (History he : History.find.where()
						.eq("parentComponent.id",componentId).findList()) {
						he.parentComponent=null;
						he.parentEquipment = pe;
						Ebean.update(he,
								new HashSet<String>(Arrays.asList("parentEquipment","parentComponent")));
					}
			pc.delete();
			Ebean.commitTransaction();
			result = true;
		} catch(Exception ex) {
			Ebean.rollbackTransaction();
			Logger.error("copyAsEquipment error",ex.fillInStackTrace());
			result=false;
		} finally {
			Ebean.endTransaction();
		}
		return result;
	}
	
}