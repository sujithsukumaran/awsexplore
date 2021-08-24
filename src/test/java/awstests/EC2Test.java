package awstests;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import driverfactory.DriverFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.*;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Properties;


public class EC2Test {

    private static WebDriver driver;
    //Replace the SENDER and RECIPIENT with an SES verified email(s).
    private static String SENDER = "yourSESverifiedmail@gmail.com";
    private static String RECIPIENT = "yourSESverifiedmail@gmail.com";
    private static String SUBJECT = "Test Execution Report " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private static String BODY_TEXT = "Please click on this latest Test Execution Report link:";


    @BeforeTest
    @Parameters("browser")
    public void setUpDriver(@Optional("chrome") String browser) {
        driver = DriverFactory.getWebDriver(browser);
    }

    @Test
    public void EC2LaunchTest() {
        //Replace the URL with another EC Public IP or use any other web page
        driver.get("http://yourinstanceip");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        String pageText = driver.findElement(By.tagName("body")).getText();
        Assert.assertEquals(pageText, "Hello world");
    }

    @AfterTest
    public void tearDown() {
        driver.quit();
    }

    @AfterSuite
    public void sendTestNGReports() {

        //Pass the name of the S3 bucket
        String bucket_name = "yourbucketnamehere";
        //Location of the report file from the project structure
        String file_path = "target/surefire-reports/awstests/awstests.html";
        String key_name = Paths.get(file_path).getFileName().toString();

        //Instantiate an Amazon S3 client, which will make the service call with the supplied AWS credentials.
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new ProfileCredentialsProvider())
                .withRegion(Regions.AP_SOUTH_1).build();

        //Upload the report to S3 bucket
        try {
            s3.putObject(bucket_name, key_name, new File(file_path));
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }

        //Generate the S3 Pre-signed URL of the Test Execution Report
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket_name, key_name, HttpMethod.GET);
        //The URL expires after one day - time in milliseconds
        request.setExpiration(new Date(new Date().getTime() + 86400000));
        URL url = s3.generatePresignedUrl(request);


        //Send the S3 Presigned URL as email using Simple Email Service (SES)
        Session session = Session.getDefaultInstance(new Properties());
        //Create a new MimeMessage object.
        MimeMessage message = new MimeMessage(session);

        try {
            //Configure the email details
            message.setSubject(SUBJECT, "UTF-8");
            message.setFrom(new InternetAddress(SENDER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(RECIPIENT));
            message.setText("\n" + BODY_TEXT + "\n" + url.toString());
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        //Instantiate an Amazon SES client, which will make the service call with the supplied AWS credentials.
        //Replace AP_SOUTH_1 with the AWS Region you're using for Amazon SES.
        try {
            AmazonSimpleEmailService client =
                    AmazonSimpleEmailServiceClientBuilder.standard()
                            .withRegion(Regions.AP_SOUTH_1).build();

            //Print the raw email content on the console
            PrintStream out = System.out;
            message.writeTo(out);

            //Send the email.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));
            SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage);
            client.sendRawEmail(rawEmailRequest);
            System.out.println("Email sent!");
        } catch (Exception ex) {
            System.out.println("Email Failed");
            System.err.println("Error message: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}



