package models;

import java.util.*;

import play.db.ebean.*;
import play.data.format.Formats;
import play.data.validation.Constraints.*;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

@Entity
@Table(name="parts_components", uniqueConstraints=@UniqueConstraint(columnNames={"component_id","part_id"}))
public class PartsComponents extends Model {
	
	private static final long serialVersionUID = -4274340804293331210L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Long id;
	
	@ManyToOne
	public Components component;
	
	@ManyToOne
	public Parts part;
	
	@Required
	@Formats.NonEmpty
	@Min(0)
	public Float quantity;
	
	// Query creation helper
		public static Model.Finder<Long, PartsComponents> find = 
				new Model.Finder<Long, PartsComponents>(Long.class, PartsComponents.class);
	
	public static List<Components> findPartComponents(Long partId) {
		List<Components> rlist = new ArrayList<Components>();
		for(PartsComponents pc : PartsComponents.find.where().eq("part.id", partId).findList()) {
			rlist.add(pc.component);
		}
		return rlist;
	}
	
	public static List<Parts> findComponentParts(Long componentId) {
	List<Parts> rlist = new ArrayList<Parts>();
	for(PartsComponents pc : PartsComponents.find.where().eq("component.id", componentId).findList()) {
		rlist.add(pc.part);
		}
	return rlist;
	}
	
	/**
	 * 
	 * @param equipmentId
	 * @return
	 */
	public static boolean copy(Long componentId, Long newParentComponentId) {
		boolean result;
		Ebean.beginTransaction();
		try {
			List<PartsComponents> copiedPCs = PartsComponents.find.where().eq("component.id", componentId).findList();
			Components newParentComponent = Components.find.byId(newParentComponentId);
			for(PartsComponents pc : copiedPCs) {
				PartsComponents pastedPC = (PartsComponents) pc._ebean_createCopy();
				pastedPC.id = null;
				pastedPC.component = newParentComponent;
				pastedPC.save();
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
	 * Returns a page of PartsComponents for display
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
	 *            Search filter: name or description of part
	 *            search
	 * @return Page<PartsComponents>: A page of Components
	 */
	public static Page<PartsComponents> page(Long parentComponentId, int page, int pageSize, String sortBy,
			String order, String filter) {
		String whoql = 
				"(component.id) = :parentComponentId AND " +
				"lower(part.name) like :filter";
		
		com.avaje.ebean.Query<PartsComponents> query = Ebean.createQuery(PartsComponents.class);
		query.where(whoql).
		setParameter("filter","%"+filter+"%").
		setParameter("parentComponentId",parentComponentId);
		
		return query.orderBy(sortBy + " " + order).
				findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}

}
