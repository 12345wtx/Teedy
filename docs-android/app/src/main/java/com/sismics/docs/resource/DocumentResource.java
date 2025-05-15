package com.sismics.docs.resource;

import android.content.Context;

import com.sismics.docs.listener.HttpCallback;
import com.sismics.docs.util.OkHttpUtil;

import java.util.Set;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * Access to /document API.
 * 
 * @author bgamard
 */
@Path("document")
public class DocumentResource extends BaseResource {
    /**
     * GET /document/list.
     *
     * @param context Context
     * @param offset Offset
     * @param query Search query
     * @param callback Callback
     */
    public static void list(Context context, int offset, String query, HttpCallback callback) {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(getApiUrl(context) + "/document/list")
                        .newBuilder()
                        .addQueryParameter("limit", "20")
                        .addQueryParameter("offset", Integer.toString(offset))
                        .addQueryParameter("sort_column", "3")
                        .addQueryParameter("asc", "false")
                        .addQueryParameter("search", query)
                        .build())
                .get()
                .build();
        OkHttpUtil.buildClient(context)
                .newCall(request)
                .enqueue(HttpCallback.buildOkHttpCallback(callback));
    }

    /**
     * GET /document/id.
     *
     * @param context Context
     * @param id ID
     * @param callback Callback
     */
    public static void get(Context context, String id, HttpCallback callback) {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(getApiUrl(context) + "/document/" + id))
                .get()
                .build();
        OkHttpUtil.buildClient(context)
                .newCall(request)
                .enqueue(HttpCallback.buildOkHttpCallback(callback));
    }

    /**
     * DELETE /document/id.
     *
     * @param context Context
     * @param id ID
     * @param callback Callback
     */
    public static void delete(Context context, String id, HttpCallback callback) {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(getApiUrl(context) + "/document/" + id))
                .delete()
                .build();
        OkHttpUtil.buildClient(context)
                .newCall(request)
                .enqueue(HttpCallback.buildOkHttpCallback(callback));
    }

    /**
     * PUT /document.
     *
     * @param context Context
     * @param title Title
     * @param description Description
     * @param tagIdList Tags ID list
     * @param language Language
     * @param createDate Create date
     * @param callback Callback
     */
    public static void add(Context context, String title, String description,
                           Set<String> tagIdList, String language, long createDate, HttpCallback callback) {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("title", title)
                .add("description", description)
                .add("language", language)
                .add("create_date", Long.toString(createDate));
        for( String tagId : tagIdList) {
            formBuilder.add("tags", tagId);
        }

        Request request = new Request.Builder()
                .url(HttpUrl.parse(getApiUrl(context) + "/document"))
                .put(formBuilder.build())
                .build();
        OkHttpUtil.buildClient(context)
                .newCall(request)
                .enqueue(HttpCallback.buildOkHttpCallback(callback));
    }

    /**
     * POST /document/id.
     *
     * @param context Context
     * @param id ID
     * @param title Title
     * @param description Description
     * @param tagIdList Tags ID list
     * @param language Language
     * @param createDate Create date
     * @param callback Callback
     */
    public static void edit(Context context, String id, String title, String description,
                           Set<String> tagIdList, String language, long createDate, HttpCallback callback) {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("title", title)
                .add("description", description)
                .add("language", language)
                .add("create_date", Long.toString(createDate));
        for( String tagId : tagIdList) {
            formBuilder.add("tags", tagId);
        }

        Request request = new Request.Builder()
                .url(HttpUrl.parse(getApiUrl(context) + "/document/" + id))
                .post(formBuilder.build())
                .build();
        OkHttpUtil.buildClient(context)
                .newCall(request)
                .enqueue(HttpCallback.buildOkHttpCallback(callback));
    }

    @POST
    @Path("{id}/translate")
    public Response translate(
            @PathParam("id") String id,
            @FormParam("targetLanguage") String targetLanguage,
            @FormParam("contentType") String contentType) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        DocumentDao documentDao = new DocumentDao();
        Document document = documentDao.getById(id);
        if (document == null) {
            throw new NotFoundException();
        }
        // 检查权限
        if (!document.hasReadAccess(getTargetIdList(null))) {
            throw new ForbiddenClientException();
        }

        try {
            String textToTranslate = null;
            if ("description".equals(contentType)) {
                textToTranslate = document.getDescription();
            } else if ("file".equals(contentType)) {
                FileDao fileDao = new FileDao();
                List<File> files = fileDao.getByDocumentId(null, id);
                if (files == null || files.isEmpty()) {
                    throw new NotFoundException("No file found for document");
                }
                File file = files.get(0); // 取第一个主文件
                String filePath = file.getPath();
                if (filePath.endsWith(".pdf")) {
                    textToTranslate = com.sismics.docs.core.util.PdfTextExtractor.extract(new java.io.File(filePath));
                } else if (filePath.endsWith(".docx")) {
                    textToTranslate = com.sismics.docs.core.util.DocxTextExtractor.extract(new java.io.File(filePath));
                } else {
                    throw new ClientException("UnsupportedFileType", "Unsupported file type");
                }
            } else {
                throw new ClientException("InvalidContentType", "Invalid contentType");
            }

            if (textToTranslate == null || textToTranslate.trim().isEmpty()) {
                throw new ClientException("NoText", "No text to translate");
            }

            com.sismics.docs.core.service.TranslationService translationService = new com.sismics.docs.core.service.TranslationService();
            String translatedContent = translationService.translate(textToTranslate, targetLanguage);

            // 用 Jakarta JSON 构造返回
            JsonObjectBuilder response = Json.createObjectBuilder();
            response.add("translated", translatedContent);
            return Response.ok(response.build()).build();
        } catch (Exception e) {
            log.error("Error translating document", e);
            throw new ServerException("TranslationError", "Error translating document", e);
        }
    }
}
