// A1
package com.sismics.docs.resource;

import com.sismics.docs.core.model.Document;
import com.sismics.docs.core.service.TranslationService;
import com.sismics.docs.exception.ForbiddenClientException;
import com.sismics.docs.exception.NotFoundClientException;
import com.sismics.docs.exception.ServerException;
import com.sismics.docs.exception.ClientException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.File;

/**
 * Document resource.
 *
 * @author jtremeaux
 */
@Path("document")
public class DocumentResource extends BaseResource {
    private static final Logger log = LoggerFactory.getLogger(DocumentResource.class);

    @POST
    @Path("{id}/translate")
    public Response translate(
            @PathParam("id") String id,
            @FormParam("targetLanguage") String targetLanguage,
            @FormParam("contentType") String contentType) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the document
        Document document = Document.getById(id);
        if (document == null) {
            throw new NotFoundClientException();
        }

        // Check if the user has access to the document
        if (!document.hasReadAccess(getTargetUserList(), getTargetGroupList())) {
            throw new ForbiddenClientException();
        }

        try {
            String textToTranslate = null;
            if ("description".equals(contentType)) {
                textToTranslate = document.getDescription();
            } else if ("file".equals(contentType)) {
                // 获取主文件
                File file = document.getMainFile();
                if (file == null) {
                    throw new NotFoundClientException("No file found for document");
                }
                textToTranslate = file.getContent();
            } else {
                throw new ClientException("Invalid contentType");
            }

            if (textToTranslate == null || textToTranslate.trim().isEmpty()) {
                throw new ClientException("No text to translate");
            }

            // Translate the content
            TranslationService translationService = new TranslationService();
            String translatedContent = translationService.translate(textToTranslate, "auto", targetLanguage);

            // Return the translated content
            JSONObject response = new JSONObject();
            response.put("translated", translatedContent);
            return Response.ok(response).build();
        } catch (Exception e) {
            log.error("Error translating document", e);
            throw new ServerException("TranslationError", "Error translating document", e);
        }
    }
} 