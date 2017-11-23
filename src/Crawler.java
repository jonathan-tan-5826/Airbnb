import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Crawler {
	private static Connection _connection;
	private WebDriver _driver;
	private static int _numBuckets = 15;
	private static boolean _doTestIp = false;
	private static final String VERSION = "2.21";
	private static final String[] GROUP_ONE = { "AL", "AR", "CA" };
	private static final String[] GROUP_TWO = { "AK", "AZ", "MI", "NY" };
	private static final String[] GROUP_THREE = { "CO", "GA", "PA", "UT" };
	private static final String[] GROUP_FOUR = { "MD", "ME", "NM", "TX" };
	private static final String[] GROUP_FIVE = { "CT", "IA", "MS", "VA", "WA", "WY" };
	private static final String[] GROUP_SIX = { "DC", "MO", "MT", "NC", "ND", "NE", "NH" };
	private static final String[] GROUP_SEVEN = { "DE", "FL", "HI", "IL", "KS", "RI" };
	private static final String[] GROUP_EIGHT = { "ID", "IN", "KY", "LA", "MA", "OR" };
	private static final String[] GROUP_NINE = { "MN", "NJ", "NV", "OK", "PR", "SC", "SD", "VT" };
	private static final String[] GROUP_TEN = { "OH", "TN", "WI", "WV" };
	private static final String[] PROXIES = { "d01.cs.ucr.edu:3128", "d02.cs.ucr.edu:3128", "d03.cs.ucr.edu:3128",
			"d04.cs.ucr.edu:3128", "d05.cs.ucr.edu:3128", "d06.cs.ucr.edu:3128", "d07.cs.ucr.edu:3128",
			"d08.cs.ucr.edu:3128", "d09.cs.ucr.edu:3128", "d10.cs.ucr.edu:3128", "dblab-rack10.cs.ucr.edu:3128",
			"dblab-rack11.cs.ucr.edu:3128", "dblab-rack12.cs.ucr.edu:3128", "dblab-rack13.cs.ucr.edu:3128",
			"dblab-rack14.cs.ucr.edu:3128", "dblab-rack15.cs.ucr.edu:3128"};
	private static final String LISTING_CSS_SELECTOR = "div._fhph4u ._1mpo9ida";
	private static final String PRICE_BUTTON_CSS_SELECTOR = "button[aria-controls='menuItemComponent-price_range']";
	private static final String PRICE_BUTTON_CSS_SELECTOR2 = "button[aria-controls='menuItemComponent-price']";
	private static final String PRICE_TEXT_CLASS = "_150a3jym";
	private static final String PER_MONTH_SPAN_XPATH = "//span[.='Per month']";
	private static final String CITY_XPATH = "//*[@id=\"english-canonical-url\"]";
	
	/**
	 * @title main
	 * @param args<String[]>
	 * @return
	 * @desc Main function
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 5) {
			System.out.println("Error: Not enough arguments passed in.");
			System.out.println("Correct usage: ./crawler <selection> <month> <year> <num_threads> <1 to test IP, 0 otherwise>");
			System.exit(1);
		}

		String selection = args[0];
		int month = convertToInt(args[1]);
		int year = convertToInt(args[2]);
		_numBuckets = convertToInt(args[3]);
		int testIp = convertToInt(args[4]);
		if (testIp != 0)
		{
			_doTestIp = true;
		}

		if (month < 1 || month > 12) {
			System.out.println("Error: Invalid month passed in. Value must be between 1-12");
			System.exit(1);
		}
		
		if (_numBuckets < 1 || _numBuckets > 15)
		{
			System.out.println("Error: Invalid number of threads passed in. Value must be between 1-15");
			System.exit(1);
		}

		_connection = getConnection();
		
		if (selection.equals("all")) {
			crawlStates(GROUP_ONE, month, year);
			crawlStates(GROUP_TWO, month, year);
			crawlStates(GROUP_THREE, month, year);
			crawlStates(GROUP_FOUR, month, year);
			crawlStates(GROUP_FIVE, month, year);
			crawlStates(GROUP_SIX, month, year);
			crawlStates(GROUP_SEVEN, month, year);
			crawlStates(GROUP_EIGHT, month, year);
			crawlStates(GROUP_NINE, month, year);
			crawlStates(GROUP_TEN, month, year);
		} else if (selection.equals("1")) {
			crawlStates(GROUP_ONE, month, year);
		} else if (selection.equals("2")) {
			crawlStates(GROUP_TWO, month, year);
		} else if (selection.equals("3")) {
			crawlStates(GROUP_THREE, month, year);
		} else if (selection.equals("4")) {
			crawlStates(GROUP_FOUR, month, year);
		} else if (selection.equals("5")) {
			crawlStates(GROUP_FIVE, month, year);
		} else if (selection.equals("6")) {
			crawlStates(GROUP_SIX, month, year);
		} else if (selection.equals("7")) {
			crawlStates(GROUP_SEVEN, month, year);
		} else if (selection.equals("8")) {
			crawlStates(GROUP_EIGHT, month, year);
		} else if (selection.equals("9")) {
			crawlStates(GROUP_NINE, month, year);
		} else if (selection.equals("10")) {
			crawlStates(GROUP_TEN, month, year);
		} else if (selection.length() == 2) {
			crawlState(selection, month, year);
		} else if (selection.equals("test")) {
			System.out.println("Test " + VERSION + " selected");
			Crawler crawler = new Crawler();
			FirefoxProfile profile = new FirefoxProfile();
			profile.setPreference(FirefoxProfile.ALLOWED_HOSTS_PREFERENCE, "localhost");
			Proxy proxy = new Proxy();
		    proxy.setAutodetect(false);
		    proxy.setProxyType(Proxy.ProxyType.MANUAL);
			proxy.setHttpProxy("dblab-rack11.cs.ucr.edu:3128").setFtpProxy("dblab-rack11.cs.ucr.edu:3128").setSslProxy("dblab-rack11.cs.ucr.edu:3128");
		    DesiredCapabilities cap = new DesiredCapabilities();
		    cap.setCapability(CapabilityType.PROXY, proxy);
		    cap.setCapability(FirefoxDriver.PROFILE, profile);
			crawler._driver = new FirefoxDriver(cap);
			crawler._driver.get("https://ipinfo.io");
			WebElement test = (new WebDriverWait(crawler._driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/header/div/div/div[2]/div[1]/p")));
			System.out.println("IpAddress = " + test.getText());
			Calendar calendar = new GregorianCalendar(year, month - 1, 1);
			int numDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			String checkInDate = year + "-" + month + "-01";
			String checkOutDate = year + "-" + month + "-" + numDays;
			crawlZipcode(crawler._driver, "Riverside", "92508", "CA", month, year, checkInDate, checkOutDate);
			crawler._driver.quit();
			
//			File file = new File("G:/Eclipse/eclipse/chromedriver.exe");
//			System.setProperty("webdriver.chrome.driver", file.getAbsolutePath());
//			ChromeOptions chromeOptions = new ChromeOptions();
//			chromeOptions.addArguments("--headless");
//			chromeOptions.addArguments("--disable-gpu");
//			chromeOptions.addArguments("window-size=1920x1080");
//			crawler._driver = new ChromeDriver(chromeOptions);
//			crawler.crawlZipcode("92508", "CA", 2017, 11);
		} else {
			System.out.println("Invalid selection passed in. Valid arguments: all, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, state_abbrev (EX: CA)");
			System.out.println("Application exiting.");
			System.exit(1);
		}
		System.out.println("Crawling completed (100%).");

		_connection.close();
	}

	/**
	 * @title crawlStates
	 * @param states<String[]>,
	 *            month<int>, year<int>
	 * @return
	 * @desc Calls crawlState() on each state<String> in states<String[]> for
	 *       month<int>, year<int>
	 */
	public static void crawlStates(String[] states, int month, int year) throws Exception {
		try {
			for (String state : states) {
				crawlState(state, month, year);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * @title crawlState
	 * @param state<String>,
	 *            month<int>, year<int>
	 * @return
	 * @desc Calls crawlZipcodeByMonthYear for each zipcode in state<String> for
	 *       month<int>, year<int>
	 */
	public static void crawlState(String state, int month, int year) throws Exception {
		try {
			System.out.println("Crawling " + state + ", " + month + "/" + year);
			Calendar calendar = new GregorianCalendar(year, month - 1, 1);
			int numDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			String checkInDate = year + "-" + month + "-01";
			String checkOutDate = year + "-" + month + "-" + numDays;

			Statement statement = _connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			statement.setFetchSize(Integer.MIN_VALUE);
			ResultSet result = statement.executeQuery(
					"SELECT city, zip FROM cities_extended WHERE state_code='" + state + "' order by zip");

			System.out.println("Total buckets: " + _numBuckets);
			List<List<CityZip>> buckets = new ArrayList<List<CityZip>>(_numBuckets);
			for (int i = 0; i < _numBuckets; ++i) {
				buckets.add(new ArrayList<CityZip>());
			}

			ArrayList<Thread> threads = new ArrayList<Thread>(_numBuckets);
			ArrayList<Boolean> threadStatuses = new ArrayList<Boolean>(_numBuckets);
			int counter = 0;
			while (result.next()) {
				String city = result.getString("city");
				String zipcode = result.getString("zip");
				CityZip cityZip = new CityZip(city, zipcode);
				
				int target = (counter++) % _numBuckets;
				buckets.get(target).add(cityZip);
			}
			result.close();
			
			// If only 1 bucket, do not multi-thread.
			if (_numBuckets == 1)
			{
				DesiredCapabilities cap = GetDesiredCapabilities(PROXIES[0]);
				WebDriver driver = new FirefoxDriver(cap);
				_connection = getConnection();

				for (int count = 0; count < buckets.get(0).size(); ++count) {
					String city = buckets.get(0).get(count).getCity();
					String zipcode = buckets.get(0).get(count).getZipcode();
					crawlZipcode(driver, city, zipcode, state, month, year, checkInDate, checkOutDate);
					
					System.out.println(buckets.get(0).size() - count - 1 + " zipcodes remaining.");
				}
				_connection.close();
			}
			// If > 1 bucket, multi-thread.
			else
			{
				CreateWorkerThreads(buckets, threads, threadStatuses, state, month, year, checkInDate, checkOutDate);

				do {
					UpdateThreadStatuses(threads, threadStatuses);
				} while (ThreadsAreActive(threadStatuses));
			}

			System.out.println("Finished crawling " + state + ", " + month + "/" + year);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static DesiredCapabilities GetDesiredCapabilities(String proxyAddress) {
		Proxy proxy = new Proxy();
		proxy.setAutodetect(false);
		proxy.setProxyType(Proxy.ProxyType.MANUAL);
		proxy.setHttpProxy(proxyAddress).setFtpProxy(proxyAddress).setSslProxy(proxyAddress);

		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference(FirefoxProfile.ALLOWED_HOSTS_PREFERENCE, "localhost");

		DesiredCapabilities cap = new DesiredCapabilities();
		cap.setCapability(CapabilityType.PROXY, proxy);
		cap.setCapability(FirefoxDriver.PROFILE, profile);

		return cap;
	}
	
	/**
	 * @title CreateWorkerThreads
	 * @param buckets <List<List<CityZip>>>,
	 * 		  threads <ArrayList<Thread>>,
	 * 		  threadStatuses <ArrayList<Boolean>>,
	 * 	      state <String>,
	 *   	  month <int>,
	 *        year <int>,
	 *        checkInDate <String>,
	 *        checkOutDate <String>,
	 *        runningStatus <RunnningStatus>
	 * @return 
	 * @desc Creates worker threads.
	 */
	public static void CreateWorkerThreads(List<List<CityZip>> buckets, 
			ArrayList<Thread> threads, 
			ArrayList<Boolean> threadStatuses, 
			String state, 
			int month, 
			int year, 
			String checkInDate, 
			String checkOutDate)
	{
		for (int i = 0; i < _numBuckets; ++i) {
			if (!buckets.get(i).isEmpty()) {
				System.out.println("bucket " + i + " size: " + buckets.get(i).size());

				String proxyAddress = PROXIES[i];
				String workerName = String.format("Worker #" + i);
				Worker worker = new Worker(
						workerName,
						state,
						buckets.get(i),
						proxyAddress,
						checkInDate,
						checkOutDate,
						month,
						year,
						VERSION,
						_doTestIp);
				threads.add(new Thread(worker));
				threadStatuses.add(true);
				threads.get(i).start();
			} else {
				threadStatuses.add(false);
				threads.add(null);
			}
		}
	}
	
	/**
	 * @title ThreadsAreActive
	 * @param threadStatuses <ArrayList<Boolean>>
	 * @return True if any threadStatus is true, else false.
	 */
	public static boolean ThreadsAreActive(ArrayList<Boolean> threadStatuses)
	{
		for (Boolean status : threadStatuses)
		{
			if (status) return true;
		}
		return false;
	}

	/**
	 * @title UpdateThreadStatuses
	 * @param threads<ArrayList<Thread>>,
	 *        threadStatuses<ArrayList<Boolean>>
	 * @return
	 * @desc Sets threadStatuses[i] to false if a thread[i] is no longer active.
	 */
	public static void UpdateThreadStatuses(ArrayList<Thread> threads, ArrayList<Boolean> threadStatuses)
	{
		for (int i = 0; i < _numBuckets; ++i)
		{
			if ((threadStatuses.get(i)) && (threads.get(i) != null) && (!threads.get(i).isAlive())) 
			{
				threadStatuses.set(i, false);
				System.out.println("Thread " + i + " is dead.");
			}
		}
	}
	
	/**
	 * @title crawlZipcode
	 * @param zipcode<String>
	 * 		  state<String>
	 * 		  month<int>
	 *        year<int>
	 * @return
	 * @desc Extracts airbnb's average-price/month for zipcode<String> for month<int>,
	 *       year<int> and stores extracted data into database.
	 */
	public static void crawlZipcode(WebDriver driver, String city, String zipcode, String state, int month, int year, String checkInDate, String checkOutDate) throws Exception {
		String searchParameter = state + " " + zipcode + ", United-States";
		String url = "https://www.airbnb.com/s/" + searchParameter + "/homes?checkin=" + checkInDate + "&checkout="
				+ checkOutDate;

		try {
			System.out.println("[ Program ]: Crawling " + zipcode + ", " + checkInDate + " - " + checkOutDate);
			System.out.println(url);

			// Load Page
			driver.get(url);

			String yearMonthDirectory = year + "_" + String.format("%02d", month);
			String modifiedCity = city.replaceAll(" ", "-");
			String fileName = zipcode + "_" + modifiedCity + "_" + month + "_" + year + ".txt";
			String directory = "./pagesources/" + yearMonthDirectory + "/" + state + "/";
			writeStringToFile(directory, fileName, driver.getPageSource());

			// Verify there are at least 5 listings
			(new WebDriverWait(driver, 15))
					.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(LISTING_CSS_SELECTOR)));
			if (driver.findElements(By.cssSelector(LISTING_CSS_SELECTOR)).size() >= 5) {
				System.out.println("[ Program ][Success] Sufficient listings");

				// Save PageSource
				writeStringToFile(directory, fileName, driver.getPageSource());

				WebElement priceRangeButton = null;
				try {
					// Wait for Price Range Button Element
					priceRangeButton = GetWebElementByCSS(driver, PRICE_BUTTON_CSS_SELECTOR, 15);
				} catch (WebDriverException e) {
					// Price button did not show up on page despite there being listings. Error out!
					System.err.println("[ Program ] ERROR GETTING PRICE RANGE BUTTON: " + url);
					System.err.println(e.getMessage());
					System.exit(-1);
				}

				// Click Price Button Element
				priceRangeButton.click();
				System.out.println("[ Program ]: Price button clicked.");

				// Wait for Price Text
				WebElement priceText = GetWebElementByClassName(driver, PRICE_TEXT_CLASS, 15);

				System.out.println("[ Program ] priceText = " + priceText.getText());

				// Verify Period (Monthly/Daily/etc)
				List<WebElement> foundPeriods = driver.findElements(By.xpath(PER_MONTH_SPAN_XPATH));
				if (foundPeriods.size() > 0) {
					System.out.println("[ Program ][Success] Period verified.");

					// Verify City
					List<WebElement> cityTexts = driver.findElements(By.xpath(CITY_XPATH));
					if (cityTexts.size() > 0)
					{
						String prefix = "/s/";
						String suffix = "--" + state;
						String foundCity = StringUtils.substringBetween(cityTexts.get(0).getAttribute("content").toString(), prefix, suffix);
						if ((foundCity != null) && (foundCity.toLowerCase().replaceAll("-", " ").equals(city.toLowerCase()))) {
							System.out.println("[ Program ][Success] City verified.");

							// Save to Database
							Airbnb airbnb = new Airbnb();
							airbnb.setCrawlTime(getCurrentTimestamp());
							airbnb.setZipcode(convertToInt(zipcode));
							airbnb.setCity(city);
							airbnb.setState(state);
							airbnb.setUrl(url);
							airbnb.setMonth(month);
							airbnb.setYear(year);
							airbnb.setAveragePrice(convertToInt(getNumericalCharacters(priceText.getText())));
							airbnb.print();

							saveAirbnbToDatabase(airbnb);
						} else {
							System.out.println("[ Program ][Error] Incorrect city.");
						}
					} else {
						System.out.println("[ Program ][Error] No city found.");
					}
				} else {
					System.out.println("[ Program ][Error] Incorrect period.");
				}
			} else {
				System.out.println("[ Program ][Error] Insufficient listings.");
			}

			System.out.println("Finished crawling " + zipcode + ", " + checkInDate + " - " + checkOutDate);
		} catch (WebDriverException e) {
			System.err.println("[ Program ] ERROR: " + e.getMessage());
			System.err.println(url);
			return;
		}
	}
	
	/**
	 * @title saveAirbnbToDatabase
	 * @param airbnb<Airbnb>
	 * @throws Exception
	 * @desc Inserts airbnb's data to database
	 */
	public static void saveAirbnbToDatabase(Airbnb airbnb) throws Exception {
		System.out.println("[ Program ] Entered saveAirbnbToDatabase");

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

	public static WebElement GetWebElementByClassName(WebDriver driver, String locator, int time) {
		return (new WebDriverWait(driver, 15))
				.ignoring(NoSuchElementException.class)
				.ignoring(StaleElementReferenceException.class)
				.until(ExpectedConditions.presenceOfElementLocated(By.className(locator)));
	}

	public static WebElement GetWebElementByCSS(WebDriver driver, String locator, int time) {
		return (new WebDriverWait(driver, 15))
				.ignoring(NoSuchElementException.class)
				.ignoring(StaleElementReferenceException.class)
				.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(locator)));
	}

	/**
	 * @title savePageSourceFromListingUrl
	 * @param url<String>
	 * @return
	 * @desc Gets the page source from the url<String> and writes it to a text
	 *       file @ "/airbnb/pagesources/state/zipcode_month_year.txt"
	 */
	public void savePageSourceFromListingUrl(String zipcode, String url) throws Exception {
		System.out.println("Entered savePageSourceFromListingUrl");
		long startTime = System.nanoTime();
		_driver.get(url);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);
		System.out.println("Page took " + duration / 1000000000 + " seconds.");

		String directory = "./test/";
		writeStringToFile(directory, "test.html", _driver.getPageSource());

		startTime = System.nanoTime();

		WebElement temp = (new WebDriverWait(_driver, 10))
				.until(ExpectedConditions.presenceOfElementLocated(By.className("_up0n8v6")));
		System.out.println(getNumericalCharacters(temp.getText()));

		WebElement link = (new WebDriverWait(_driver, 10)).until(ExpectedConditions
				.presenceOfElementLocated(By.cssSelector("button[aria-controls='menuItemComponent-price']")));
		endTime = System.nanoTime();
		duration = (endTime - startTime);
		System.out.println("Link took " + duration / 1000000000 + " seconds.");
		link.click();
		startTime = System.nanoTime();
		WebElement text = (new WebDriverWait(_driver, 10))
				.until(ExpectedConditions.presenceOfElementLocated(By.className("_150a3jym")));
		System.out.println(text.getText());

		System.out.println("City = " + StringUtils.substringBetween(text.getText(), "for ", " is"));
		System.out.println("Period = " + StringUtils.substringBetween(text.getText(), "per ", " for"));

		String fileName = "test.txt";
		// String directory = "./pagesources/";
		writeStringToFile(directory, fileName, _driver.getPageSource());
		String averagePrice = getNumericalCharacters(text.getText());
		System.out.println(convertToInt(averagePrice));
		endTime = System.nanoTime();
		duration = (endTime - startTime);
		System.out.println("Parse took " + duration / 1000000000 + " seconds.");

		_driver.close();
	}

	/**
	 * @title writeStringToFile
	 * @param fileName<String>,
	 *            text<String>
	 * @return
	 * @desc Creates fileName, if it does not exist, in /pagesources and writes text
	 *       to file.
	 */
	public static void writeStringToFile(String directory, String fileName, String text) throws Exception {
		String fullFileName = directory + fileName;

		File file = new File(fullFileName);
		FileUtils.writeStringToFile(file, text, "UTF-8");
	}

	/**
	 * @title getNumericalCharacters
	 * @param value<String>
	 * @return value<String> with numerical characters only
	 */
	public static String getNumericalCharacters(String value) {
		return value.replaceAll("[^0-9]", "");
	}

	/**
	 * @title convertToInt
	 * @param value<String>
	 * @return Converts value<String> to an integer after removing all non-numerical
	 *         characters
	 */
	public static int convertToInt(String value) {
		String filteredValue = getNumericalCharacters(value);

		if (filteredValue.length() > 0) {
			return Integer.parseInt(filteredValue);
		} else {
			return 0;
		}
	}

	/**
	 * @title getConnection
	 * @param
	 * @return connection<Connection> to MySQL database where data will be stored
	 */
	private static Connection getConnection() throws ClassNotFoundException, SQLException {
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
	private static Timestamp getCurrentTimestamp() {
		System.out.println("[ Program ] Entered getCurrentTimestamp");

		Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
		return currentTimestamp;
	}
}
