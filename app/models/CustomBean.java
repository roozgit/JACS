package models;

public class CustomBean
{


	/**
	 *
	 */
	private Integer row;
	private String item;

	/**
	 *
	 */
	public CustomBean getMe()
	{
		return this;
	}


	/**
	 * @return the row
	 */
	public Integer getRow() {
		return row;
	}


	/**
	 * @param row the row to set
	 */
	public void setRow(Integer row) {
		this.row = row;
	}


	/**
	 * @return the description
	 */
	public String getItem() {
		return item;
	}


	/**
	 * @param description the description to set
	 */
	public void setItem(String item) {
		this.item = item;
	}


	}
