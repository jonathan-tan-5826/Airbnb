
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Worker implements Runnable {
	private Connection _connection;
	private String _name;
	private String _checkInDate;
	private String _checkOutDate;
	private String _state;
	private int _month;
	private int _year;
	private List<CityZip> _cityZip;
	private String _proxyAddress;
	private String _version;
	private final String LISTING_CSS_SELECTOR = "div._fhph4u ._1uyh6pwn";
	private final String PRICE_BUTTON_CSS_SELECTOR = "button[aria-controls='menuItemComponent-price']";
	private final String PRICE_TEXT_CLASS = "_150a3jym";

	public Worker(String name, String state, List<CityZip> cityZip, String proxyAddress,
			String checkInDate, String checkOutDate, int month, int year, String version) {
		_name = name;
		_state = state;
		_cityZip = cityZip;
		_checkInDate = checkInDate;
		_checkOutDate = checkOutDate;
		_proxyAddress = proxyAddress;
		_month = month;
		_year = year;
		_version = version;
	}

	public void run() {
		System.out.println("[" + _version + "] Running " + _name);
		try {
			System.out.println("[" + _name + "]: Using proxy address: " + _proxyAddress);
		    DesiredCapabilities cap = GetDesiredCapabilities();
			WebDriver driver = new FirefoxDriver(cap);
			TestIpAddress(driver);
			_connection = getConnection();
			
			for (int count = 0; count < _cityZip.size(); ++count) {
				String city = _cityZip.get(count).getCity();
				String zipcode = _cityZip.get(count).getZipcode();
				crawlZipcode(driver, city, zipcode);
				
				System.out.println(_cityZip.size() - count - 1 + " zipcodes remaining.");
			}
			_connection.close();
			driver.quit();
		} catch (Exception e) {
			System.err.println("[" + _name + "] ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("[" + _name + "]: exiting.");
	}
	
	public void TestIpAddress(WebDriver driver)
	{
		driver.get("https://ipinfo.io");
		WebElement test = (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/header/div/div/div[2]/div[1]/p")));
		System.out.println("[" + _name + "]: IpAddress = " + test.getText());
	}
	
	public DesiredCapabilities GetDesiredCapabilities()
	{
		Proxy proxy = new Proxy();
	    proxy.setAutodetect(false);
	    proxy.setProxyType(Proxy.ProxyType.MANUAL);
		proxy.setHttpProxy(_proxyAddress).setFtpProxy(_proxyAddress).setSslProxy(_proxyAddress);
		
	    FirefoxProfile profile = new FirefoxProfile();
	    profile.setPreference(FirefoxProfile.ALLOWED_HOSTS_PREFERENCE, "localhost");
	    
	    DesiredCapabilities cap = new DesiredCapabilities();
	    cap.setCapability(CapabilityType.PROXY, proxy);
	    cap.setCapability(FirefoxDriver.PROFILE, profile);
	    
	    return cap;
	}

	public void crawlZipcode(WebDriver driver, String city, String zipcode) throws Exception {
		try {
			System.out.println("[" + _name + "]: Crawling " + zipcode + ", " + _checkInDate + " - " + _checkOutDate);
			String searchParameter = _state + " " + zipcode + ", United-States";
			String url = "https://www.airbnb.com/s/" + searchParameter + "/homes?checkin=" + _checkInDate + "&checkout="
					+ _checkOutDate;
			System.out.println(url);
	
			// Load Page
			driver.get(url);
			
			String yearMonthDirectory = _year + "_" + String.format("%02d", _month);
			String modifiedCity = city.replaceAll(" ", "-");
			String fileName = zipcode + "_" + modifiedCity + "_" + _month + "_" + _year + ".txt";
			String directory = "./pagesources/" + yearMonthDirectory + "/" + _state + "/";
			writeStringToFile(directory, fileName, driver.getPageSource());

			// Verify there are at least 5 listings
			(new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(LISTING_CSS_SELECTOR)));
			if (driver.findElements(By.cssSelector(LISTING_CSS_SELECTOR)).size() >= 5) {
				System.out.println("[" + _name + "][Success] Sufficient listings");
	
				// Save PageSource
				writeStringToFile(directory, fileName, driver.getPageSource());
				
				// Wait for Price Range Button Element
				WebElement priceRangeButton = (new WebDriverWait(driver, 10))
					.until(ExpectedConditions.presenceOfElementLocated
							(By.cssSelector(PRICE_BUTTON_CSS_SELECTOR)));
	
				// Click Price Button Element
				priceRangeButton.click();
				System.out.println("Button clicked");
				Thread.sleep(2000);
				
				// Wait for Price Text
				WebElement priceText = null;
				try
				{
					priceText = (new WebDriverWait(driver, 10))
							.until(ExpectedConditions.presenceOfElementLocated(By.className(PRICE_TEXT_CLASS)));
				}
				catch (Exception e)
				{
					// Price text did not show up on page despite there being listings. Error out!
					System.err.println("[" + _name + "] ERROR: " + e.getMessage());
					e.printStackTrace();
					System.exit(-1);
				}
	
				System.out.println("[" + _name + "] priceText = " + priceText.getText());
	
				// Verify Period (Monthly/Daily/etc)
				String foundPeriod = StringUtils.substringBetween(priceText.getText(), "per ", " for");
				if ((foundPeriod != null) && (foundPeriod.toLowerCase().equals("month"))) {
					System.out.println("[" + _name + "][Success] Period verified.");
	
					// Verify City
					String foundCity = StringUtils.substringBetween(priceText.getText(), "for ", " is");
					if ((foundCity != null) && (foundCity.toLowerCase().equals(city.toLowerCase()))) {
						System.out.println("[" + _name + "][Success] City verified.");
	
						// Save to Database
						Airbnb airbnb = new Airbnb();
						airbnb.setCrawlTime(getCurrentTimestamp());
						airbnb.setZipcode(convertToInt(zipcode));
						airbnb.setCity(city);
						airbnb.setState(_state);
						airbnb.setUrl(url);
						airbnb.setMonth(_month);
						airbnb.setYear(_year);
						airbnb.setAveragePrice(convertToInt(getNumericalCharacters(priceText.getText())));
						airbnb.print();
	
						saveAirbnbToDatabase(airbnb);
					} else {
						System.out.println("[" + _name + "][Error] Incorrect city.");
					}
				} else {
					System.out.println("[" + _name + "][Error] Incorrect period.");
				}
			} else {
				System.out.println("[" + _name + "][Error] Insufficient listings.");
			}
	
			System.out.println("Finished crawling " + zipcode + ", " + _checkInDate + " - " + _checkOutDate);
		} catch (WebDriverException e) {
			System.err.println("[" + _name + "] ERROR: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	/**
	 * @title getNumericalCharacters
	 * @param value<String>
	 * @return value<String> with numerical characters only
	 */
	public String getNumericalCharacters(String value) {
		return value.replaceAll("[^0-9]", "");
	}

	/**
	 * @title convertToInt
	 * @param value<String>
	 * @return Converts value<String> to an integer after removing all non-numerical
	 *         characters
	 */
	public int convertToInt(String value) {
		String filteredValue = getNumericalCharacters(value);

		if (filteredValue.length() > 0) {
			return Integer.parseInt(filteredValue);
		} else {
			return 0;
		}
	}

	/**
	 * @title writeStringToFile
	 * @param fileName<String>,
	 *            text<String>
	 * @return
	 * @desc Creates fileName, if it does not exist, in /pagesources and writes text
	 *       to file.
	 */
	public void writeStringToFile(String directory, String fileName, String text) throws Exception {
		System.out.println("[" + _name + "]: writeStringToFile.");
		String fullFileName = directory + fileName;

		File file = new File(fullFileName);
		FileUtils.writeStringToFile(file, text, "UTF-8");
	}

	/**
	 * @title saveAirbnbToDatabase
	 * @param airbnb<Airbnb>
	 * @throws Exception
	 * @desc Inserts airbnb's data to database
	 */
	public void saveAirbnbToDatabase(Airbnb airbnb) throws Exception {
		System.out.println("[" + _name + "] Entered saveAirbnbToDatabase");

		String sqlStatement = "INSERT INTO airbnb(zipcode, average_price, month, year, url, crawl_time) VALUES(?,?,?,?,?,?)";
		PreparedStatement preparedStatement = _connection.prepareStatement(sqlStatement);
		// 1
		preparedStatement.setInt(1, airbnb.getZipcode());
		// 2
		preparedStatement.setInt(2, airbnb.getAveragePrice());
		// 3
		preparedStatement.setInt(3, airbnb.getMonth());
		// 4
		preparedStatement.setInt(4, airbnb.getYear());
		// 5
		preparedStatement.setString(5, airbnb.getUrl());
		// 6
		preparedStatement.setTimestamp(6, airbnb.getCrawlTime());
		preparedStatement.executeUpdate();
		preparedStatement.close();
	}

	/**
	 * @title getConnection
	 * @param
	 * @return connection<Connection> to MySQL database where data will be stored
	 */
	private Connection getConnection() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		String urldb = "jdbc:mysql://localhost/homeDB";
		String user = "jonathan";
		String password = "password";

		Connection connection = DriverManager.getConnection(urldb, user, password);
		return connection;
	}

	/**
	 * @title getCurrentTimestamp
	 * @param
	 * @return currentTimestamp<Timestamp>
	 */
	private Timestamp getCurrentTimestamp() {
		System.out.println("[" + _name + "] Entered getCurrentTimestamp");

		Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
		return currentTimestamp;
	}
}