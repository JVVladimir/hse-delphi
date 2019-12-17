package ru.hse.delphi.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import org.springframework.stereotype.Service;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleDocConfigWriter extends AbstractGoogleDocConfig implements ConfigWriter {

    private Integer endIndex;
    private Integer startIndex;
    private boolean needDeleteRaw;

    @Override
    public void writeConfig() {
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

    protected String readParagraphElement(ParagraphElement element) {
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
