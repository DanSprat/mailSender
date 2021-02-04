package ru.popov;


import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.text.html.parser.Parser;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;


import org.xml.sax.SAXException;

/**
 * Goal which touches a timestamp file.
 */
@Mojo( name = "touch", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class MyMojo
    extends AbstractMojo
{

    @Parameter(property = "emailTo")
    private String emailTo;

    @Parameter(property = "emailFrom")
    private String emailFrom;

    @Parameter(property = "authServ")
    private String authServ;

    @Parameter(property = "authUser")
    private String authUser;

    @Parameter(property = "authPass")
    private String authPass;

    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    @Parameter(defaultValue ="${project.build.finalName}")
    private String finalName;


    public void execute()
        throws MojoExecutionException {

        Properties properties = new Properties();
        properties.put("mail.smtp.host",authServ);
        properties.put("mail.smtp.port","587");
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.starttls.enable", "true");
        Session session = Session.getDefaultInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailFrom,authPass);
            }
        });
        Message msg = new MimeMessage(session);
        try {

            msg.setFrom(new InternetAddress(emailFrom)); //  Устанавливаем отправителя
            msg.setRecipient(Message.RecipientType.TO,new InternetAddress(emailTo)); // Устанавливливаем получателя
            msg.setSubject("Ваш Jar  файл готов"); // Тема сообщения
            msg.setSentDate(new Date()); // Время отправки

            Path path = Paths.get(outputDirectory.getAbsolutePath());
            Path pathFile =  path.resolve(finalName+"-jar-with-dependencies.jar"); // Путь к jar файлу

            if (Files.notExists(pathFile)){
                pathFile = path.resolve(finalName+".jar");
            }

            FileInputStream is = new FileInputStream(pathFile.toString());
            JarInputStream jarStream = new JarInputStream(is);
            Manifest mf = jarStream.getManifest(); // Получаем манифест jar файла
            Attributes attributes = mf.getMainAttributes(); // Получаем список аттрибутов

            String body = ""; // Составляем Мета информацию
            for (Map.Entry<Object,Object> x:attributes.entrySet()){
                body+=(x.getKey()+": "+x.getValue()+"\n");
            }
            BasicFileAttributes attr = Files.readAttributes(pathFile,BasicFileAttributes.class);
            body+=("Creation Time: "+attr.creationTime());

            // Вставляем тектовую составляющую
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);

            // Прикрепляем ее
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // Добавляем файл
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(pathFile.toAbsolutePath().toString()); // Источник
            messageBodyPart.setDataHandler(new DataHandler(source)); // Указываем источник
            messageBodyPart.setFileName(finalName); // Устанавливаем название файла
            multipart.addBodyPart(messageBodyPart); // Прикрепляем

            msg.setContent(multipart); // Прикрепляем к сообщению текст и файл

            Transport.send(msg); // Отправляем
            System.out.println("EMail Sent Successfully with attachment!!");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
