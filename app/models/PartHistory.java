package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import models.maintenance.Maintenances;
import myUtils.DateTimeUtils;
import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;
import com.avaje.ebean.Query;

@Entity
@Table(name="part_history")
public class PartHistory extends Model {

	private static final long serialVersionUID = -1344538949466836196L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long id;
	
	@ManyToOne
	public Parts parentPart;
	
	@ManyToOne
	public Maintenances parentMaintenance;
	
	public String receiptNumber;
	public String requestNumber;
	
	@Formats.DateTime(pattern="yyyy/MM/dd HH:mm")
	public Date commenceDate;
	
	@ManyToOne
	public InventoryEvents eventType;
	
	@Required
	@Formats.NonEmpty
	public Float stockBalance;
	
	public Float remainingStock;
	
	public Float offeredUnitPrice;
	
	@ManyToOne
	public Users requester;
	
	@ManyToOne
	public Users registrar;
	
	@OneToOne
	public Blobs attachedDoc;
	
	@Column(columnDefinition="TEXT")
	public String comments;
	
	//Query creation helper
		public static Model.Finder<Long,PartHistory> find =
				new Model.Finder<Long,PartHistory>(Long.class,PartHistory.class);
		
		/**
		 * Returns a page of part history for display
		 * 
		 * @param page
		 *            Page number
		 * @param pageSize
		 *            Size of each page
		 * @param sortBy
		 *            parameter to sort the histories by
		 * @param order
		 *            order of sort
		 * @param filter
		 *            Search filter
		 *            search
		 * @return Page<PartHistory>: A page
		 */
		public static Page<PartHistory> page(Long parentPart, int page, int pageSize, String sortBy,
				String order, String filter1, String filter2, String filter3, String filter4, String filter5
				, String filter6, String filter7, String filter8) {
			String phQuery = new String();
			String adds4="",adds5="",adds6="",adds7="",adds8="";
			Date start = new Date();
			Date end = new Date();
			boolean cf=false;
			
			if(filter2.matches("(13|14)\\d\\d(/)(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])-"
					+ "(13|14)\\d\\d\\2(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])")) {
						String[] dates = filter2.split("-");
						start = DateTimeUtils.getGregorianDateTime(dates[0] + " 00:00");
						end = DateTimeUtils.getGregorianDateTime(dates[1] + " 00:00");
						cf = true;
			}
			
			if(filter4.length()==0)
				adds4 = " OR requester.hiringCompany is NULL";
			if(filter5.length()==0)
				adds5 = " OR requester.lastName is NULL";
			if(filter6.length()==0)
				adds6 = " OR requester.discipline is NULL";
			if(filter7.length()==0)
				adds7 = " OR receiptNumber is NULL";
			if(filter8.length()==0)
				adds8 = " OR requestNumber is NULL";
			
			Query<PartHistory> pq = Ebean.createQuery(PartHistory.class);
			
			if(parentPart != -1L)
				phQuery = "parentPart.id = :parentId AND " +
						"lower(eventType.name) like :fil3 AND " +
						"(lower(requester.hiringCompany.name) like :fil4"+adds4+") AND " +
						"(lower(requester.lastName) like :fil5"+adds5+") AND " +
						"(lower(requester.discipline.name) like :fil6"+adds6+") AND " +
						"(receiptNumber like :fil7"+adds7+") AND " +
						"(requestNumber like :fil8"+adds8+")";
						
			else phQuery =
					"lower(parentPart.name) like :fil1 AND " +
					"lower(eventType.name) like :fil3 AND " +
					"(lower(requester.hiringCompany.name) like :fil4"+adds4+") AND " +
					"(lower(requester.lastName) like :fil5"+adds5+") AND " +
					"(lower(requester.discipline.name) like :fil6"+adds6+") AND " +
					"(receiptNumber like :fil7"+adds7+") AND " +
					"(requestNumber like :fil8"+adds8+")";

			if(cf) {
				phQuery = phQuery.concat(" AND (commenceDate >= :startDate AND commenceDate <= :endDate)");
			}
			
			if(parentPart != -1L) pq.where(phQuery)
				.setParameter("parentId", parentPart)
				.setParameter("fil3", "%"+filter3+"%")
				.setParameter("fil4", "%"+filter4+"%")
				.setParameter("fil5", "%"+filter5+"%")
				.setParameter("fil6", "%"+filter6+"%")
				.setParameter("fil7", "%"+filter7+"%")
				.setParameter("fil8", "%"+filter8+"%");
			else
				pq.where(phQuery)
				.setParameter("fil1", "%"+filter1+"%")
				.setParameter("fil3", "%"+filter3+"%")
				.setParameter("fil4", "%"+filter4+"%")
				.setParameter("fil5", "%"+filter5+"%")
				.setParameter("fil6", "%"+filter6+"%")
				.setParameter("fil7", "%"+filter7+"%")
				.setParameter("fil8", "%"+filter8+"%");
			
			if(cf) {
				pq.setParameter("startDate", start)
				.setParameter("endDate", end);
			}
			
			return pq
					.where()
					.orderBy(sortBy + " " + order)
                    .findPagingList(pageSize)
                    .setFetchAhead(false)
                    .getPage(page);
		}
}