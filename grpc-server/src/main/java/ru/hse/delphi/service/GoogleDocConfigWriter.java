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
import com.google.api.services.docs.v1.model.*;
import org.springframework.stereotype.Service;
import ru.hse.delphi.ApplicationRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDocConfigWriter implements ConfigWriter {

    private static final String APPLICATION_NAME = "Application Runner";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String DOCUMENT_ID = "1QeXCG8rEsT-UFD181NA5ZsdF9_5LPd12txJRA2BfFq8";
    public static final String USER_ID = "vovabear1@gmail.com";

    private static final List<String> SCOPES = Collections.singletonList(DocsScopes.DOCUMENTS);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";
    private Integer endIndex;
    private Integer startIndex;
    private boolean needDeleteRaw;

    @Override
    public void writeConfig(String text) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Docs service = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Document doc = service.documents().get(DOCUMENT_ID).execute();
            readStructuralElements(doc.getBody().getContent());

            String ip = getRealIPAddress();

            List<Request> requests = new ArrayList<>();
            if (!needDeleteRaw) {
                requests.add(getRequestForTextInsertion(ip, endIndex - 1));
            } else {
                requests.add(getRequestForContentDeletion(startIndex + 9, endIndex - 1));
                requests.add(getRequestForTextInsertion(ip, startIndex + 9));
            }

            BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(requests);
            service.documents().batchUpdate(DOCUMENT_ID, body).execute();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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

    private Request getRequestForContentDeletion(int startIndex, int endIndex) {
        return new Request().setDeleteContentRange(
                new DeleteContentRangeRequest()
                        .setRange(new Range().setStartIndex(startIndex).setEndIndex(endIndex))
        );
    }

    private Request getRequestForTextInsertion(String ip, int startIndex) {
        return new Request().setInsertText(
                new InsertTextRequest()
                        .setLocation(new Location().setIndex(startIndex))
                        .setText(ip)
        );
    }

    private String getRealIPAddress() {
        try {
            String ip;
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                ip = socket.getLocalAddress().getHostAddress();
            }
            return ip;
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка при попытке узнать IP адрес сервера");
        }
    }

    private String readStructuralElements(List<StructuralElement> elements) {
        StringBuilder sb = new StringBuilder();
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
        if (run.getContent().startsWith("ServerIP=")) { // 9
            startIndex = element.getStartIndex();
            endIndex = element.getEndIndex();
            needDeleteRaw = endIndex - 1 - startIndex != 9;
        }
        return run.getContent();
    }
}
