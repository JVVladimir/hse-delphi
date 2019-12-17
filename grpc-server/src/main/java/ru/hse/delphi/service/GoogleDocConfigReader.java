package ru.hse.delphi.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.ParagraphElement;
import com.google.api.services.docs.v1.model.StructuralElement;
import com.google.api.services.docs.v1.model.TextRun;
import org.springframework.stereotype.Service;
import ru.hse.delphi.ApplicationRunner;
import ru.hse.delphi.entity.Config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDocConfigReader implements ConfigReader {

    private static final String APPLICATION_NAME = "Application Runner";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String DOCUMENT_ID = "1QeXCG8rEsT-UFD181NA5ZsdF9_5LPd12txJRA2BfFq8";
    public static final String USER_ID = "vovabear1@gmail.com";

    private static final List<String> SCOPES = Collections.singletonList(DocsScopes.DOCUMENTS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    @Override
    public Config readConfig() {
        try {
            var stringConfig = readGoogleConfigFile();
            return parseString(stringConfig);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            return null;
        }
    }

    public Config parseString(String stringConfig) {
        var questionPrefix = "Question=";
        var expertsPrefix = "NumberOfExperts=";
        var questionPart = stringConfig.substring(stringConfig.indexOf(questionPrefix));
        var endOfQuestion = questionPart.indexOf("\n");
        var question = questionPart.substring(questionPrefix.length(), endOfQuestion);

        var expertPart = stringConfig.substring(stringConfig.indexOf(expertsPrefix));
        var endOfExpert = expertPart.indexOf("\n");
        var expertCount = Integer.parseInt(expertPart.substring(expertsPrefix.length(), endOfExpert));
        return new Config(question, expertCount);
    }

    public String readGoogleConfigFile() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Docs service = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        Document doc = service.documents().get(DOCUMENT_ID).execute();
        return readStructuralElements(doc.getBody().getContent());
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = ApplicationRunner.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(USER_ID);
    }

    private String readStructuralElements(List<StructuralElement> elements) {
        var sb = new StringBuilder();
        for (StructuralElement element : elements) {
            if (element.getParagraph() != null) {
                for (ParagraphElement paragraphElement : element.getParagraph().getElements()) {
                    sb.append(readParagraphElement(paragraphElement));
                }
            }
        }
        return sb.toString();
    }

    private String readParagraphElement(ParagraphElement element) {
        TextRun run = element.getTextRun();
        if (run == null || run.getContent() == null) {
            return "";
        }
        return run.getContent();
    }
}
