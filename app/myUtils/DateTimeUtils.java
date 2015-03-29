package myUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.ValidationException;

public class DateTimeUtils {

	public static List<String> dateAction() {
		List<String> output = new ArrayList<String>();
		persianCalendar pc = new persianCalendar();
		output.add(pc.getWeekDayStr());
		output.add(pc.getIranianDate());
		return output;
	}

	public static String getIranianDateTime(Date inDate) {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
			String[] strDateTime = dateFormat.format(inDate).split(" ");
			String[] strDate = strDateTime[0].split("/");
			int gyear = Integer.parseInt(strDate[0]);
			int gmonth = Integer.parseInt(strDate[1]);
			int gday = Integer.parseInt(strDate[2]);
			persianCalendar pc = new persianCalendar(gyear, gmonth, gday);
			String irDate = pc.getIranianDate();
			String[] irArrayDate = irDate.split("/");
			if(irArrayDate[1].length()<2) irArrayDate[1] = "0".concat(irArrayDate[1]);
			if(irArrayDate[2].length()<2) irArrayDate[2] = "0".concat(irArrayDate[2]);
			irDate = irArrayDate[0]+"/"+irArrayDate[1]+"/" + irArrayDate[2];
			return (irDate + " " + strDateTime[1]);
		} catch (NullPointerException e) {
			return ("N/A");
		}
	}
	
	public static String getIranianDate(Date inDate) {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
			String strDateTime = dateFormat.format(inDate);
			String[] strDate = strDateTime.split("/");
			int gyear = Integer.parseInt(strDate[0]);
			int gmonth = Integer.parseInt(strDate[1]);
			int gday = Integer.parseInt(strDate[2]);
			persianCalendar pc = new persianCalendar(gyear, gmonth, gday);
			String irDate = pc.getIranianDate();
			String[] irArrayDate = irDate.split("/");
			if(irArrayDate[1].length()<2) irArrayDate[1] = "0".concat(irArrayDate[1]);
			if(irArrayDate[2].length()<2) irArrayDate[2] = "0".concat(irArrayDate[2]);
			irDate = irArrayDate[0]+"/"+irArrayDate[1]+"/" + irArrayDate[2];
			return irDate;
		} catch (NullPointerException e) {
			return ("N/A");
		}
	}
	
	public static String getIranianYear(Date inDate) {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
			String strDateTime = dateFormat.format(inDate);
			String[] strDate = strDateTime.split("/");
			int gyear = Integer.parseInt(strDate[0]);
			int gmonth = Integer.parseInt(strDate[1]);
			int gday = Integer.parseInt(strDate[2]);
			persianCalendar pc = new persianCalendar(gyear, gmonth, gday);
			String irDate = String.valueOf(pc.getIranianYear());
			return irDate;
		} catch (NullPointerException e) {
			return ("N/A");
		}
	}
	
	public static Date getIranianDateAsDate(Date inDate) {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
			String strDateTime = dateFormat.format(inDate);
			String[] strDate = strDateTime.split("/");
			int gyear = Integer.parseInt(strDate[0]);
			int gmonth = Integer.parseInt(strDate[1]);
			int gday = Integer.parseInt(strDate[2]);
			persianCalendar pc = new persianCalendar(gyear, gmonth, gday);
			String irDate = pc.getIranianDate();
			return dateFormat.parse(irDate);
		} catch (NullPointerException | ParseException e) {
			return null;
		}
	}
	
	public static Date getIranianDateTimeAsDate(Date inDate) {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
			String strDateTime = dateFormat.format(inDate);
			String[] dt = strDateTime.split(" ");
			String[] strDate = dt[0].split("/");
			int gyear = Integer.parseInt(strDate[0]);
			int gmonth = Integer.parseInt(strDate[1]);
			int gday = Integer.parseInt(strDate[2]);
			persianCalendar pc = new persianCalendar(gyear, gmonth, gday);
			String irDate = pc.getIranianDate()+" "+dt[1];
			return dateFormat.parse(irDate);
		} catch (NullPointerException | ParseException e) {
			return null;
		}
	}
	
	
	public static Date getGregorianDateTime(Date inDate){
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
			String[] strDateTime = dateFormat.format(inDate).split(" ");
			String[] strDate = strDateTime[0].split("/");
			int iyear = Integer.parseInt(strDate[0]);
			//check if entered date is gregorian itself and then return itself and exit
			//Any date entered over 1900 is considered Gregorian date
			if(iyear>=1900) {
				return(inDate);
			}
			int imonth = Integer.parseInt(strDate[1]);
			int iday = Integer.parseInt(strDate[2]);
			persianCalendar pc = new persianCalendar();
			pc.setIranianDate(iyear, imonth, iday);
			String grDate = pc.getGregorianDate();
			String retString = grDate + " " + strDateTime[1];
			return dateFormat.parse(retString);
		} catch (NullPointerException | ParseException | NumberFormatException e) {
			return null;
		}
	}
	
	public static Date getGregorianDateTime(String inDate){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		try {
			String[] strDateTime = inDate.split(" ");
			String[] strDate = strDateTime[0].split("/");
			int iyear = Integer.parseInt(strDate[0]);
			//check if entered date is gregorian itself and then return itself and exit
			//Any date entered over 1900 is considered Gregorian date
			if(iyear>=1900) {
				return(dateFormat.parse(inDate));
			}
			int imonth = Integer.parseInt(strDate[1]);
			int iday = Integer.parseInt(strDate[2]);
			persianCalendar pc = new persianCalendar();
			pc.setIranianDate(iyear, imonth, iday);
			String grDate = pc.getGregorianDate();
			String retString = grDate + " " + strDateTime[1];
			return dateFormat.parse(retString);
		} catch (NullPointerException | ParseException | NumberFormatException e) {
			return new Date();
		}
	}
	
	public static String getGregorianDateTimeAsString(String inDate){
		int imonth=1,iday=1,iyear;
		try {
			String[] strDateTime = inDate.split(" ");
			String[] strDate = strDateTime[0].split("/");
			iyear = Integer.parseInt(strDate[0]);
			//check if entered date is gregorian itself and then return itself and exit
			//Any date entered over 1900 is considered Gregorian date
			if(iyear>=1900) {
				return(inDate);
			}
			if(strDate.length>=2)
			 imonth = Integer.parseInt(strDate[1]);
			if(strDate.length==3)
				iday = Integer.parseInt(strDate[2]);
			persianCalendar pc = new persianCalendar();
			pc.setIranianDate(iyear, imonth, iday);
			String grDate = pc.getGregorianDate();
			if(strDateTime.length == 2)
				return grDate + " " + strDateTime[1];
			else
				return grDate;
		} catch (NullPointerException | NumberFormatException e) {
			return "";
		}
	}
	
	public static Date addTimeToRawDate(Date inDate, String time) throws ValidationException {
		try {
			String[] timeElements = time.split(":");
			if(timeElements.length>2) throw new ValidationException("Invalid time length");
			long hours = Long.parseLong(timeElements[0]);
			if(hours>23 || hours <0) throw new ValidationException("Invalid hour");
			long hoursMSec = hours*3600000;
			long minutes = Long.parseLong(timeElements[1]);
			if(minutes>59 || minutes<0) throw new ValidationException("Invalid minute");
			long minutesMSec = minutes*60000;
			long d = inDate.getTime()+hoursMSec+minutesMSec;
			return new Date(d);
		} catch(Exception e) {
			throw new ValidationException("Time format is not valid");
		}
	}
}