// A1
package com.sismics.docs.service;

import com.google.common.collect.Lists;
import com.sismics.docs.util.ConfigUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Translation service using Baidu Cloud Translation API.
 */
public class TranslationService {
    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    private static final String BAIDU_TRANSLATE_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    private static final String BAIDU_APP_ID = ConfigUtil.getConfigStringValue("baidu.translate.app.id");
    private static final String BAIDU_SECRET_KEY = ConfigUtil.getConfigStringValue("baidu.translate.secret.key");

    /**
     * Translate text using Baidu Cloud Translation API.
     *
     * @param text Text to translate
     * @param targetLanguage Target language code
     * @return Translated text
     * @throws Exception If an error occurs during translation
     */
    public String translate(String text, String targetLanguage) throws Exception {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Generate salt
        String salt = String.valueOf(new Random().nextInt(10000));

        // Generate sign
        String sign = DigestUtils.md5Hex(BAIDU_APP_ID + text + salt + BAIDU_SECRET_KEY);

        // Prepare request parameters
        List<NameValuePair> params = Lists.newArrayList();
        params.add(new BasicNameValuePair("q", text));
        params.add(new BasicNameValuePair("from", "auto"));
        params.add(new BasicNameValuePair("to", targetLanguage));
        params.add(new BasicNameValuePair("appid", BAIDU_APP_ID));
        params.add(new BasicNameValuePair("salt", salt));
        params.add(new BasicNameValuePair("sign", sign));

        // Send request
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(BAIDU_TRANSLATE_API_URL);
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity);
                JSONObject jsonResponse = new JSONObject(responseString);

                if (jsonResponse.has("error_code")) {
                    String errorCode = jsonResponse.getString("error_code");
                    String errorMsg = jsonResponse.getString("error_msg");
                    throw new Exception("Translation error: " + errorCode + " - " + errorMsg);
                }

                return jsonResponse.getJSONArray("trans_result")
                        .getJSONObject(0)
                        .getString("dst");
            }
        }
    }
} 