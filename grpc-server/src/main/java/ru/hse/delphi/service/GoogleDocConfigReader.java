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
public class GoogleDocConfigReader extends AbstractGoogleDocConfig implements ConfigReader {

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

    protected String readParagraphElement(ParagraphElement element) {
        TextRun run = element.getTextRun();
        if (run == null || run.getContent() == null) {
            return "";
        }
        return run.getContent();
    }
}
