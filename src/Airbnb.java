import java.sql.Timestamp;

public class Airbnb {
	private int _zipcode;
	private String _city;
	private String _state;
	private int _averagePrice;
	private int _month;
	private int _year;
	private String _url;
	private Timestamp _crawlTime;
	
	public Airbnb() {
		_zipcode = -1;
		_city = "";
		_state = "";
		_averagePrice = -1;
		_month = -1;
		_year = -1;
		_url = "";
		_crawlTime = null;
	}
	
	public int getZipcode() {
		return _zipcode;
	}
	
	public void setZipcode(int value) {
		_zipcode = value;
	}
	
	public String getCity() {
		return _city;
	}
	
	public void setCity(String value) {
		_city = value;
	}
	
	public String getState() {
		return _state;
	}
	
	public void setState(String value) {
		_state = value;
	}
	public int getAveragePrice() {
		return _averagePrice;
	}
	
	public void setAveragePrice(int value) {
		_averagePrice = value;
	}
	
	public int getMonth() {
		return _month;
	}
	
	public void setMonth(int value) {
		_month = value;
	}
	
	public int getYear() {
		return _year;
	}
	
	public void setYear(int value) {
		_year = value;
	}
	public String getUrl() {
		return _url;
	}

	public void setUrl(String value) {
		_url = value;
	}
	
	public Timestamp getCrawlTime() {
		return _crawlTime;
	}
	
	public void setCrawlTime(Timestamp value) {
		_crawlTime = value;
	}
	
	public void print() {
		System.out.println("Zipcode: " + getZipcode());
		System.out.println("City: " + getCity());
		System.out.println("State: " + getState());
		System.out.println("Average Price: " + getAveragePrice());
		System.out.println("Month: " + getMonth());
		System.out.println("Year: " + getYear());
		System.out.println("Url: " + getUrl());
		System.out.println("CrawlTime: " + getCrawlTime());
	}
}
